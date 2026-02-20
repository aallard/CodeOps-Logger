package com.codeops.logger.security;

import com.codeops.logger.exception.AuthorizationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SecurityUtils}.
 */
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserIdReturnsUuid() {
        UUID userId = UUID.randomUUID();
        setAuthentication(userId, "test@test.com", List.of("ADMIN"));

        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(userId);
    }

    @Test
    void getCurrentUserIdThrowsWhenNoAuth() {
        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    void getCurrentUserIdThrowsWhenPrincipalNotUuid() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("string-principal", null, List.of()));
        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Invalid authentication principal");
    }

    @Test
    void getCurrentEmailReturnsEmail() {
        setAuthentication(UUID.randomUUID(), "admin@codeops.dev", List.of("ADMIN"));
        assertThat(SecurityUtils.getCurrentEmail()).isEqualTo("admin@codeops.dev");
    }

    @Test
    void getCurrentEmailThrowsWhenNoAuth() {
        assertThatThrownBy(SecurityUtils::getCurrentEmail)
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void getCurrentRolesReturnsRoles() {
        setAuthentication(UUID.randomUUID(), "test@test.com", List.of("ADMIN", "MEMBER"));
        List<String> roles = SecurityUtils.getCurrentRoles();
        assertThat(roles).containsExactlyInAnyOrder("ADMIN", "MEMBER");
    }

    @Test
    void isAdminReturnsTrueForAdmin() {
        setAuthentication(UUID.randomUUID(), "test@test.com", List.of("ADMIN"));
        assertThat(SecurityUtils.isAdmin()).isTrue();
    }

    @Test
    void isAdminReturnsTrueForOwner() {
        setAuthentication(UUID.randomUUID(), "test@test.com", List.of("OWNER"));
        assertThat(SecurityUtils.isAdmin()).isTrue();
    }

    @Test
    void isAdminReturnsFalseForMember() {
        setAuthentication(UUID.randomUUID(), "test@test.com", List.of("MEMBER"));
        assertThat(SecurityUtils.isAdmin()).isFalse();
    }

    @Test
    void hasRoleReturnsTrueForMatchingRole() {
        setAuthentication(UUID.randomUUID(), "test@test.com", List.of("VIEWER"));
        assertThat(SecurityUtils.hasRole("VIEWER")).isTrue();
    }

    @Test
    void hasRoleReturnsFalseForNoAuth() {
        assertThat(SecurityUtils.hasRole("ADMIN")).isFalse();
    }

    private void setAuthentication(UUID userId, String email, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, email, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
