package local.careerarc;

import local.careerarc.entity.Account;
import local.careerarc.repository.*;
import local.careerarc.seed.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:career_arc_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.auth.jwt-secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    "app.demo-seed=true"
})
@AutoConfigureMockMvc
class ApplicationWorkflowTest {
    @Autowired private MockMvc mvc;
    @Autowired private DemoDataSeeder seeder;
    @Autowired private AccountRepository accounts;
    @Autowired private SkillRepository skills;
    @Autowired private CareerRoleRepository roles;
    @Autowired private LearningResourceRepository resources;
    @Autowired private CareerRecommendationRepository recommendations;
    @Autowired private SkillGapRepository gaps;
    @Autowired private RoadmapRepository roadmaps;
    @Autowired private RoadmapMilestoneRepository milestones;

    @Test
    void seededJourneyIsAvailableAndInitializationIsIdempotent() throws Exception {
        Account student = accounts.findByEmailIgnoreCase("student@careerarc.local").orElseThrow();
        long[] counts = counts();
        student.role = "MENTOR";
        accounts.save(student);
        seeder.run(new DefaultApplicationArguments(new String[0]));
        assertEquals("STUDENT", accounts.findByEmailIgnoreCase("student@careerarc.local").orElseThrow().role);
        assertArrayEquals(counts, counts());
        assertTrue(skills.count() >= 10);
        assertTrue(roles.count() >= 4);
        assertTrue(resources.count() >= 10);
        assertTrue(recommendations.findByStudentIdOrderByScoreDescCalculatedAtDesc(student.id).size() >= 3);
        assertFalse(gaps.findByStudentIdAndRoleIdOrderByPriorityAsc(student.id,
            roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(student.id).orElseThrow().role.id).isEmpty());
        assertTrue(milestones.findByRoadmapIdOrderBySequenceNumberAsc(
            roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(student.id).orElseThrow().id).size() >= 2);
    }

    @Test
    void backendEnforcesRoleBoundaries() throws Exception {
        mvc.perform(get("/api/admin/reports").with(user("student").roles("STUDENT")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/student/dashboard").with(user("mentor").roles("MENTOR")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/mentor/students").with(user("mentor@careerarc.local").roles("MENTOR")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/reports").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.users").isNumber());
        mvc.perform(get("/api/admin/reports")).andExpect(status().isUnauthorized());
    }

    @Test
    void servesFrontendShellAndStaticAssetsWithoutAuthentication() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Your next chapter")));
        mvc.perform(get("/css/app.css")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("--forest")));
        mvc.perform(get("/js/app.js")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/api/student/dashboard")));
    }

    @Test
    void demoStudentCanLoginAndReadPersistedDashboard() throws Exception {
        var login = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@careerarc.local\",\"password\":\"CareerArcDemo123!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(cookie().exists("CAREER_ARC_REFRESH"))
            .andReturn();
        String refreshCookie = login.getResponse().getHeader("Set-Cookie");
        assertNotNull(refreshCookie);
        assertTrue(refreshCookie.contains("HttpOnly"));
        assertTrue(refreshCookie.contains("SameSite=Strict"));
        assertTrue(refreshCookie.contains("Path=/api/auth"));
        String accessToken = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/student/dashboard").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.recommendations.length()").value(3))
            .andExpect(jsonPath("$.targetRole.title").value("Software Engineer"));
        var roadmap = roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(
            accounts.findByEmailIgnoreCase("student@careerarc.local").orElseThrow().id).orElseThrow();
        var nextMilestone = milestones.findByRoadmapIdOrderBySequenceNumberAsc(roadmap.id).stream()
            .filter(item -> item.status.equals("NOT_STARTED")).findFirst().orElseThrow();
        mvc.perform(patch("/api/student/milestones/" + nextMilestone.id).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertEquals("COMPLETED", milestones.findById(nextMilestone.id).orElseThrow().status);
        mvc.perform(get("/api/student/dashboard").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.progress").value(67));
    }

    @Test
    void refreshTokensRotateAndReplayRevokesTheFamily() throws Exception {
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@careerarc.local\",\"password\":\"CareerArcDemo123!\"}"))
            .andExpect(status().isOk()).andReturn();
        var originalRefresh = login.getResponse().getCookie("CAREER_ARC_REFRESH");
        assertNotNull(originalRefresh);

        var rotated = mvc.perform(post("/api/auth/refresh").cookie(originalRefresh))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();
        var newRefresh = rotated.getResponse().getCookie("CAREER_ARC_REFRESH");
        assertNotNull(newRefresh);
        assertNotEquals(originalRefresh.getValue(), newRefresh.getValue());

        mvc.perform(post("/api/auth/refresh").cookie(originalRefresh)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(newRefresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void studentApiRequiresBearerAccessToken() throws Exception {
        mvc.perform(get("/api/student/dashboard")).andExpect(status().isUnauthorized());
    }

    private long[] counts() {
        return new long[]{accounts.count(), skills.count(), roles.count(), resources.count(), recommendations.count(),
            gaps.count(), roadmaps.count(), milestones.count()};
    }
}