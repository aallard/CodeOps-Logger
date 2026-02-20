package com.codeops.logger.service;

import com.codeops.logger.dto.mapper.RetentionPolicyMapper;
import com.codeops.logger.dto.request.CreateRetentionPolicyRequest;
import com.codeops.logger.dto.request.UpdateRetentionPolicyRequest;
import com.codeops.logger.dto.response.RetentionPolicyResponse;
import com.codeops.logger.dto.response.StorageUsageResponse;
import com.codeops.logger.entity.RetentionPolicy;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.entity.enums.RetentionAction;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.MetricSeriesRepository;
import com.codeops.logger.repository.RetentionPolicyRepository;
import com.codeops.logger.repository.TraceSpanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages retention policy CRUD operations and storage usage reporting.
 * Policies define how long log data is kept and what action (purge or archive)
 * is taken when data exceeds the retention period.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RetentionService {

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final RetentionPolicyMapper retentionPolicyMapper;
    private final LogEntryRepository logEntryRepository;
    private final MetricSeriesRepository metricSeriesRepository;
    private final TraceSpanRepository traceSpanRepository;

    /**
     * Creates a new retention policy for the given team.
     *
     * @param request the policy creation request
     * @param teamId  the team that owns this policy
     * @param userId  the user creating the policy
     * @return the created policy response
     * @throws ValidationException if ARCHIVE action is missing an archive destination
     */
    public RetentionPolicyResponse createPolicy(CreateRetentionPolicyRequest request, UUID teamId, UUID userId) {
        validateArchiveDestination(
                RetentionAction.valueOf(request.action().toUpperCase()),
                request.archiveDestination()
        );

        RetentionPolicy entity = retentionPolicyMapper.toEntity(request);
        entity.setTeamId(teamId);
        entity.setCreatedBy(userId);

        RetentionPolicy saved = retentionPolicyRepository.save(entity);
        log.info("Created retention policy '{}' (id={}) for team {}", saved.getName(), saved.getId(), teamId);
        return retentionPolicyMapper.toResponse(saved);
    }

    /**
     * Updates an existing retention policy with partial fields.
     *
     * @param policyId the policy ID to update
     * @param request  the partial update request
     * @param teamId   the team that owns this policy (for authorization)
     * @return the updated policy response
     * @throws NotFoundException   if the policy does not exist or belongs to another team
     * @throws ValidationException if ARCHIVE action is missing an archive destination after update
     */
    public RetentionPolicyResponse updatePolicy(UUID policyId, UpdateRetentionPolicyRequest request, UUID teamId) {
        RetentionPolicy policy = findPolicyByIdAndTeam(policyId, teamId);

        if (request.name() != null) {
            policy.setName(request.name());
        }
        if (request.sourceName() != null) {
            policy.setSourceName(request.sourceName());
        }
        if (request.logLevel() != null) {
            policy.setLogLevel(LogLevel.valueOf(request.logLevel().toUpperCase()));
        }
        if (request.retentionDays() != null) {
            policy.setRetentionDays(request.retentionDays());
        }
        if (request.action() != null) {
            policy.setAction(RetentionAction.valueOf(request.action().toUpperCase()));
        }
        if (request.archiveDestination() != null) {
            policy.setArchiveDestination(request.archiveDestination());
        }
        if (request.isActive() != null) {
            policy.setIsActive(request.isActive());
        }

        validateArchiveDestination(policy.getAction(), policy.getArchiveDestination());

        RetentionPolicy saved = retentionPolicyRepository.save(policy);
        log.info("Updated retention policy '{}' ({})", saved.getName(), policyId);
        return retentionPolicyMapper.toResponse(saved);
    }

    /**
     * Retrieves a retention policy by ID, scoped to the given team.
     *
     * @param policyId the policy ID
     * @param teamId   the owning team ID
     * @return the policy response
     * @throws NotFoundException if the policy does not exist or belongs to another team
     */
    @Transactional(readOnly = true)
    public RetentionPolicyResponse getPolicy(UUID policyId, UUID teamId) {
        return retentionPolicyMapper.toResponse(findPolicyByIdAndTeam(policyId, teamId));
    }

    /**
     * Lists all retention policies for a team.
     *
     * @param teamId the team ID
     * @return list of policy responses
     */
    @Transactional(readOnly = true)
    public List<RetentionPolicyResponse> getPoliciesByTeam(UUID teamId) {
        return retentionPolicyMapper.toResponseList(retentionPolicyRepository.findByTeamId(teamId));
    }

    /**
     * Deletes a retention policy by ID, scoped to the given team.
     *
     * @param policyId the policy ID to delete
     * @param teamId   the owning team ID
     * @throws NotFoundException if the policy does not exist or belongs to another team
     */
    public void deletePolicy(UUID policyId, UUID teamId) {
        RetentionPolicy policy = findPolicyByIdAndTeam(policyId, teamId);
        retentionPolicyRepository.delete(policy);
        log.info("Deleted retention policy '{}' ({})", policy.getName(), policyId);
    }

    /**
     * Activates or deactivates a retention policy.
     *
     * @param policyId the policy ID
     * @param teamId   the owning team ID
     * @param active   true to activate, false to deactivate
     * @return the updated policy response
     * @throws NotFoundException if the policy does not exist or belongs to another team
     */
    public RetentionPolicyResponse togglePolicyActive(UUID policyId, UUID teamId, boolean active) {
        RetentionPolicy policy = findPolicyByIdAndTeam(policyId, teamId);
        policy.setIsActive(active);
        RetentionPolicy saved = retentionPolicyRepository.save(policy);
        log.info("{} retention policy '{}' ({})", active ? "Activated" : "Deactivated", saved.getName(), policyId);
        return retentionPolicyMapper.toResponse(saved);
    }

    /**
     * Computes storage usage statistics across all log entries, metric data points,
     * and trace spans, along with active retention policy counts.
     *
     * @return the storage usage summary
     */
    @Transactional(readOnly = true)
    public StorageUsageResponse getStorageUsage() {
        long totalLogs = logEntryRepository.count();
        long totalMetrics = metricSeriesRepository.count();
        long totalSpans = traceSpanRepository.count();

        Map<String, Long> byService = new LinkedHashMap<>();
        for (Object[] row : logEntryRepository.countGroupByServiceName()) {
            byService.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (Object[] row : logEntryRepository.countGroupByLevel()) {
            byLevel.put(((LogLevel) row[0]).name(), (Long) row[1]);
        }

        int activePolicies = retentionPolicyRepository.findByIsActiveTrue().size();
        Instant oldest = logEntryRepository.findOldestTimestamp().orElse(null);
        Instant newest = logEntryRepository.findNewestTimestamp().orElse(null);

        return new StorageUsageResponse(
                totalLogs, totalMetrics, totalSpans,
                byService, byLevel,
                activePolicies, oldest, newest
        );
    }

    /**
     * Finds a policy by ID and validates team ownership.
     *
     * @param policyId the policy ID
     * @param teamId   the expected team ID
     * @return the retention policy entity
     * @throws NotFoundException if not found or team mismatch
     */
    private RetentionPolicy findPolicyByIdAndTeam(UUID policyId, UUID teamId) {
        RetentionPolicy policy = retentionPolicyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException("Retention policy not found: " + policyId));
        if (!policy.getTeamId().equals(teamId)) {
            throw new NotFoundException("Retention policy not found: " + policyId);
        }
        return policy;
    }

    /**
     * Validates that ARCHIVE actions include a non-blank archive destination.
     *
     * @param action             the retention action
     * @param archiveDestination the archive destination path
     * @throws ValidationException if ARCHIVE action has no destination
     */
    private void validateArchiveDestination(RetentionAction action, String archiveDestination) {
        if (action == RetentionAction.ARCHIVE
                && (archiveDestination == null || archiveDestination.isBlank())) {
            throw new ValidationException("Archive destination is required for ARCHIVE action");
        }
    }
}
