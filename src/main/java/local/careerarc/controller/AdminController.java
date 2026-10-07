package local.careerarc.controller;

import jakarta.validation.Valid;
import local.careerarc.service.AdminService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    public AdminController(AdminService admin) { this.admin = admin; }
    @GetMapping("/users") public Object users() { return admin.users(); }
    @PostMapping("/users") public Object createUser(@Valid @RequestBody AdminService.UserCreate input) { return admin.createUser(input); }
    @PutMapping("/users/{id}") public Object updateUser(@PathVariable long id, @Valid @RequestBody AdminService.UserUpdate input) { return admin.updateUser(id, input); }
    @GetMapping("/assignments") public Object assignments() { return admin.assignments(); }
    @PostMapping("/assignments") public Object assignMentor(@Valid @RequestBody AdminService.AssignmentRequest input) { return admin.assignMentor(input); }
    @DeleteMapping("/assignments/{id}") public void removeAssignment(@PathVariable long id) { admin.removeAssignment(id); }
    @GetMapping("/roles") public Object roles() { return admin.roles(); }
    @PostMapping("/roles") public Object createRole(@Valid @RequestBody AdminService.RoleRequest input) { return admin.saveRole(null, input); }
    @PutMapping("/roles/{id}") public Object updateRole(@PathVariable long id, @Valid @RequestBody AdminService.RoleRequest input) { return admin.saveRole(id, input); }
    @PostMapping("/roles/{id}/skills") public Object addRoleSkill(@PathVariable long id, @Valid @RequestBody AdminService.RoleSkillRequest input) { return admin.addRoleSkill(id, input); }
    @DeleteMapping("/roles/{id}/skills/{linkId}") public void removeRoleSkill(@PathVariable long id, @PathVariable long linkId) { admin.removeRoleSkill(id, linkId); }
    @GetMapping("/skills") public Object skills() { return admin.skills(); }
    @PostMapping("/skills") public Object createSkill(@Valid @RequestBody AdminService.SkillRequest input) { return admin.saveSkill(null, input); }
    @PutMapping("/skills/{id}") public Object updateSkill(@PathVariable long id, @Valid @RequestBody AdminService.SkillRequest input) { return admin.saveSkill(id, input); }
    @GetMapping("/resources") public Object resources() { return admin.resources(); }
    @PostMapping("/resources") public Object createResource(@Valid @RequestBody AdminService.ResourceRequest input) { return admin.createResource(input); }
    @DeleteMapping("/resources/{id}") public void removeResource(@PathVariable long id) { admin.removeResource(id); }
    @GetMapping("/reports") public Object report() { return admin.report(); }
}