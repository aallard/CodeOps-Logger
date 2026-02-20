package com.codeops.logger.exception;

import com.codeops.logger.config.AppConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link GlobalExceptionHandler} verifying that
 * Spring framework exceptions for client input errors return the correct HTTP status
 * codes and structured error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String API = AppConstants.API_PREFIX;

    /**
     * Verifies that a missing required {@code @RequestParam} returns 400
     * with a message identifying the missing parameter.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testMissingRequestParameter_returns400() throws Exception {
        mockMvc.perform(get(API + "/logs/search")
                        .header("X-Team-Id", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Missing required parameter")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("'q'")));
    }

    /**
     * Verifies that a type mismatch on a UUID path variable returns 400
     * with a message identifying the invalid value.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testTypeMismatch_returns400() throws Exception {
        mockMvc.perform(get(API + "/logs/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid value")));
    }

    /**
     * Verifies that a missing {@code X-Team-Id} header on a team-scoped endpoint returns 400.
     * The header is extracted manually by {@link com.codeops.logger.controller.BaseController},
     * which throws {@link ValidationException} when absent.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testMissingTeamIdHeader_returns400() throws Exception {
        mockMvc.perform(get(API + "/sources"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("X-Team-Id")));
    }

    /**
     * Verifies that a malformed JSON request body returns 400
     * with a "Malformed request body" message.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testMalformedRequestBody_returns400() throws Exception {
        mockMvc.perform(post(API + "/logs")
                        .header("X-Team-Id", "00000000-0000-0000-0000-000000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    /**
     * Verifies that an unsupported HTTP method returns 405
     * with a message identifying the rejected method.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testMethodNotAllowed_returns405() throws Exception {
        mockMvc.perform(delete(API + "/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not supported")));
    }
}
