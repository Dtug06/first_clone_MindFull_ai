package com.mindbridge.behavior.feature.job.persistence;

import com.mindbridge.behavior.feature.job.entity.UserDailyFeature;
import java.util.UUID;

public interface UserDailyFeatureUpsertService {
    UUID upsert(UserDailyFeature row);
}
