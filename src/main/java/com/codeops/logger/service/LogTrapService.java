package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.LogTrapMapper;
import com.codeops.logger.dto.request.CreateLogTrapRequest;
import com.codeops.logger.dto.request.CreateTrapConditionRequest;
import com.codeops.logger.dto.request.UpdateLogTrapRequest;
import com.codeops.logger.dto.response.LogTrapResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.TrapTestResult;
import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.LogTrap;
import com.codeops.logger.entity.TrapCondition;
import com.codeops.logger.entity.enums.ConditionType;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.entity.enums.TrapType;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.LogTrapRepository;
import com.codeops.logger.repository.TrapConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Manages log trap lifecycle (CRUD) and provides trap evaluation for incoming log entries.
 * When a log entry is ingested, active traps are evaluated and firing traps are recorded
 * for subsequent alert delivery.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogTrapService {

    private final LogTrapRepository logTrapRepository;
    private final TrapConditionRepository trapConditionRepository;
    private final LogEntryRepository logEntryRepository;
    private final LogTrapMapper logTrapMapper;
    private final TrapEvaluationEngine evaluationEngine;

    // ==================== CRUD ====================

    /**
     * Creates a new log trap with its conditions.
     *
     * @param request the create request with trap details and conditions
     * @param teamId  the team scope
     * @param userId  the creating user
     * @return the created trap response with conditions
     * @throws ValidationException if team has reached MAX_TRAPS_PER_TEAM
     */
    @Transactional
    public LogTrapResponse createTrap(CreateLogTrapRequest request, UUID teamId, UUID userId) {
        long currentCount = logTrapRepository.countByTeamId(teamId);
        if (currentCount >= AppConstants.MAX_TRAPS_PER_TEAM) {
            throw new ValidationException(
                    "Team has reached maximum trap limit (" + AppConstants.MAX_TRAPS_PER_TEAM + ")");
        }

        if (request.conditions().size() > AppConstants.MAX_TRAP_CONDITIONS) {
            throw new ValidationException(
                    "Trap conditions exceed maximum (" + AppConstants.MAX_TRAP_CONDITIONS + ")");
        }

        TrapType trapType = parseTrapType(request.trapType());

        LogTrap trap = logTrapMapper.toEntity(request);
        trap.setTrapType(trapType);
        trap.setTeamId(teamId);
        trap.setCreatedBy(userId);
        trap.setIsActive(true);
        trap.setTriggerCount(0L);
        trap.setConditions(new ArrayList<>());

        for (CreateTrapConditionRequest condReq : request.conditions()) {
            TrapCondition condition = buildCondition(condReq, trap);
            trap.getConditions().add(condition);
        }

        LogTrap saved = logTrapRepository.save(trap);
        log.info("Created trap '{}' ({}) for team {}", saved.getName(), saved.getId(), teamId);
        return logTrapMapper.toResponse(saved);
    }

    /**
     * Returns all traps for a team.
     *
     * @param teamId the team scope
     * @return list of trap responses
     */
    public List<LogTrapResponse> getTrapsByTeam(UUID teamId) {
        List<LogTrap> traps = logTrapRepository.findByTeamId(teamId);
        return logTrapMapper.toResponseList(traps);
    }

    /**
     * Returns a single trap by ID.
     *
     * @param trapId the trap ID
     * @return the trap response with conditions
     * @throws NotFoundException if not found
     */
    public LogTrapResponse getTrap(UUID trapId) {
        LogTrap trap = logTrapRepository.findById(trapId)
                .orElseThrow(() -> new NotFoundException("Log trap not found: " + trapId));
        // Eagerly initialize conditions for the response
        trap.getConditions().size();
        return logTrapMapper.toResponse(trap);
    }

    /**
     * Returns paginated traps for a team.
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated trap responses
     */
    public PageResponse<LogTrapResponse> getTrapsByTeamPaged(UUID teamId, int page, int size) {
        Page<LogTrap> springPage = logTrapRepository.findByTeamId(teamId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<LogTrapResponse> content = logTrapMapper.toResponseList(springPage.getContent());
        return new PageResponse<>(
                content,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }

    /**
     * Updates an existing trap. If conditions are provided in the request,
     * replaces ALL existing conditions (full replacement, not merge).
     *
     * @param trapId  the trap to update
     * @param request the update request
     * @param userId  the updating user
     * @return the updated trap response
     * @throws NotFoundException if not found
     */
    @Transactional
    public LogTrapResponse updateTrap(UUID trapId, UpdateLogTrapRequest request, UUID userId) {
        LogTrap trap = logTrapRepository.findById(trapId)
                .orElseThrow(() -> new NotFoundException("Log trap not found: " + trapId));

        if (request.name() != null) {
            trap.setName(request.name());
        }
        if (request.description() != null) {
            trap.setDescription(request.description());
        }
        if (request.trapType() != null) {
            trap.setTrapType(parseTrapType(request.trapType()));
        }
        if (request.isActive() != null) {
            trap.setIsActive(request.isActive());
        }

        if (request.conditions() != null) {
            if (request.conditions().size() > AppConstants.MAX_TRAP_CONDITIONS) {
                throw new ValidationException(
                        "Trap conditions exceed maximum (" + AppConstants.MAX_TRAP_CONDITIONS + ")");
            }
            trap.getConditions().clear();
            for (CreateTrapConditionRequest condReq : request.conditions()) {
                TrapCondition condition = buildCondition(condReq, trap);
                trap.getConditions().add(condition);
            }
        }

        LogTrap saved = logTrapRepository.save(trap);
        log.info("Updated trap '{}' ({})", saved.getName(), saved.getId());
        return logTrapMapper.toResponse(saved);
    }

    /**
     * Deletes a trap and all its conditions.
     *
     * @param trapId the trap ID
     * @throws NotFoundException if not found
     */
    @Transactional
    public void deleteTrap(UUID trapId) {
        LogTrap trap = logTrapRepository.findById(trapId)
                .orElseThrow(() -> new NotFoundException("Log trap not found: " + trapId));
        logTrapRepository.delete(trap);
        log.info("Deleted trap '{}' ({})", trap.getName(), trapId);
    }

    /**
     * Toggles a trap's active status.
     *
     * @param trapId the trap ID
     * @return the updated trap response
     * @throws NotFoundException if not found
     */
    @Transactional
    public LogTrapResponse toggleTrap(UUID trapId) {
        LogTrap trap = logTrapRepository.findById(trapId)
                .orElseThrow(() -> new NotFoundException("Log trap not found: " + trapId));
        trap.setIsActive(!trap.getIsActive());
        LogTrap saved = logTrapRepository.save(trap);
        log.info("Toggled trap '{}' ({}) — now {}", saved.getName(), trapId,
                saved.getIsActive() ? "active" : "inactive");
        return logTrapMapper.toResponse(saved);
    }

    // ==================== Evaluation ====================

    /**
     * Evaluates a single log entry against all active traps for the team.
     * Returns the list of trap IDs that fired.
     *
     * @param entry the persisted log entry
     * @return list of trap IDs that fired (empty if none)
     */
    @Transactional
    public List<UUID> evaluateEntry(LogEntry entry) {
        List<LogTrap> activeTraps = logTrapRepository.findByTeamIdAndIsActiveTrue(entry.getTeamId());
        List<UUID> firedTrapIds = new ArrayList<>();

        for (LogTrap trap : activeTraps) {
            boolean fired = false;

            if (trap.getTrapType() == TrapType.PATTERN) {
                fired = evaluationEngine.evaluatePatternConditions(entry, trap.getConditions());
            } else if (trap.getTrapType() == TrapType.FREQUENCY) {
                fired = evaluateAllFrequencyConditions(trap.getConditions(), entry.getTeamId());
            }
            // ABSENCE traps are evaluated on schedule, not per-entry

            if (fired) {
                trap.setLastTriggeredAt(Instant.now());
                trap.setTriggerCount(trap.getTriggerCount() + 1);
                logTrapRepository.save(trap);
                firedTrapIds.add(trap.getId());
                log.debug("Trap '{}' ({}) fired for entry {}", trap.getName(), trap.getId(), entry.getId());
            }
        }

        return firedTrapIds;
    }

    // ==================== Trap Testing ====================

    /**
     * Tests a trap against historical log entries to see how many would have matched.
     * Does NOT actually fire the trap or create alerts — purely diagnostic.
     *
     * @param trapId    the trap to test
     * @param hoursBack how many hours of historical logs to test against
     * @return test results with match count and sample IDs
     * @throws NotFoundException if trap not found
     */
    public TrapTestResult testTrap(UUID trapId, int hoursBack) {
        LogTrap trap = logTrapRepository.findById(trapId)
                .orElseThrow(() -> new NotFoundException("Log trap not found: " + trapId));
        trap.getConditions().size(); // initialize lazy collection

        Instant from = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        Instant to = Instant.now();

        Page<LogEntry> entries = logEntryRepository.findByTeamIdAndTimestampBetween(
                trap.getTeamId(), from, to,
                PageRequest.of(0, AppConstants.MAX_QUERY_RESULTS));

        return evaluateEntriesAgainstConditions(entries.getContent(), trap.getConditions(), from, to);
    }

    /**
     * Tests a trap definition (before saving) against historical logs.
     *
     * @param request   the trap definition to test
     * @param teamId    the team scope
     * @param hoursBack hours of history to scan
     * @return test results
     */
    public TrapTestResult testTrapDefinition(CreateLogTrapRequest request, UUID teamId, int hoursBack) {
        List<TrapCondition> conditions = new ArrayList<>();
        for (CreateTrapConditionRequest condReq : request.conditions()) {
            TrapCondition condition = new TrapCondition();
            condition.setConditionType(parseConditionType(condReq.conditionType()));
            condition.setField(condReq.field());
            condition.setPattern(condReq.pattern());
            condition.setThreshold(condReq.threshold());
            condition.setWindowSeconds(condReq.windowSeconds());
            condition.setServiceName(condReq.serviceName());
            if (condReq.logLevel() != null) {
                condition.setLogLevel(parseLogLevel(condReq.logLevel()));
            }
            conditions.add(condition);
        }

        Instant from = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        Instant to = Instant.now();

        Page<LogEntry> entries = logEntryRepository.findByTeamIdAndTimestampBetween(
                teamId, from, to,
                PageRequest.of(0, AppConstants.MAX_QUERY_RESULTS));

        return evaluateEntriesAgainstConditions(entries.getContent(), conditions, from, to);
    }

    // ==================== Internal Helpers ====================

    /**
     * Evaluates a list of log entries against trap conditions and builds a TrapTestResult.
     */
    private TrapTestResult evaluateEntriesAgainstConditions(List<LogEntry> entries,
                                                             List<TrapCondition> conditions,
                                                             Instant from, Instant to) {
        List<UUID> matchIds = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (evaluationEngine.evaluatePatternConditions(entry, conditions)) {
                matchIds.add(entry.getId());
            }
        }

        int maxSamples = Math.min(matchIds.size(), 100);
        List<UUID> sampleIds = matchIds.subList(0, maxSamples);

        return TrapTestResult.of(matchIds.size(), entries.size(), sampleIds, from, to);
    }

    /**
     * Evaluates all frequency conditions for a trap.
     */
    private boolean evaluateAllFrequencyConditions(List<TrapCondition> conditions, UUID teamId) {
        for (TrapCondition condition : conditions) {
            if (condition.getConditionType() == ConditionType.FREQUENCY_THRESHOLD) {
                if (!evaluationEngine.evaluateFrequencyThreshold(condition, teamId)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Builds a TrapCondition entity from a request DTO, performing validation.
     */
    private TrapCondition buildCondition(CreateTrapConditionRequest request, LogTrap trap) {
        ConditionType conditionType = parseConditionType(request.conditionType());

        if (conditionType == ConditionType.REGEX && request.pattern() != null) {
            validateRegexPattern(request.pattern());
        }
        if (conditionType == ConditionType.FREQUENCY_THRESHOLD) {
            if (request.threshold() == null) {
                throw new ValidationException("Frequency threshold condition requires a threshold value");
            }
            if (request.windowSeconds() == null) {
                throw new ValidationException("Frequency threshold condition requires a windowSeconds value");
            }
        }
        if (conditionType == ConditionType.ABSENCE) {
            if (request.windowSeconds() == null) {
                throw new ValidationException("Absence condition requires a windowSeconds value");
            }
        }

        TrapCondition condition = logTrapMapper.toConditionEntity(request);
        condition.setConditionType(conditionType);
        condition.setTrap(trap);
        if (request.logLevel() != null) {
            condition.setLogLevel(parseLogLevel(request.logLevel()));
        }
        return condition;
    }

    /**
     * Parses a trap type string to the TrapType enum.
     *
     * @param trapType the string to parse
     * @return the parsed TrapType
     * @throws ValidationException if the string is not a valid trap type
     */
    private TrapType parseTrapType(String trapType) {
        try {
            return TrapType.valueOf(trapType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid trap type: " + trapType);
        }
    }

    /**
     * Parses a condition type string to the ConditionType enum.
     *
     * @param conditionType the string to parse
     * @return the parsed ConditionType
     * @throws ValidationException if the string is not a valid condition type
     */
    private ConditionType parseConditionType(String conditionType) {
        try {
            return ConditionType.valueOf(conditionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid condition type: " + conditionType);
        }
    }

    /**
     * Parses a log level string to the LogLevel enum.
     *
     * @param logLevel the string to parse
     * @return the parsed LogLevel
     * @throws ValidationException if the string is not a valid log level
     */
    private LogLevel parseLogLevel(String logLevel) {
        try {
            return LogLevel.valueOf(logLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid log level: " + logLevel);
        }
    }

    /**
     * Validates a regex pattern.
     *
     * @param pattern the regex string
     * @throws ValidationException if the pattern is not valid regex
     */
    void validateRegexPattern(String pattern) {
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new ValidationException("Invalid regex pattern: '" + pattern + "' — " + e.getMessage());
        }
    }
}
