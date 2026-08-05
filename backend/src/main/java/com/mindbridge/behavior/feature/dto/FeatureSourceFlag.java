package com.mindbridge.behavior.feature.dto;

import java.util.EnumSet;
import java.util.Set;

public enum FeatureSourceFlag {
    EXPLICIT_USED,
    INFERRED_USED,
    BEHAVIORAL_USED,
    SAFETY_USED;

    public static Set<FeatureSourceFlag> fromSources(FeatureSource... sources) {
        EnumSet<FeatureSourceFlag> flags = EnumSet.noneOf(FeatureSourceFlag.class);
        if (sources == null) {
            return flags;
        }
        for (FeatureSource src : sources) {
            if (src == null) {
                continue;
            }
            switch (src) {
                case DAILY_ANSWER -> flags.add(EXPLICIT_USED);
                case INFERRED -> flags.add(INFERRED_USED);
                case BEHAVIORAL -> flags.add(BEHAVIORAL_USED);
                case SAFETY_DERIVED -> flags.add(SAFETY_USED);
                case NONE -> { /* no-op */ }
            }
        }
        return flags;
    }
}