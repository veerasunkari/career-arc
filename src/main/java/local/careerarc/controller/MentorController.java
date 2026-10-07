package local.careerarc.controller;

import jakarta.validation.Valid;
import local.careerarc.repository.AccountRepository;
import local.careerarc.service.MentorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentor")
public class MentorController {
    private final MentorService mentor;
    private final AccountRepository accounts;
    public MentorController(MentorService mentor, AccountRepository accounts) { this.mentor = mentor; this.accounts = accounts; }
    @GetMapping("/students") public Object assignedStudents(Authentication auth) { return mentor.assignedStudents(id(auth)); }
    @GetMapping("/students/{studentId}") public Object review(Authentication auth, @PathVariable long studentId) { return mentor.review(id(auth), studentId); }
    @PostMapping("/students/{studentId}/feedback") public Object addFeedback(Authentication auth, @PathVariable long studentId,
                                                                                 @Valid @RequestBody MentorService.FeedbackRequest input) {
        return mentor.addFeedback(id(auth), studentId, input);
    }
    private long id(Authentication auth) { return accounts.findByEmailIgnoreCase(auth.getName()).orElseThrow().id; }
}