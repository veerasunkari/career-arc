package local.careerarc.service;

import local.careerarc.entity.*;
import local.careerarc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CareerService {
    private final AccountRepository accounts;
    private final StudentProfileRepository profiles;
    private final AssessmentRepository assessments;
    private final SkillRepository skills;
    private final StudentSkillRepository studentSkills;
    private final CareerRoleRepository roles;
    private final CareerRoleSkillRepository roleSkills;
    private final CareerRecommendationRepository recommendations;
    private final SkillGapRepository gaps;
    private final LearningResourceRepository resources;
    private final RoadmapRepository roadmaps;
    private final RoadmapMilestoneRepository milestones;
    private final RecommendationEngine engine;

    public CareerService(AccountRepository accounts, StudentProfileRepository profiles, AssessmentRepository assessments,
                         SkillRepository skills, StudentSkillRepository studentSkills, CareerRoleRepository roles,
                         CareerRoleSkillRepository roleSkills, CareerRecommendationRepository recommendations,
                         SkillGapRepository gaps, LearningResourceRepository resources, RoadmapRepository roadmaps,
                         RoadmapMilestoneRepository milestones, RecommendationEngine engine) {
        this.accounts = accounts; this.profiles = profiles; this.assessments = assessments; this.skills = skills;
        this.studentSkills = studentSkills; this.roles = roles; this.roleSkills = roleSkills;
        this.recommendations = recommendations; this.gaps = gaps; this.resources = resources;
        this.roadmaps = roadmaps; this.milestones = milestones; this.engine = engine;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> profile(long studentId) {
        StudentProfile profile = profileEntity(studentId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", profile.student.name); result.put("email", profile.student.email);
        result.put("phone", profile.phone); result.put("educationLevel", profile.educationLevel);
        result.put("fieldOfStudy", profile.fieldOfStudy); result.put("academicScore", profile.academicScore);
        result.put("interests", profile.interests); result.put("preferredDomains", profile.preferredDomains);
        result.put("careerGoals", profile.careerGoals); result.put("targetRole", profile.targetCareerRole == null ? null : roleView(profile.targetCareerRole));
        return result;
    }

    public Map<String, Object> updateProfile(long studentId, ProfileRequest input) {
        StudentProfile profile = profileEntity(studentId);
        profile.student.name = input.name().trim();
        profile.phone = clean(input.phone()); profile.educationLevel = clean(input.educationLevel());
        profile.fieldOfStudy = clean(input.fieldOfStudy()); profile.academicScore = input.academicScore();
        profile.interests = clean(input.interests()); profile.preferredDomains = clean(input.preferredDomains());
        profile.careerGoals = clean(input.careerGoals());
        profiles.save(profile);
        return profile(studentId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> questions() {
        String[][] prompts = {
            {"Analytical", "I enjoy finding patterns in information."}, {"Analytical", "I like breaking complex problems into smaller steps."},
            {"Creative", "I enjoy imagining different ways to solve a problem."}, {"Creative", "I like making or designing new things."},
            {"Communication", "I enjoy explaining ideas to other people."}, {"Communication", "I listen carefully and help groups reach understanding."},
            {"Practical", "I learn well by building and testing something."}, {"Practical", "I enjoy troubleshooting hands-on challenges."}
        };
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < prompts.length; i++) result.add(Map.of("id", i, "category", prompts[i][0], "prompt", prompts[i][1]));
        return result;
    }

    public Map<String, Object> submitAssessment(long studentId, AssessmentRequest input) {
        Account student = account(studentId);
        Assessment assessment = new Assessment(); assessment.student = student;
        List<Integer> answers = input.answers();
        assessment.analyticalScore = averageScore(answers, 0);
        assessment.creativeScore = averageScore(answers, 2);
        assessment.communicationScore = averageScore(answers, 4);
        assessment.practicalScore = averageScore(answers, 6);
        assessment.interests = clean(input.interests()); assessment.preferredDomains = clean(input.preferredDomains());
        assessment = assessments.save(assessment);
        StudentProfile profile = profileEntity(studentId);
        profile.interests = assessment.interests; profile.preferredDomains = assessment.preferredDomains;
        profiles.save(profile);
        return assessmentView(assessment);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> assessmentHistory(long studentId) {
        return assessments.findByStudentIdOrderByCompletedAtDesc(studentId).stream().map(this::assessmentView).toList();
    }

    public List<Map<String, Object>> studentSkills(long studentId) {
        return studentSkills.findByStudentId(studentId).stream().map(this::studentSkillView).toList();
    }

    public Map<String, Object> addSkill(long studentId, SkillRequest input) {
        if (input.proficiency() < 1 || input.proficiency() > 3) throw new IllegalArgumentException("Proficiency must be Beginner (1), Intermediate (2), or Advanced (3).");
        Skill skill = skills.findByNameIgnoreCase(input.name().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a skill from the catalog."));
        StudentSkill record = studentSkills.findByStudentIdAndSkillId(studentId, skill.id).orElseGet(() -> new StudentSkill(account(studentId), skill, input.proficiency()));
        record.proficiency = input.proficiency();
        return studentSkillView(studentSkills.save(record));
    }

    public void removeSkill(long studentId, long skillId) {
        StudentSkill record = studentSkills.findByStudentIdAndSkillId(studentId, skillId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found on this profile."));
        studentSkills.delete(record);
    }

    public List<Map<String, Object>> calculateRecommendations(long studentId) {
        StudentProfile profile = profileEntity(studentId);
        Assessment latest = assessments.findByStudentIdOrderByCompletedAtDesc(studentId).stream().findFirst().orElse(null);
        Map<String, Integer> current = studentSkills.findByStudentId(studentId).stream()
            .collect(Collectors.toMap(item -> item.skill.name.toLowerCase(Locale.ROOT), item -> item.proficiency, Math::max));
        recommendations.deleteByStudentId(studentId);
        List<CareerRecommendation> saved = new ArrayList<>();
        for (CareerRole role : roles.findByActiveTrueOrderByTitleAsc()) {
            Map<String, Integer> requirements = roleSkills.findByRoleId(role.id).stream()
                .collect(Collectors.toMap(item -> item.skill.name, item -> item.requiredLevel, Math::max));
            RecommendationEngine.MatchResult match = engine.calculate(role.title, role.domain, role.academicFit, role.interests,
                requirements, current, profile.fieldOfStudy, profile.interests, profile.preferredDomains, latest);
            CareerRecommendation item = new CareerRecommendation(); item.student = profile.student; item.role = role;
            item.score = match.score(); item.matchingSkills = join(match.matchingSkills());
            item.relevantFactors = join(match.relevantFactors()); item.missingSkills = join(match.missingSkills());
            item.explanation = match.explanation(); saved.add(recommendations.save(item));
        }
        return saved.stream().sorted(Comparator.comparingInt((CareerRecommendation item) -> item.score).reversed())
            .map(this::recommendationView).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recommendationHistory(long studentId) {
        return recommendations.findByStudentIdOrderByScoreDescCalculatedAtDesc(studentId).stream().map(this::recommendationView).toList();
    }

    public Map<String, Object> selectRole(long studentId, long roleId) {
        CareerRole role = roles.findById(roleId).filter(item -> item.active)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Career role not found."));
        StudentProfile profile = profileEntity(studentId); profile.targetCareerRole = role; profiles.save(profile);
        gaps.deleteByStudentIdAndRoleId(studentId, roleId);
        Map<String, Integer> current = studentSkills.findByStudentId(studentId).stream()
            .collect(Collectors.toMap(item -> item.skill.id.toString(), item -> item.proficiency));
        List<CareerRoleSkill> requirements = roleSkills.findByRoleId(roleId);
        List<SkillGap> result = new ArrayList<>();
        for (CareerRoleSkill requirement : requirements) {
            int level = current.getOrDefault(requirement.skill.id.toString(), 0);
            SkillGap gap = new SkillGap(); gap.student = profile.student; gap.role = role; gap.skill = requirement.skill;
            gap.currentLevel = level; gap.requiredLevel = requirement.requiredLevel;
            gap.status = CareerPlanningRules.gapStatus(level, requirement.requiredLevel);
            gap.priority = requirement.skill.prerequisite == null ? 1 : 2;
            result.add(gaps.save(gap));
        }
        return Map.of("role", roleView(role), "gaps", result.stream().sorted(gapOrder()).map(this::gapView).toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> skillGaps(long studentId) {
        StudentProfile profile = profileEntity(studentId);
        if (profile.targetCareerRole == null) return Map.of("role", Map.of(), "gaps", List.of());
        List<SkillGap> result = gaps.findByStudentIdAndRoleIdOrderByPriorityAsc(studentId, profile.targetCareerRole.id);
        return Map.of("role", roleView(profile.targetCareerRole), "gaps", result.stream().sorted(gapOrder()).map(this::gapView).toList());
    }

    public Map<String, Object> generateRoadmap(long studentId) {
        StudentProfile profile = profileEntity(studentId);
        if (profile.targetCareerRole == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a target career before generating a roadmap.");
        Roadmap previous = roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(studentId).orElse(null);
        if (previous != null) { previous.active = false; roadmaps.save(previous); }
        Roadmap roadmap = new Roadmap(); roadmap.student = profile.student; roadmap.role = profile.targetCareerRole;
        roadmap = roadmaps.save(roadmap);
        List<SkillGap> learningGaps = gaps.findByStudentIdAndRoleIdOrderByPriorityAsc(studentId, roadmap.role.id).stream()
            .filter(gap -> !"MET".equals(gap.status)).toList();
        List<SkillGap> ordered = CareerPlanningRules.prerequisiteOrder(learningGaps);
        Long targetRoleId = roadmap.role.id;
        int sequence = 1;
        for (SkillGap gap : ordered) {
            LearningResource resource = resources.findBySkillId(gap.skill.id).stream()
                .filter(item -> item.role == null || item.role.id.equals(targetRoleId)).findFirst().orElse(null);
            RoadmapMilestone milestone = new RoadmapMilestone(); milestone.roadmap = roadmap; milestone.skill = gap.skill;
            milestone.resource = resource; milestone.sequenceNumber = sequence++;
            milestone.title = resource == null ? "Build " + gap.skill.name : resource.title;
            milestone.description = ("MISSING".equals(gap.status) ? "Start with the foundations for " : "Strengthen your proficiency in ")
                + gap.skill.name + " (" + levelName(gap.requiredLevel) + " target).";
            milestones.save(milestone);
        }
        if (ordered.isEmpty()) {
            RoadmapMilestone milestone = new RoadmapMilestone(); milestone.roadmap = roadmap; milestone.sequenceNumber = 1;
            milestone.title = "Create a portfolio project"; milestone.description = "Apply your existing skills to a project relevant to " + roadmap.role.title + ".";
            milestones.save(milestone);
        }
        return roadmapView(roadmap);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> roadmap(long studentId) {
        return roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(studentId).map(this::roadmapView)
            .orElseGet(() -> Map.of("roadmap", Map.of(), "milestones", List.of(), "progress", 0));
    }

    public Map<String, Object> updateMilestone(long studentId, long milestoneId, String status) {
        if (!Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED").contains(status)) throw new IllegalArgumentException("Status must be NOT_STARTED, IN_PROGRESS, or COMPLETED.");
        Roadmap roadmap = roadmaps.findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active roadmap."));
        RoadmapMilestone milestone = milestones.findById(milestoneId).filter(item -> item.roadmap.id.equals(roadmap.id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found on your roadmap."));
        milestone.status = status;
        return milestoneView(milestones.save(milestone));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> resourceCatalog(String skill, String role, String type, String difficulty) {
        return resources.findAllByOrderByTitleAsc().stream()
            .filter(item -> blank(skill) || item.skill != null && item.skill.name.equalsIgnoreCase(skill))
            .filter(item -> blank(role) || item.role != null && item.role.title.equalsIgnoreCase(role))
            .filter(item -> blank(type) || item.type.equalsIgnoreCase(type))
            .filter(item -> blank(difficulty) || item.difficulty != null && item.difficulty.equalsIgnoreCase(difficulty))
            .map(this::resourceView).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> careerRoles() {
        return roles.findByActiveTrueOrderByTitleAsc().stream().map(this::roleView).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> skillCatalog() {
        return skills.findAll().stream().sorted(Comparator.comparing(item -> item.name))
            .map(item -> Map.<String, Object>of("id", item.id, "name", item.name, "category", item.category,
                "prerequisite", item.prerequisite == null ? "" : item.prerequisite.name)).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(long studentId) {
        StudentProfile profile = profileEntity(studentId);
        List<Map<String, Object>> skillRows = studentSkills(studentId);
        List<Map<String, Object>> recommendationRows = recommendationHistory(studentId);
        Map<String, Object> roadmap = roadmap(studentId);
        @SuppressWarnings("unchecked") List<Map<String, Object>> milestoneRows = (List<Map<String, Object>>) roadmap.get("milestones");
        long completed = milestoneRows.stream().filter(item -> "COMPLETED".equals(item.get("status"))).count();
        int progress = milestoneRows.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / milestoneRows.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profileCompletion", profileCompletion(profile)); result.put("assessment", assessmentHistory(studentId).stream().findFirst().orElse(null));
        result.put("recommendations", recommendationRows.stream().limit(3).toList());
        result.put("targetRole", profile.targetCareerRole == null ? null : roleView(profile.targetCareerRole));
        result.put("skillCount", skillRows.size()); result.put("gaps", skillGaps(studentId).get("gaps"));
        result.put("roadmap", roadmap); result.put("completedMilestones", completed);
        result.put("totalMilestones", milestoneRows.size()); result.put("progress", progress);
        result.put("alerts", dashboardAlerts(profile, milestoneRows));
        return result;
    }

    private List<String> dashboardAlerts(StudentProfile profile, List<Map<String, Object>> milestoneRows) {
        List<String> alerts = new ArrayList<>();
        if (profile.fieldOfStudy == null || profile.academicScore == null || profile.careerGoals == null) alerts.add("Complete your profile details to improve recommendation context.");
        if (milestoneRows.stream().anyMatch(item -> "NOT_STARTED".equals(item.get("status")))) alerts.add("You have roadmap milestones waiting to start.");
        if (profile.targetCareerRole != null) alerts.add("Review your skill gaps for " + profile.targetCareerRole.title + ".");
        return alerts;
    }

    private Map<String, Object> roadmapView(Roadmap roadmap) {
        List<Map<String, Object>> items = milestones.findByRoadmapIdOrderBySequenceNumberAsc(roadmap.id).stream().map(this::milestoneView).toList();
        long done = items.stream().filter(item -> "COMPLETED".equals(item.get("status"))).count();
        Map<String, Object> result = new LinkedHashMap<>(); result.put("roadmap", Map.of("id", roadmap.id, "role", roleView(roadmap.role), "generatedAt", roadmap.generatedAt));
        result.put("milestones", items); result.put("progress", items.isEmpty() ? 0 : (int) Math.round(done * 100.0 / items.size()));
        return result;
    }

    private Comparator<SkillGap> gapOrder() {
        return Comparator.comparingInt((SkillGap item) -> item.priority).thenComparing(item -> item.skill.name);
    }

    private Map<String, Object> profileView(StudentProfile profile) { return profile(profile.student.id); }
    private Map<String, Object> studentSkillView(StudentSkill item) {
        return Map.of("id", item.skill.id, "name", item.skill.name, "category", item.skill.category,
            "proficiency", item.proficiency, "level", levelName(item.proficiency));
    }
    private Map<String, Object> roleView(CareerRole role) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("id", role.id); result.put("title", role.title);
        result.put("domain", role.domain); result.put("description", role.description); result.put("academicFit", role.academicFit);
        result.put("interests", role.interests); result.put("requirements", roleSkills.findByRoleId(role.id).stream()
            .map(item -> Map.of("skillId", item.skill.id, "skill", item.skill.name, "requiredLevel", item.requiredLevel,
                "level", levelName(item.requiredLevel), "prerequisite", item.skill.prerequisite == null ? "" : item.skill.prerequisite.name)).toList());
        return result;
    }
    private Map<String, Object> recommendationView(CareerRecommendation item) {
        return Map.of("id", item.id, "role", roleView(item.role), "score", item.score,
            "matchingSkills", split(item.matchingSkills), "relevantFactors", split(item.relevantFactors),
            "missingSkills", split(item.missingSkills), "explanation", item.explanation);
    }
    private Map<String, Object> gapView(SkillGap item) {
        return Map.of("id", item.id, "skillId", item.skill.id, "skill", item.skill.name, "status", item.status,
            "currentLevel", item.currentLevel, "requiredLevel", item.requiredLevel, "currentLabel", levelName(item.currentLevel),
            "requiredLabel", levelName(item.requiredLevel), "priority", item.priority,
            "prerequisite", item.skill.prerequisite == null ? "" : item.skill.prerequisite.name);
    }
    private Map<String, Object> assessmentView(Assessment item) {
        Map<String, Object> scores = new LinkedHashMap<>(); scores.put("analytical", item.analyticalScore);
        scores.put("creative", item.creativeScore); scores.put("communication", item.communicationScore); scores.put("practical", item.practicalScore);
        String strongest = scores.entrySet().stream().max(Comparator.comparingInt(entry -> (Integer) entry.getValue())).map(Map.Entry::getKey).orElse("analytical");
        return Map.of("id", item.id, "scores", scores, "strongestArea", strongest, "interests", safe(item.interests),
            "preferredDomains", safe(item.preferredDomains), "completedAt", item.completedAt,
            "explanation", "Project-level guidance aid only; results are not a scientifically validated psychological test.");
    }
    private Map<String, Object> milestoneView(RoadmapMilestone item) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.id); result.put("title", item.title);
        result.put("description", item.description); result.put("sequence", item.sequenceNumber); result.put("status", item.status);
        result.put("skill", item.skill == null ? null : item.skill.name);
        result.put("resource", item.resource == null ? null : resourceView(item.resource)); return result;
    }
    private Map<String, Object> resourceView(LearningResource item) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.id); result.put("title", item.title);
        result.put("description", item.description); result.put("type", item.type); result.put("skill", item.skill == null ? "" : item.skill.name);
        result.put("role", item.role == null ? "" : item.role.title); result.put("difficulty", item.difficulty);
        result.put("url", item.url); result.put("illustrative", item.illustrative); return result;
    }
    private int profileCompletion(StudentProfile profile) {
        List<Object> fields = List.of(profile.educationLevel == null ? "" : profile.educationLevel,
            profile.fieldOfStudy == null ? "" : profile.fieldOfStudy, profile.academicScore == null ? "" : profile.academicScore,
            profile.interests == null ? "" : profile.interests, profile.preferredDomains == null ? "" : profile.preferredDomains,
            profile.careerGoals == null ? "" : profile.careerGoals);
        return (int) Math.round(fields.stream().filter(value -> !value.toString().isBlank()).count() * 100.0 / fields.size());
    }
    private int averageScore(List<Integer> answers, int start) { return (int) Math.round((answers.get(start) + answers.get(start + 1)) * 10.0); }
    private String levelName(int level) { return switch (level) { case 1 -> "Beginner"; case 2 -> "Intermediate"; case 3 -> "Advanced"; default -> "Not recorded"; }; }
    private Account account(long id) { return accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found.")); }
    private StudentProfile profileEntity(long id) { return profiles.findByStudentId(id).orElseGet(() -> profiles.save(new StudentProfile(account(id)))); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String safe(String value) { return value == null ? "" : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String join(List<String> values) { return String.join("|", values); }
    private List<String> split(String value) { return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split("\\|")); }

    public record ProfileRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 120) String name,
                                 @jakarta.validation.constraints.Size(max = 30) String phone,
                                 @jakarta.validation.constraints.Size(max = 50) String educationLevel,
                                 @jakarta.validation.constraints.Size(max = 120) String fieldOfStudy,
                                 @jakarta.validation.constraints.DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("100.0") Double academicScore,
                                 @jakarta.validation.constraints.Size(max = 500) String interests,
                                 @jakarta.validation.constraints.Size(max = 500) String preferredDomains,
                                 @jakarta.validation.constraints.Size(max = 1000) String careerGoals) {}
    public record AssessmentRequest(@jakarta.validation.constraints.Size(max = 500) String interests,
                                    @jakarta.validation.constraints.Size(max = 500) String preferredDomains,
                                    @jakarta.validation.constraints.Size(min = 8, max = 8)
                                    List<@jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) Integer> answers) {}
    public record SkillRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String name,
                               @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 20) String category,
                               @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(3) int proficiency) {}
}