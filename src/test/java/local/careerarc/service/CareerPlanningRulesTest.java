package local.careerarc.service;

import local.careerarc.entity.Skill;
import local.careerarc.entity.SkillGap;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CareerPlanningRulesTest {
    @Test
    void comparesCurrentAndRequiredProficiency() {
        assertEquals("MISSING", CareerPlanningRules.gapStatus(0, 2));
        assertEquals("IMPROVE", CareerPlanningRules.gapStatus(1, 2));
        assertEquals("MET", CareerPlanningRules.gapStatus(2, 2));
        assertEquals("MET", CareerPlanningRules.gapStatus(3, 2));
    }

    @Test
    void placesPrerequisiteBeforeDependentSkill() {
        Skill foundation = new Skill("Java", "TECHNICAL"); foundation.id = 1L;
        Skill advanced = new Skill("Spring Boot", "TECHNICAL"); advanced.id = 2L; advanced.prerequisite = foundation;
        SkillGap dependentGap = gap(advanced, 1);
        SkillGap foundationGap = gap(foundation, 2);

        List<SkillGap> ordered = CareerPlanningRules.prerequisiteOrder(List.of(dependentGap, foundationGap));

        assertEquals(List.of(foundation, advanced), ordered.stream().map(item -> item.skill).toList());
    }

    @Test
    void calculatesProgressFromStoredCompletionCount() {
        assertEquals(0, CareerPlanningRules.progress(0, 0));
        assertEquals(33, CareerPlanningRules.progress(1, 3));
        assertEquals(100, CareerPlanningRules.progress(3, 3));
    }

    private SkillGap gap(Skill skill, int priority) {
        SkillGap result = new SkillGap(); result.skill = skill; result.priority = priority; return result;
    }
}