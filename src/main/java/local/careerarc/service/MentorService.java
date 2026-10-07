package local.careerarc.service;

import local.careerarc.entity.*;
import local.careerarc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@Transactional
public class MentorService {
    private final AccountRepository accounts;
    private final MentorAssignmentRepository assignments;
    private final MentorFeedbackRepository feedback;
    private final CareerRoleRepository roles;
    private final LearningResourceRepository resources;
    private final CareerService career;

    public MentorService(AccountRepository accounts, MentorAssignmentRepository assignments, MentorFeedbackRepository feedback,
                         CareerRoleRepository roles, LearningResourceRepository resources, CareerService career) {
        this.accounts = accounts; this.assignments = assignments; this.feedback = feedback;
        this.roles = roles; this.resources = resources; this.career = career;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> assignedStudents(long mentorId) {
        return assignments.findByMentorId(mentorId).stream().map(assignment -> {
            Account student = assignment.student;
            return Map.<String, Object>of("id", student.id, "name", student.name, "email", student.email,
                "profile", career.profile(student.id), "dashboard", career.dashboard(student.id));
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> review(long mentorId, long studentId) {
        ensureAssigned(mentorId, studentId);
        return Map.of("profile", career.profile(studentId), "assessmentHistory", career.assessmentHistory(studentId),
            "recommendations", career.recommendationHistory(studentId), "skillGaps", career.skillGaps(studentId),
            "roadmap", career.roadmap(studentId), "dashboard", career.dashboard(studentId), "feedback", feedbackFor(studentId));
    }

    public Map<String, Object> addFeedback(long mentorId, long studentId, FeedbackRequest input) {
        ensureAssigned(mentorId, studentId);
        Account mentor = accounts.findById(mentorId).orElseThrow();
        Account student = accounts.findById(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));
        MentorFeedback item = new MentorFeedback(); item.mentor = mentor; item.student = student; item.message = input.message().trim();
        if (input.suggestedRoleId() != null) item.suggestedRole = roles.findById(input.suggestedRoleId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggested role not found."));
        if (input.suggestedResourceId() != null) item.suggestedResource = resources.findById(input.suggestedResourceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggested resource not found."));
        return feedbackView(feedback.save(item));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> studentFeedback(long studentId) { return feedbackFor(studentId); }

    private List<Map<String, Object>> feedbackFor(long studentId) {
        return feedback.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::feedbackView).toList();
    }

    private Map<String, Object> feedbackView(MentorFeedback item) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.id); result.put("mentor", item.mentor.name);
        result.put("message", item.message); result.put("createdAt", item.createdAt);
        result.put("suggestedRole", item.suggestedRole == null ? null : Map.of("id", item.suggestedRole.id, "title", item.suggestedRole.title));
        result.put("suggestedResource", item.suggestedResource == null ? null : Map.of("id", item.suggestedResource.id, "title", item.suggestedResource.title));
        return result;
    }

    private void ensureAssigned(long mentorId, long studentId) {
        assignments.findByMentorIdAndStudentId(mentorId, studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "This student is not assigned to your mentor account."));
    }

    public record FeedbackRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 1800) String message,
                                  Long suggestedRoleId, Long suggestedResourceId) {}
}