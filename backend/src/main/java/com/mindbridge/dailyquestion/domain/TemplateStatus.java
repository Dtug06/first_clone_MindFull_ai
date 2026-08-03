package com.mindbridge.dailyquestion.domain;

/**
 * Lifecycle status of a daily question template version.
 *
 * Only APPROVED templates can be assigned to users.
 * A new version must be created to change content of an assigned template.
 */
public enum TemplateStatus {
    /** Not yet reviewed; cannot be assigned to users. */
    DRAFT,
    /** Reviewed and usable; can be assigned to users. */
    APPROVED,
    /** Superseded by a newer version; not assignable to new users. */
    RETIRED
}
