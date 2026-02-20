package com.codeops.logger.service;

import com.codeops.logger.dto.mapper.LogSourceMapper;
import com.codeops.logger.dto.request.CreateLogSourceRequest;
import com.codeops.logger.dto.request.UpdateLogSourceRequest;
import com.codeops.logger.dto.response.LogSourceResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.entity.LogSource;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LogSourceService}.
 */
@ExtendWith(MockitoExtension.class)
class LogSourceServiceTest {

    @Mock
    private LogSourceRepository logSourceRepository;

    @Mock
    private LogSourceMapper logSourceMapper;

    @InjectMocks
    private LogSourceService logSourceService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID SOURCE_ID = UUID.randomUUID();

    @Test
    void testCreateSource_success() {
        CreateLogSourceRequest request = new CreateLogSourceRequest("my-service", null, "Test service", "production");
        LogSource entity = new LogSource();
        entity.setName("my-service");
        LogSource saved = new LogSource();
        saved.setName("my-service");
        LogSourceResponse response = new LogSourceResponse(SOURCE_ID, "my-service", null, "Test service",
                "production", true, TEAM_ID, null, 0L, Instant.now(), Instant.now());

        when(logSourceRepository.existsByTeamIdAndName(TEAM_ID, "my-service")).thenReturn(false);
        when(logSourceMapper.toEntity(request)).thenReturn(entity);
        when(logSourceRepository.save(entity)).thenReturn(saved);
        when(logSourceMapper.toResponse(saved)).thenReturn(response);

        LogSourceResponse result = logSourceService.createSource(request, TEAM_ID);

        assertThat(result).isEqualTo(response);
        verify(logSourceRepository).save(entity);
    }

    @Test
    void testCreateSource_duplicateName_throwsValidation() {
        CreateLogSourceRequest request = new CreateLogSourceRequest("my-service", null, null, null);

        when(logSourceRepository.existsByTeamIdAndName(TEAM_ID, "my-service")).thenReturn(true);

        assertThatThrownBy(() -> logSourceService.createSource(request, TEAM_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void testGetSourcesByTeam_returnsList() {
        LogSource source = new LogSource();
        LogSourceResponse response = new LogSourceResponse(SOURCE_ID, "svc", null, null, null,
                true, TEAM_ID, null, 0L, Instant.now(), Instant.now());

        when(logSourceRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(source));
        when(logSourceMapper.toResponseList(List.of(source))).thenReturn(List.of(response));

        List<LogSourceResponse> result = logSourceService.getSourcesByTeam(TEAM_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetSourcesByTeamPaged_returnsPage() {
        LogSource source = new LogSource();
        Page<LogSource> springPage = new PageImpl<>(List.of(source));
        LogSourceResponse response = new LogSourceResponse(SOURCE_ID, "svc", null, null, null,
                true, TEAM_ID, null, 0L, Instant.now(), Instant.now());

        when(logSourceRepository.findByTeamId(eq(TEAM_ID), any(Pageable.class))).thenReturn(springPage);
        when(logSourceMapper.toResponseList(springPage.getContent())).thenReturn(List.of(response));

        PageResponse<LogSourceResponse> result = logSourceService.getSourcesByTeamPaged(TEAM_ID, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void testGetSource_found() {
        LogSource source = new LogSource();
        LogSourceResponse response = new LogSourceResponse(SOURCE_ID, "svc", null, null, null,
                true, TEAM_ID, null, 0L, Instant.now(), Instant.now());

        when(logSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(logSourceMapper.toResponse(source)).thenReturn(response);

        LogSourceResponse result = logSourceService.getSource(SOURCE_ID);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void testGetSource_notFound_throwsNotFound() {
        when(logSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logSourceService.getSource(SOURCE_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(SOURCE_ID.toString());
    }

    @Test
    void testUpdateSource_success() {
        LogSource source = new LogSource();
        source.setName("old-name");
        source.setDescription("old desc");
        source.setEnvironment("dev");
        source.setIsActive(true);
        UpdateLogSourceRequest request = new UpdateLogSourceRequest("new-name", "new desc", "prod", false);
        LogSourceResponse response = new LogSourceResponse(SOURCE_ID, "new-name", null, "new desc",
                "prod", false, TEAM_ID, null, 0L, Instant.now(), Instant.now());

        when(logSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(logSourceRepository.save(source)).thenReturn(source);
        when(logSourceMapper.toResponse(source)).thenReturn(response);

        LogSourceResponse result = logSourceService.updateSource(SOURCE_ID, request);

        assertThat(result.name()).isEqualTo("new-name");
        assertThat(source.getName()).isEqualTo("new-name");
        assertThat(source.getDescription()).isEqualTo("new desc");
        assertThat(source.getEnvironment()).isEqualTo("prod");
        assertThat(source.getIsActive()).isFalse();
    }

    @Test
    void testUpdateSource_partialUpdate() {
        LogSource source = new LogSource();
        source.setName("old-name");
        source.setDescription("old desc");
        source.setEnvironment("dev");
        source.setIsActive(true);
        UpdateLogSourceRequest request = new UpdateLogSourceRequest("new-name", null, null, null);
        LogSourceResponse response = new LogSourceResponse(SOURCE_ID, "new-name", null, "old desc",
                "dev", true, TEAM_ID, null, 0L, Instant.now(), Instant.now());

        when(logSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(logSourceRepository.save(source)).thenReturn(source);
        when(logSourceMapper.toResponse(source)).thenReturn(response);

        logSourceService.updateSource(SOURCE_ID, request);

        assertThat(source.getName()).isEqualTo("new-name");
        assertThat(source.getDescription()).isEqualTo("old desc");
        assertThat(source.getEnvironment()).isEqualTo("dev");
        assertThat(source.getIsActive()).isTrue();
    }

    @Test
    void testUpdateSource_notFound_throwsNotFound() {
        UpdateLogSourceRequest request = new UpdateLogSourceRequest("x", null, null, null);

        when(logSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logSourceService.updateSource(SOURCE_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testDeleteSource_success() {
        LogSource source = new LogSource();
        source.setName("test");

        when(logSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));

        logSourceService.deleteSource(SOURCE_ID);

        verify(logSourceRepository).delete(source);
    }

    @Test
    void testDeleteSource_notFound_throwsNotFound() {
        when(logSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logSourceService.deleteSource(SOURCE_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
