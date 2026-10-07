package local.careerarc.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import local.careerarc.controller.AuthController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InputValidationTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory factory;

    @BeforeAll static void setUp() { factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator(); }
    @AfterAll static void tearDown() { factory.close(); }

    @Test
    void rejectsAssessmentOutsideExpectedRangeAndLength() {
        var input = new CareerService.AssessmentRequest("interests", "domains", List.of(1, 2, 3, 4, 5, 6, 7, 8));
        assertFalse(validator.validate(input).isEmpty());
    }

    @Test
    void rejectsInvalidRegistrationAndAcademicScore() {
        var registration = new AuthController.RegisterRequest("Student", "not-an-email", "short");
        var profile = new CareerService.ProfileRequest("", null, null, null, 120.0, null, null, null);
        assertTrue(validator.validate(registration).size() >= 2);
        assertTrue(validator.validate(profile).size() >= 2);
    }
}