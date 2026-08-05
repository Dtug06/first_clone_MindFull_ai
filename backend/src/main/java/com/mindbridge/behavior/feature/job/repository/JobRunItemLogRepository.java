package com.mindbridge.behavior.feature.job.repository;

import com.mindbridge.behavior.feature.job.entity.JobRunItemLog;
import com.mindbridge.behavior.feature.job.entity.JobRunItemLogStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRunItemLogRepository extends JpaRepository<JobRunItemLog, UUID> {
    List<JobRunItemLog> findByJobRunIdOrderByCreatedAtAsc(UUID jobRunId);
    long countByJobRunIdAndStatus(UUID jobRunId, JobRunItemLogStatus status);
}
