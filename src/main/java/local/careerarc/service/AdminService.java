package local.careerarc.service;

import local.careerarc.entity.*;
import local.careerarc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@Transactional
public class AdminService {
    private final AccountRepository accounts;
    private final StudentProfileRepository profiles;
    private final CareerRoleRepository roles;
    private final CareerRoleSkillRepository roleSkills;
    private final SkillRepository skills;
    private final LearningResourceRepository resources;
    private final CareerRecommendationRepository recommendations;
    private final SkillGapRepository gaps;
    private final RoadmapRepository roadmaps;
    private final RoadmapMilestoneRepository milestones;
    private final MentorAssignmentRepository assignments;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AccountRepository accounts, StudentProfileRepository profiles, CareerRoleRepository roles, CareerRoleSkillRepository roleSkills,
                        SkillRepository skills, LearningResourceRepository resources,
                        CareerRecommendationRepository recommendations, SkillGapRepository gaps,
                        RoadmapRepository roadmaps, RoadmapMilestoneRepository milestones,
                        MentorAssignmentRepository assignments, PasswordEncoder passwordEncoder) {
        this.accounts = accounts; this.profiles = profiles; this.roles = roles; this.roleSkills = roleSkills; this.skills = skills;
        this.resources = resources; this.recommendations = recommendations; this.gaps = gaps;
        this.roadmaps = roadmaps; this.milestones = milestones; this.assignments = assignments; this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> users() {
        return accounts.findAll().stream().map(account -> Map.<String, Object>of("id", account.id, "email", account.email,
            "name", account.name, "role", account.role, "enabled", account.enabled)).toList();
    }

    public Map<String, Object> updateUser(long id, UserUpdate input) {
        Account account = accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        if (!Set.of("STUDENT", "MENTOR", "ADMIN").contains(input.role())) throw new IllegalArgumentException("Role must be STUDENT, MENTOR, or ADMIN.");
        account.role = input.role(); account.enabled = input.enabled(); account.name = input.name().trim();
        accounts.save(account);
        if (account.role.equals("STUDENT")) profiles.findByStudentId(account.id).orElseGet(() -> profiles.save(new StudentProfile(account)));
        return Map.of("id", account.id, "email", account.email, "name", account.name, "role", account.role, "enabled", account.enabled);
    }

    public Map<String, Object> createUser(UserCreate input) {
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("An account with this email already exists.");
        if (!Set.of("STUDENT", "MENTOR", "ADMIN").contains(input.role())) throw new IllegalArgumentException("Role must be STUDENT, MENTOR, or ADMIN.");
        Account account = accounts.save(new Account(email, passwordEncoder.encode(input.password()), input.role(), input.name().trim()));
        if (account.role.equals("STUDENT")) profiles.save(new StudentProfile(account));
        return Map.of("id", account.id, "email", account.email, "name", account.name, "role", account.role, "enabled", account.enabled);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> assignments() {
        return assignments.findAll().stream().map(item -> Map.<String, Object>of("id", item.id,
            "mentorId", item.mentor.id, "mentor", item.mentor.name, "studentId", item.student.id, "student", item.student.name)).toList();
    }

    public Map<String, Object> assignMentor(AssignmentRequest input) {
        Account mentor = accounts.findById(input.mentorId()).filter(item -> item.role.equals("MENTOR"))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an account with the MENTOR role."));
        Account student = accounts.findById(input.studentId()).filter(item -> item.role.equals("STUDENT"))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an account with the STUDENT role."));
        MentorAssignment assignment = assignments.findByMentorIdAndStudentId(mentor.id, student.id)
            .orElseGet(() -> assignments.save(new MentorAssignment(mentor, student)));
        return Map.of("id", assignment.id, "mentor", mentor.name, "student", student.name);
    }

    public void removeAssignment(long id) {
        if (!assignments.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor assignment not found.");
        assignments.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> roles() {
        return roles.findAll().stream().map(role -> {
            Map<String, Object> result = new LinkedHashMap<>(); result.put("id", role.id); result.put("title", role.title);
            result.put("domain", role.domain); result.put("description", role.description); result.put("academicFit", role.academicFit);
            result.put("interests", role.interests); result.put("active", role.active);
            result.put("skills", roleSkills.findByRoleId(role.id).stream().map(item -> Map.of("id", item.id,
                "skillId", item.skill.id, "skill", item.skill.name, "requiredLevel", item.requiredLevel)).toList());
            return result;
        }).toList();
    }

    public Map<String, Object> saveRole(Long id, RoleRequest input) {
        CareerRole role = id == null ? new CareerRole() : roles.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Career role not found."));
        role.title = input.title().trim(); role.domain = input.domain().trim(); role.description = input.description();
        role.academicFit = input.academicFit(); role.interests = input.interests(); role.active = input.active();
        role = roles.save(role);
        return Map.of("id", role.id, "title", role.title, "domain", role.domain, "active", role.active);
    }

    public Map<String, Object> addRoleSkill(long roleId, RoleSkillRequest input) {
        CareerRole role = roles.findById(roleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Career role not found."));
        Skill skill = skills.findById(input.skillId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found."));
        CareerRoleSkill link = roleSkills.findAll().stream().filter(item -> item.role.id.equals(roleId) && item.skill.id.equals(input.skillId()))
            .findFirst().orElseGet(() -> new CareerRoleSkill(role, skill, input.requiredLevel()));
        link.requiredLevel = input.requiredLevel(); roleSkills.save(link);
        return Map.of("role", role.title, "skill", skill.name, "requiredLevel", link.requiredLevel);
    }

    public void removeRoleSkill(long roleId, long linkId) {
        CareerRoleSkill link = roleSkills.findById(linkId).filter(item -> item.role.id.equals(roleId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role skill not found."));
        roleSkills.delete(link);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> skills() {
        return skills.findAll().stream().map(skill -> Map.<String, Object>of("id", skill.id, "name", skill.name,
            "category", skill.category, "prerequisite", skill.prerequisite == null ? "" : skill.prerequisite.name)).toList();
    }

    public Map<String, Object> saveSkill(Long id, SkillRequest input) {
        Skill skill = id == null ? new Skill() : skills.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found."));
        skill.name = input.name().trim(); skill.category = input.category().trim().toUpperCase(Locale.ROOT);
        if (input.prerequisiteId() != null) {
            if (id != null && id.equals(input.prerequisiteId())) throw new IllegalArgumentException("A skill cannot be its own prerequisite.");
            skill.prerequisite = skills.findById(input.prerequisiteId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prerequisite skill not found."));
            Set<Long> visited = new HashSet<>();
            Skill cursor = skill.prerequisite;
            while (cursor != null && cursor.id != null && visited.add(cursor.id)) {
                if (id != null && cursor.id.equals(id)) throw new IllegalArgumentException("Skill prerequisites cannot contain a cycle.");
                cursor = cursor.prerequisite;
            }
        } else skill.prerequisite = null;
        skill = skills.save(skill);
        return Map.of("id", skill.id, "name", skill.name, "category", skill.category,
            "prerequisite", skill.prerequisite == null ? "" : skill.prerequisite.name);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> resources() {
        return resources.findAllByOrderByTitleAsc().stream().map(item -> Map.<String, Object>of("id", item.id,
            "title", item.title, "description", item.description == null ? "" : item.description, "type", item.type,
            "skill", item.skill == null ? "" : item.skill.name, "role", item.role == null ? "" : item.role.title,
            "difficulty", item.difficulty == null ? "" : item.difficulty, "url", item.url == null ? "" : item.url,
            "illustrative", item.illustrative)).toList();
    }

    public Map<String, Object> createResource(ResourceRequest input) {
        LearningResource item = new LearningResource(); item.title = input.title().trim(); item.description = input.description();
        item.type = input.type().toUpperCase(Locale.ROOT); item.difficulty = input.difficulty(); item.url = input.url();
        item.skill = input.skillId() == null ? null : skills.findById(input.skillId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found."));
        item.role = input.roleId() == null ? null : roles.findById(input.roleId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Career role not found."));
        item.illustrative = input.illustrative(); item = resources.save(item);
        return Map.of("id", item.id, "title", item.title, "type", item.type);
    }

    public void removeResource(long id) {
        LearningResource item = resources.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found."));
        milestones.findByResourceId(id).forEach(milestone -> { milestone.resource = null; milestones.save(milestone); });
        resources.delete(item);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report() {
        Map<String, Long> roleCounts = new TreeMap<>();
        recommendations.findAll().forEach(item -> roleCounts.merge(item.role.title, 1L, Long::sum));
        Map<String, Long> milestoneCounts = new TreeMap<>();
        milestones.findAll().forEach(item -> milestoneCounts.merge(item.status, 1L, Long::sum));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", accounts.count()); result.put("students", accounts.findAll().stream().filter(item -> item.role.equals("STUDENT")).count());
        result.put("mentors", accounts.findAll().stream().filter(item -> item.role.equals("MENTOR")).count());
        result.put("careerRoles", roles.count()); result.put("skills", skills.count());
        result.put("recommendationRecords", recommendations.count()); result.put("skillGaps", gaps.count());
        result.put("roadmaps", roadmaps.findAll().stream().filter(item -> item.active).count()); result.put("milestoneStatuses", milestoneCounts);
        result.put("recommendationsByRole", roleCounts); result.put("resources", resources.count());
        return result;
    }

    public record UserCreate(@jakarta.validation.constraints.NotBlank String name, @jakarta.validation.constraints.Email String email,
                             @jakarta.validation.constraints.Size(min = 10, max = 72) String password, String role) {}
    public record UserUpdate(@jakarta.validation.constraints.NotBlank String name, String role, boolean enabled) {}
    public record AssignmentRequest(@jakarta.validation.constraints.NotNull Long mentorId,
                                    @jakarta.validation.constraints.NotNull Long studentId) {}
    public record RoleRequest(@jakarta.validation.constraints.NotBlank String title, @jakarta.validation.constraints.NotBlank String domain,
                              String description, String academicFit, String interests, boolean active) {}
    public record RoleSkillRequest(@jakarta.validation.constraints.NotNull Long skillId,
                                   @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(3) int requiredLevel) {}
    public record SkillRequest(@jakarta.validation.constraints.NotBlank String name, @jakarta.validation.constraints.NotBlank String category, Long prerequisiteId) {}
    public record ResourceRequest(@jakarta.validation.constraints.NotBlank String title, String description, @jakarta.validation.constraints.NotBlank String type,
                                  Long skillId, Long roleId, String difficulty, String url, boolean illustrative) {}
}