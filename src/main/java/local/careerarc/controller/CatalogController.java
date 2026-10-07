package local.careerarc.controller;

import local.careerarc.service.CareerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CareerService career;
    public CatalogController(CareerService career) { this.career = career; }
    @GetMapping("/roles") public Object roles() { return career.careerRoles(); }
    @GetMapping("/skills") public Object skills() { return career.skillCatalog(); }
    @GetMapping("/resources") public Object resources(@RequestParam(required = false) String skill,
                                                         @RequestParam(required = false) String role,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) String difficulty) {
        return career.resourceCatalog(skill, role, type, difficulty);
    }
}