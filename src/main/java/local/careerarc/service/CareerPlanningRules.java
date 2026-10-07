package local.careerarc.service;

import local.careerarc.entity.SkillGap;
import java.util.*;

public final class CareerPlanningRules {
    private CareerPlanningRules() {}

    public static String gapStatus(int currentLevel, int requiredLevel) {
        if (currentLevel <= 0) return "MISSING";
        return currentLevel < requiredLevel ? "IMPROVE" : "MET";
    }

    public static int progress(int completed, int total) {
        return total <= 0 ? 0 : (int) Math.round(completed * 100.0 / total);
    }

    public static List<SkillGap> prerequisiteOrder(List<SkillGap> source) {
        List<SkillGap> remaining = new ArrayList<>(source);
        List<SkillGap> ordered = new ArrayList<>();
        Set<Long> included = new HashSet<>();
        while (!remaining.isEmpty()) {
            SkillGap next = remaining.stream().filter(item -> item.skill.prerequisite == null
                    || included.contains(item.skill.prerequisite.id)
                    || source.stream().noneMatch(gap -> gap.skill.id.equals(item.skill.prerequisite.id)))
                .min(Comparator.comparingInt((SkillGap item) -> item.priority).thenComparing(item -> item.skill.name))
                .orElse(remaining.get(0));
            ordered.add(next); included.add(next.skill.id); remaining.remove(next);
        }
        return ordered;
    }
}