package local.careerarc.service;

import local.careerarc.entity.Assessment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RecommendationEngine {
    private final double skillWeight;
    private final double assessmentWeight;
    private final double interestWeight;
    private final double academicWeight;

    public RecommendationEngine() { this(55, 25, 15, 5); }

    @Autowired
    public RecommendationEngine(@Value("${career.match.weights.skill:55}") double skillWeight,
                                @Value("${career.match.weights.assessment:25}") double assessmentWeight,
                                @Value("${career.match.weights.interest:15}") double interestWeight,
                                @Value("${career.match.weights.academic:5}") double academicWeight) {
        this.skillWeight = skillWeight; this.assessmentWeight = assessmentWeight;
        this.interestWeight = interestWeight; this.academicWeight = academicWeight;
    }

    public MatchResult calculate(String title, String domain, String academicFit, String roleInterests,
                                 Map<String, Integer> required, Map<String, Integer> current,
                                 String academicField, String interests, String preferredDomains, Assessment assessment) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        double skillRatio = 0;
        for (var requirement : required.entrySet()) {
            int level = current.getOrDefault(normalize(requirement.getKey()), 0);
            skillRatio += Math.min(level, requirement.getValue()) / (double) Math.max(1, requirement.getValue());
            if (level >= requirement.getValue()) matched.add(requirement.getKey());
            else missing.add(requirement.getKey());
        }
        if (!required.isEmpty()) skillRatio /= required.size();
        int assessmentScore = assessmentScore(domain + " " + title, assessment);
        boolean interestMatch = overlaps(domain + "," + roleInterests, interests + "," + preferredDomains);
        boolean academicMatch = academicField != null && academicFit != null
            && academicFit.toLowerCase(Locale.ROOT).contains(academicField.toLowerCase(Locale.ROOT));
        int score = (int) Math.round(skillRatio * skillWeight + assessmentScore * assessmentWeight / 100
            + (interestMatch ? interestWeight : interestWeight * 0.267)
            + (academicMatch ? academicWeight : academicWeight * 0.4));
        List<String> factors = new ArrayList<>();
        if (interestMatch) factors.add("Your stated interests align with " + domain + ".");
        if (academicMatch) factors.add("Your academic background is relevant to this role.");
        if (assessment != null) factors.add("Assessment strongest area: " + strongestArea(assessment) + ".");
        if (!matched.isEmpty()) factors.add("You already meet requirements for " + String.join(", ", matched) + ".");
        String explanation = factors.isEmpty() ? "This role is included as a comparison based on its skill profile."
            : String.join(" ", factors);
        return new MatchResult(Math.max(0, Math.min(100, score)), matched, factors, missing,
            title + " matches at " + score + "%. " + explanation);
    }

    private static int assessmentScore(String domain, Assessment assessment) {
        if (assessment == null) return 50;
        String value = domain == null ? "" : domain.toLowerCase(Locale.ROOT);
        if (value.contains("design") || value.contains("creative")) return assessment.creativeScore;
        if (value.contains("data") || value.contains("software") || value.contains("technology")) return assessment.analyticalScore;
        if (value.contains("mentor") || value.contains("counsel") || value.contains("communication")) return assessment.communicationScore;
        return assessment.practicalScore;
    }

    private static String strongestArea(Assessment assessment) {
        Map<String, Integer> scores = Map.of("analytical thinking", assessment.analyticalScore,
            "creative thinking", assessment.creativeScore, "communication", assessment.communicationScore,
            "practical problem solving", assessment.practicalScore);
        return scores.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }

    private static boolean overlaps(String left, String right) {
        Set<String> target = tokens(left);
        target.retainAll(tokens(right));
        return !target.isEmpty();
    }

    private static Set<String> tokens(String value) {
        if (value == null) return new HashSet<>();
        Set<String> result = new HashSet<>();
        for (String token : value.toLowerCase(Locale.ROOT).split("[,;| ]+")) if (token.length() > 2) result.add(token.trim());
        return result;
    }

    private static String normalize(String value) { return value.toLowerCase(Locale.ROOT).trim(); }

    public record MatchResult(int score, List<String> matchingSkills, List<String> relevantFactors,
                              List<String> missingSkills, String explanation) {}
}