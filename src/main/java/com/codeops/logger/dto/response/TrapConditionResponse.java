package com.codeops.logger.dto.response;

import java.util.UUID;

/**
 * Response DTO for a trap condition within a log trap.
 */
public record TrapConditionResponse(
        UUID id,
        String conditionType,
        String field,
        String pattern,
        Integer threshold,
        Integer windowSeconds,
        String serviceName,
        String logLevel
) {}
