package com.wakfu.parser;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.actors.Player;

import java.util.regex.Matcher;

public class DamageValidator {

    public static boolean shouldIgnoreDamage(Fighter caster, Fighter target, String reason) {
        if (caster.getType() != Fighter.FighterType.PLAYER) {
            return true;
        }

        if (caster instanceof Player && caster.getName().equals(target.getName())) {
            System.out.printf("[Parser] IGNORED (%s): %s → %s%n", reason, caster.getName(), target.getName());
            return true;
        }

        if (caster instanceof Player && target instanceof Player && !caster.getName().equals(target.getName())) {
            System.out.printf("[Parser] IGNORED (%s): %s → %s%n", reason, caster.getName(), target.getName());
            return true;
        }

        return false;
    }

    public static class DamageInfo {
        private final String targetName;
        private final int value;
        private final Element element;

        public DamageInfo(Matcher matcher) {
            this.targetName = matcher.group(1).trim();
            this.value = parseIntSafe(matcher.group(2));
            this.element = Element.fromString(matcher.group(3).trim());
        }

        public String getTargetName() {
            return targetName;
        }

        public int getValue() {
            return value;
        }

        public Element getElement() {
            return element;
        }

        private int parseIntSafe(String s) {
            try {
                String cleaned = s.replaceAll("[^0-9]", "");
                return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
            } catch (Exception e) {
                System.err.println("[Parser] Invalid number: " + s);
                return 0;
            }
        }
    }
}

