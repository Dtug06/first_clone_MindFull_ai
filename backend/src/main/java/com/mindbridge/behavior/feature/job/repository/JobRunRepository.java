package com.mindbridge.behavior.feature.job.repository;

import com.mindbridge.behavior.feature.job.entity.JobRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, UUID> {
    Optional<JobRun> findFirstByJobNameOrderByStartedAtDesc(String jobName);
}
