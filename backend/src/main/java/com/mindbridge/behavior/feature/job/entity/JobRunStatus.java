package com.mindbridge.behavior.feature.job.entity;

public enum JobRunStatus {
    RUNNING,
    SUCCEEDED,
    PARTIAL,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == PARTIAL || this == FAILED;
    }
}
