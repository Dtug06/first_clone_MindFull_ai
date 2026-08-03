package com.mindbridge.dailyquestion.repository;

import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyQuestionTemplateRepository extends JpaRepository<DailyQuestionTemplate, UUID> {

    /** Returns all versions for a code, ordered by version descending. */
    List<DailyQuestionTemplate> findByCodeOrderByVersionDesc(String code);

    /** Returns the latest (highest version number) template for a code. */
    Optional<DailyQuestionTemplate> findTopByCodeOrderByVersionDesc(String code);

    /** Returns all latest-version templates (one per code). */
    default List<DailyQuestionTemplate> findLatestVersions() {
        return findAllByOrderByCodeAscVersionDesc().stream()
                .collect(Collectors.toMap(
                        DailyQuestionTemplate::getCode,
                        t -> t,
                        (older, newer) ->
                                newer.getVersion() > older.getVersion() ? newer : older))
                .values()
                .stream()
                .sorted(Comparator.comparing(DailyQuestionTemplate::getCode))
                .toList();
    }

    /** Returns the latest APPROVED template for a code. */
    Optional<DailyQuestionTemplate> findFirstByCodeAndStatusOrderByVersionDesc(
            String code, TemplateStatus status);

    /** Returns latest-version APPROVED templates for assignment purposes. */
    default List<DailyQuestionTemplate> findLatestApproved() {
        return findAllByOrderByCodeAscVersionDesc().stream()
                .filter(t -> t.getStatus() == TemplateStatus.APPROVED)
                .collect(Collectors.toMap(
                        DailyQuestionTemplate::getCode,
                        t -> t,
                        (older, newer) ->
                                newer.getVersion() > older.getVersion() ? newer : older))
                .values()
                .stream()
                .sorted(Comparator.comparing(DailyQuestionTemplate::getCode))
                .toList();
    }

    /** Returns the highest version number for a code, or 0 if none. */
    default int maxVersionByCode(String code) {
        return findByCodeOrderByVersionDesc(code).stream()
                .mapToInt(DailyQuestionTemplate::getVersion)
                .max()
                .orElse(0);
    }

    /** Checks whether a specific (code, version) pair exists. */
    boolean existsByCodeAndVersion(String code, Integer version);

    /** Returns all templates ordered by code, then version descending. */
    List<DailyQuestionTemplate> findAllByOrderByCodeAscVersionDesc();
}
