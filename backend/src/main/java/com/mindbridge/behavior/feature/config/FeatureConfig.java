package com.mindbridge.behavior.feature.config;

import java.math.BigDecimal;
import java.util.Objects;

public final class FeatureConfig {

    private final BigDecimal minInferredConfidence;

    private FeatureConfig(BigDecimal minInferredConfidence) {
        this.minInferredConfidence = minInferredConfidence;
    }

    public static FeatureConfig of(BigDecimal minInferredConfidence) {
        if (minInferredConfidence != null) {
            if (minInferredConfidence.signum() < 0 || minInferredConfidence.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException(
                        "minInferredConfidence must be in [0, 1] or null; got " + minInferredConfidence);
            }
        }
        return new FeatureConfig(minInferredConfidence);
    }

    public static FeatureConfig defaults() {
        return new FeatureConfig(null);
    }

    public BigDecimal getMinInferredConfidence() {
        return minInferredConfidence;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FeatureConfig that)) return false;
        return Objects.equals(this.minInferredConfidence, that.minInferredConfidence);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(minInferredConfidence);
    }

    @Override
    public String toString() {
        return "FeatureConfig{minInferredConfidence=" + minInferredConfidence + "}";
    }
}