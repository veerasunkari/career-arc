package local.careerarc.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import local.careerarc.entity.Account;
import local.careerarc.repository.AccountRepository;
import local.careerarc.service.CareerService;
import local.careerarc.service.MentorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    private final CareerService career;
    private final MentorService mentor;
    private final AccountRepository accounts;
    public StudentController(CareerService career, MentorService mentor, AccountRepository accounts) {
        this.career = career; this.mentor = mentor; this.accounts = accounts;
    }

    @GetMapping("/dashboard") public Map<String, Object> dashboard(Authentication auth) { return career.dashboard(studentId(auth)); }
    @GetMapping("/profile") public Map<String, Object> profile(Authentication auth) { return career.profile(studentId(auth)); }
    @PutMapping("/profile") public Map<String, Object> updateProfile(Authentication auth, @Valid @RequestBody CareerService.ProfileRequest body) {
        return career.updateProfile(studentId(auth), body);
    }
    @GetMapping("/assessment/questions") public Object questions() { return career.questions(); }
    @PostMapping("/assessment") public Map<String, Object> submitAssessment(Authentication auth, @Valid @RequestBody CareerService.AssessmentRequest body) {
        return career.submitAssessment(studentId(auth), body);
    }
    @GetMapping("/assessment/history") public Object assessmentHistory(Authentication auth) { return career.assessmentHistory(studentId(auth)); }
    @GetMapping("/skills") public Object skills(Authentication auth) { return career.studentSkills(studentId(auth)); }
    @PostMapping("/skills") public Map<String, Object> addSkill(Authentication auth, @Valid @RequestBody CareerService.SkillRequest body) {
        return career.addSkill(studentId(auth), body);
    }
    @DeleteMapping("/skills/{skillId}") public void removeSkill(Authentication auth, @PathVariable long skillId) { career.removeSkill(studentId(auth), skillId); }
    @GetMapping("/recommendations") public Object recommendations(Authentication auth) { return career.recommendationHistory(studentId(auth)); }
    @PostMapping("/recommendations/calculate") public Object calculateRecommendations(Authentication auth) { return career.calculateRecommendations(studentId(auth)); }
    @PutMapping("/target-role") public Object selectRole(Authentication auth, @Valid @RequestBody TargetRoleRequest body) {
        return career.selectRole(studentId(auth), body.roleId());
    }
    @GetMapping("/skill-gaps") public Object skillGaps(Authentication auth) { return career.skillGaps(studentId(auth)); }
    @GetMapping("/roadmap") public Object roadmap(Authentication auth) { return career.roadmap(studentId(auth)); }
    @GetMapping("/feedback") public Object feedback(Authentication auth) { return mentor.studentFeedback(studentId(auth)); }
    @PostMapping("/roadmap/generate") public Object generateRoadmap(Authentication auth) { return career.generateRoadmap(studentId(auth)); }
    @PatchMapping("/milestones/{milestoneId}") public Object updateMilestone(Authentication auth, @PathVariable long milestoneId,
                                                                                 @Valid @RequestBody MilestoneRequest body) {
        return career.updateMilestone(studentId(auth), milestoneId, body.status());
    }

    private long studentId(Authentication auth) { return account(auth).id; }
    private Account account(Authentication auth) { return accounts.findByEmailIgnoreCase(auth.getName()).orElseThrow(); }
    public record TargetRoleRequest(@jakarta.validation.constraints.NotNull Long roleId) {}
    public record MilestoneRequest(@NotBlank String status) {}
}