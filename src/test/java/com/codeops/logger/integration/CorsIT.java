package com.codeops.logger.integration;

import com.codeops.logger.config.AppConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test verifying CORS behavior through the full MVC stack.
 */
@AutoConfigureMockMvc
class CorsIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromAllowedOriginReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options(AppConstants.API_PREFIX + "/health")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void requestFromDisallowedOriginLacksCorsHeaders() throws Exception {
        mockMvc.perform(get(AppConstants.API_PREFIX + "/health")
                        .header("Origin", "http://evil.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
