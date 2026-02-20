package com.codeops.logger.controller;

import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Abstract base controller providing common utilities for all Logger API controllers.
 * Extracts the team ID from the {@code X-Team-Id} request header and the current user ID
 * from the Spring Security context.
 */
public abstract class BaseController {

    /** HTTP header name for the team identifier. */
    protected static final String TEAM_ID_HEADER = "X-Team-Id";

    /**
     * Extracts and validates the team ID from the {@code X-Team-Id} request header.
     *
     * @param request the incoming HTTP request
     * @return the parsed team UUID
     * @throws ValidationException if the header is missing or contains an invalid UUID
     */
    protected UUID extractTeamId(HttpServletRequest request) {
        String teamIdHeader = request.getHeader(TEAM_ID_HEADER);
        if (teamIdHeader == null || teamIdHeader.isBlank()) {
            throw new ValidationException("X-Team-Id header is required");
        }
        try {
            return UUID.fromString(teamIdHeader);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid X-Team-Id header format");
        }
    }

    /**
     * Retrieves the current authenticated user's UUID from the security context.
     *
     * @return the current user's UUID
     */
    protected UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}
