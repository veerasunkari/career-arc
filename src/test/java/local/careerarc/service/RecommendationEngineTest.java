package local.careerarc.service;

import local.careerarc.entity.Assessment;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationEngineTest {
    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void matchingSkillsInterestsAndAcademicContextIncreaseMatchWithReasons() {
        Assessment assessment = new Assessment(); assessment.analyticalScore = 90; assessment.practicalScore = 70;
        var result = engine.calculate("Software Engineer", "Software & Data", "Computer Science",
            "software, programming", Map.of("Java", 2, "Git", 2), Map.of("java", 2, "git", 1),
            "Computer Science", "software, programming", "Software & Data", assessment);

        assertEquals(84, result.score());
        assertEquals(java.util.List.of("Java"), result.matchingSkills());
        assertEquals(java.util.List.of("Git"), result.missingSkills());
        assertTrue(result.explanation().contains("academic background"));
        assertTrue(result.explanation().contains("Assessment strongest area: analytical thinking"));
        assertTrue(result.explanation().contains("align with Software & Data"));
    }

    @Test
    void noProfileSignalsProducesLowerScoreThanRelevantProfile() {
        var empty = engine.calculate("Data Analyst", "Software & Data", "Statistics", "data, research",
            Map.of("SQL", 2), Map.of(), null, null, null, null);
        var aligned = engine.calculate("Data Analyst", "Software & Data", "Statistics", "data, research",
            Map.of("SQL", 2), Map.of("sql", 2), "Statistics", "data, research", "Software & Data", null);
        assertTrue(aligned.score() > empty.score());
        assertTrue(empty.score() >= 0 && empty.score() <= 100);
    }
}