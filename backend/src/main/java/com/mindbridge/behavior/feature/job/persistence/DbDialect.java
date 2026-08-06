package com.mindbridge.behavior.feature.job.persistence;

public enum DbDialect {
    POSTGRESQL,
    H2,
    UNKNOWN;

    public static DbDialect fromJdbcUrl(String url) {
        if (url == null) return UNKNOWN;
        if (url.contains("postgresql:") || url.contains("pgsql:")) return POSTGRESQL;
        if (url.contains("h2:")) return H2;
        return UNKNOWN;
    }
}
