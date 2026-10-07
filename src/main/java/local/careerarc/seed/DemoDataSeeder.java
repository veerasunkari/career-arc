package local.careerarc.seed;

import local.careerarc.entity.*;
import local.careerarc.repository.*;
import local.careerarc.service.CareerService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
@ConditionalOnProperty(prefix = "app", name = "demo-seed", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {
    private final AccountRepository accounts;
    private final StudentProfileRepository profiles;
    private final AssessmentRepository assessments;
    private final SkillRepository skills;
    private final StudentSkillRepository studentSkills;
    private final CareerRoleRepository roles;
    private final CareerRoleSkillRepository roleSkills;
    private final LearningResourceRepository resources;
    private final RoadmapRepository roadmaps;
    private final RoadmapMilestoneRepository milestones;
    private final MentorAssignmentRepository assignments;
    private final CareerRecommendationRepository recommendations;
    private final SkillGapRepository gaps;
    private final PasswordEncoder passwordEncoder;
    private final CareerService career;

    public DemoDataSeeder(AccountRepository accounts, StudentProfileRepository profiles, AssessmentRepository assessments,
                          SkillRepository skills, StudentSkillRepository studentSkills, CareerRoleRepository roles,
                          CareerRoleSkillRepository roleSkills, LearningResourceRepository resources,
                          RoadmapRepository roadmaps, RoadmapMilestoneRepository milestones,
                          MentorAssignmentRepository assignments, CareerRecommendationRepository recommendations,
                          SkillGapRepository gaps, PasswordEncoder passwordEncoder, CareerService career) {
        this.accounts = accounts; this.profiles = profiles; this.assessments = assessments; this.skills = skills;
        this.studentSkills = studentSkills; this.roles = roles; this.roleSkills = roleSkills; this.resources = resources;
        this.roadmaps = roadmaps; this.milestones = milestones; this.assignments = assignments;
        this.recommendations = recommendations; this.gaps = gaps; this.passwordEncoder = passwordEncoder; this.career = career;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Account student = account("student@careerarc.local", "CareerArcDemo123!", "STUDENT", "Aarav Mehta");
        Account mentor = account("mentor@careerarc.local", "CareerArcMentor123!", "MENTOR", "Mira Shah");
        account("admin@careerarc.local", "CareerArcAdmin123!", "ADMIN", "Career Arc Admin");

        Skill problemSolving = skill("Problem Solving", "SOFT", null);
        Skill git = skill("Git", "TECHNICAL", null);
        Skill java = skill("Java", "TECHNICAL", problemSolving);
        Skill sql = skill("SQL", "TECHNICAL", git);
        Skill python = skill("Python", "TECHNICAL", null);
        Skill visualization = skill("Data Visualization", "TECHNICAL", sql);
        Skill communication = skill("Communication", "SOFT", null);
        Skill teamwork = skill("Teamwork", "SOFT", communication);
        Skill figma = skill("Figma", "TECHNICAL", null);
        Skill research = skill("User Research", "TECHNICAL", communication);
        Skill empathy = skill("Empathy", "SOFT", communication);
        Skill listening = skill("Active Listening", "SOFT", communication);
        Skill creativity = skill("Creative Thinking", "SOFT", null);

        CareerRole developer = role("Software Engineer", "Software & Data", "Design, build, test, and maintain software products.",
            "Computer Science, Information Technology, Engineering", "software, programming, problem solving, technology");
        CareerRole analyst = role("Data Analyst", "Software & Data", "Turn data into clear findings that support decisions.",
            "Computer Science, Statistics, Mathematics, Business", "data, analysis, research, problem solving");
        CareerRole designer = role("UX Designer", "Product Design", "Research user needs and shape useful digital experiences.",
            "Design, Psychology, Computer Science", "design, creativity, research, people");
        CareerRole counsellor = role("Career Counsellor", "Guidance & Education", "Support people as they explore learning and career choices.",
            "Psychology, Education, Human Resources", "people, communication, mentoring, learning");

        requirement(developer, java, 3); requirement(developer, sql, 2); requirement(developer, git, 2); requirement(developer, problemSolving, 2);
        requirement(analyst, sql, 2); requirement(analyst, python, 2); requirement(analyst, visualization, 2); requirement(analyst, communication, 2);
        requirement(designer, figma, 2); requirement(designer, research, 2); requirement(designer, communication, 2); requirement(designer, creativity, 2);
        requirement(counsellor, communication, 3); requirement(counsellor, empathy, 2); requirement(counsellor, listening, 2); requirement(counsellor, research, 1);

        resource("Java Foundations", "Illustrative project resource: practice core syntax, classes, and small programs.", "COURSE", java, developer, "BEGINNER");
        resource("SQL Query Lab", "Illustrative project resource: write joins, filters, and aggregate queries against sample data.", "PRACTICE", sql, analyst, "BEGINNER");
        resource("Versioned Portfolio Project", "Create a small Java project and track changes with meaningful Git commits.", "PROJECT", git, developer, "BEGINNER");
        resource("Python Data Notebook", "Use Python to clean a small dataset and explain your choices.", "PROJECT", python, analyst, "INTERMEDIATE");
        resource("Data Storyboard", "Choose a dataset, make two clear charts, and write a concise insight summary.", "PROJECT", visualization, analyst, "INTERMEDIATE");
        resource("Interface Prototype", "Build a task flow and a clickable prototype for a focused user need.", "PROJECT", figma, designer, "BEGINNER");
        resource("Research Interview Plan", "Practice neutral questions, note-taking, and turning observations into themes.", "COURSE", research, designer, "INTERMEDIATE");
        resource("Peer Explanation Practice", "Explain one technical idea clearly, then invite questions and feedback.", "PRACTICE", communication, null, "BEGINNER");
        resource("Collaborative Sprint", "Plan, build, and review a small deliverable with a peer team.", "PROJECT", teamwork, null, "INTERMEDIATE");
        resource("Problem Decomposition", "Break a larger challenge into testable steps and reflect on your approach.", "PRACTICE", problemSolving, null, "BEGINNER");
        resource("Empathy Mapping Exercise", "Summarize a person's goals and constraints without assuming their needs.", "PROJECT", empathy, counsellor, "BEGINNER");
        resource("Listening Reflection", "Practice paraphrasing, clarifying questions, and next-step summaries.", "PRACTICE", listening, counsellor, "BEGINNER");
        resource("Creative Concept Sprint", "Generate options, choose one using clear criteria, and share a rough concept.", "PROJECT", creativity, designer, "BEGINNER");

        StudentProfile profile = profiles.findByStudentId(student.id).orElseGet(() -> profiles.save(new StudentProfile(student)));
        if (profile.fieldOfStudy == null) {
            profile.educationLevel = "Undergraduate"; profile.fieldOfStudy = "Computer Science"; profile.academicScore = 82.0;
            profile.interests = "software, data, programming, problem solving"; profile.preferredDomains = "Software & Data";
            profile.careerGoals = "Build reliable software and grow into a backend engineering role.";
            profile.phone = ""; profiles.save(profile);
        }
        addStudentSkill(student, java, 2); addStudentSkill(student, sql, 1); addStudentSkill(student, git, 1);
        addStudentSkill(student, problemSolving, 2); addStudentSkill(student, communication, 2);
        addStudentSkill(student, python, 1); addStudentSkill(student, teamwork, 2);
        if (assessments.findByStudentIdOrderByCompletedAtDesc(student.id).isEmpty()) {
            Assessment assessment = new Assessment(); assessment.student = student; assessment.analyticalScore = 80;
            assessment.creativeScore = 70; assessment.communicationScore = 80; assessment.practicalScore = 90;
            assessment.interests = profile.interests; assessment.preferredDomains = profile.preferredDomains; assessments.save(assessment);
        }
        if (assignments.findByMentorIdAndStudentId(mentor.id, student.id).isEmpty()) assignments.save(new MentorAssignment(mentor, student));

        if (recommendations.findByStudentIdOrderByScoreDescCalculatedAtDesc(student.id).isEmpty()) career.calculateRecommendations(student.id);
        profile = profiles.findByStudentId(student.id).orElseThrow();
        if (profile.targetCareerRole == null) { career.selectRole(student.id, developer.id); profile.targetCareerRole = developer; }
        if (gaps.findByStudentIdAndRoleIdOrderByPriorityAsc(student.id, developer.id).isEmpty()) career.selectRole(student.id, developer.id);
        if (roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(student.id).isEmpty()) {
            career.generateRoadmap(student.id);
            List<RoadmapMilestone> items = roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(student.id)
                .map(item -> milestones.findByRoadmapIdOrderBySequenceNumberAsc(item.id)).orElse(List.of());
            if (!items.isEmpty()) { items.get(0).status = "COMPLETED"; milestones.save(items.get(0)); }
            if (items.size() > 1) { items.get(1).status = "IN_PROGRESS"; milestones.save(items.get(1)); }
        }
    }

    private Account account(String email, String password, String role, String name) {
        Account account = accounts.findByEmailIgnoreCase(email).orElse(null);
        if (account == null) {
            return accounts.save(new Account(email, passwordEncoder.encode(password), role, name));
        }
        if (!role.equals(account.role)) {
            account.role = role;
            account = accounts.save(account);
        }
        return account;
    }
    private Skill skill(String name, String category, Skill prerequisite) {
        Skill item = skills.findByNameIgnoreCase(name).orElseGet(() -> skills.save(new Skill(name, category)));
        if (prerequisite != null && item.prerequisite == null) { item.prerequisite = prerequisite; item = skills.save(item); }
        return item;
    }
    private CareerRole role(String title, String domain, String description, String academicFit, String interests) {
        return roles.findByTitleIgnoreCase(title).orElseGet(() -> roles.save(new CareerRole(title, domain, description, academicFit, interests)));
    }
    private void requirement(CareerRole role, Skill skill, int level) {
        CareerRoleSkill item = roleSkills.findByRoleIdAndSkillId(role.id, skill.id).orElseGet(() -> new CareerRoleSkill(role, skill, level));
        if (item.requiredLevel != level) item.requiredLevel = level;
        roleSkills.save(item);
    }
    private void resource(String title, String description, String type, Skill skill, CareerRole role, String difficulty) {
        if (resources.existsByTitleIgnoreCase(title)) return;
        LearningResource item = new LearningResource(); item.title = title; item.description = description; item.type = type;
        item.skill = skill; item.role = role; item.difficulty = difficulty; item.illustrative = true; resources.save(item);
    }
    private void addStudentSkill(Account student, Skill skill, int level) {
        StudentSkill item = studentSkills.findByStudentIdAndSkillId(student.id, skill.id)
            .orElseGet(() -> new StudentSkill(student, skill, level));
        item.proficiency = level; studentSkills.save(item);
    }
}