package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.LogTrapMapper;
import com.codeops.logger.dto.request.CreateLogTrapRequest;
import com.codeops.logger.dto.request.CreateTrapConditionRequest;
import com.codeops.logger.dto.request.UpdateLogTrapRequest;
import com.codeops.logger.dto.response.LogTrapResponse;
import com.codeops.logger.dto.response.TrapConditionResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LogTrapService}.
 */
@ExtendWith(MockitoExtension.class)
class LogTrapServiceTest {

    @Mock
    private LogTrapRepository logTrapRepository;

    @Mock
    private TrapConditionRepository trapConditionRepository;

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private LogTrapMapper logTrapMapper;

    @Mock
    private TrapEvaluationEngine evaluationEngine;

    @InjectMocks
    private LogTrapService logTrapService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private CreateLogTrapRequest createValidRequest() {
        CreateTrapConditionRequest condition = new CreateTrapConditionRequest(
                "KEYWORD", "message", "error", null, null, null, null);
        return new CreateLogTrapRequest("Test Trap", "Detect errors", "PATTERN", List.of(condition));
    }

    private LogTrap createTrap(String name, TrapType type) {
        LogTrap trap = new LogTrap();
        trap.setId(UUID.randomUUID());
        trap.setName(name);
        trap.setTrapType(type);
        trap.setTeamId(TEAM_ID);
        trap.setCreatedBy(USER_ID);
        trap.setIsActive(true);
        trap.setTriggerCount(0L);
        trap.setConditions(new ArrayList<>());
        return trap;
    }

    private LogTrapResponse createResponse(UUID id, String name) {
        return new LogTrapResponse(id, name, null, "PATTERN", true, TEAM_ID, USER_ID,
                null, 0L, List.of(), Instant.now(), Instant.now());
    }

    @Test
    void testCreateTrap_valid_succeeds() {
        CreateLogTrapRequest request = createValidRequest();
        LogTrap entity = createTrap("Test Trap", TrapType.PATTERN);
        LogTrapResponse response = createResponse(entity.getId(), "Test Trap");

        when(logTrapRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(logTrapMapper.toEntity(request)).thenReturn(entity);
        when(logTrapMapper.toConditionEntity(any(CreateTrapConditionRequest.class)))
                .thenReturn(new TrapCondition());
        when(logTrapRepository.save(any(LogTrap.class))).thenReturn(entity);
        when(logTrapMapper.toResponse(entity)).thenReturn(response);

        LogTrapResponse result = logTrapService.createTrap(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("Test Trap");
        verify(logTrapRepository).save(any(LogTrap.class));
    }

    @Test
    void testCreateTrap_exceedsMaxTraps_throwsValidation() {
        when(logTrapRepository.countByTeamId(TEAM_ID)).thenReturn((long) AppConstants.MAX_TRAPS_PER_TEAM);

        assertThatThrownBy(() -> logTrapService.createTrap(createValidRequest(), TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum trap limit");
    }

    @Test
    void testCreateTrap_exceedsMaxConditions_throwsValidation() {
        List<CreateTrapConditionRequest> conditions = new ArrayList<>();
        for (int i = 0; i < AppConstants.MAX_TRAP_CONDITIONS + 1; i++) {
            conditions.add(new CreateTrapConditionRequest(
                    "KEYWORD", "message", "error" + i, null, null, null, null));
        }
        CreateLogTrapRequest request = new CreateLogTrapRequest(
                "Trap", "desc", "PATTERN", conditions);

        when(logTrapRepository.countByTeamId(TEAM_ID)).thenReturn(0L);

        assertThatThrownBy(() -> logTrapService.createTrap(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceed maximum");
    }

    @Test
    void testCreateTrap_invalidRegex_throwsValidation() {
        CreateTrapConditionRequest badCondition = new CreateTrapConditionRequest(
                "REGEX", "message", "[invalid", null, null, null, null);
        CreateLogTrapRequest request = new CreateLogTrapRequest(
                "Trap", "desc", "PATTERN", List.of(badCondition));

        LogTrap entity = createTrap("Trap", TrapType.PATTERN);
        when(logTrapRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(logTrapMapper.toEntity(request)).thenReturn(entity);

        assertThatThrownBy(() -> logTrapService.createTrap(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid regex pattern");
    }

    @Test
    void testCreateTrap_frequencyMissingThreshold_throwsValidation() {
        CreateTrapConditionRequest badCondition = new CreateTrapConditionRequest(
                "FREQUENCY_THRESHOLD", "message", null, null, 60, null, null);
        CreateLogTrapRequest request = new CreateLogTrapRequest(
                "Freq Trap", "desc", "FREQUENCY", List.of(badCondition));

        LogTrap entity = createTrap("Freq Trap", TrapType.FREQUENCY);
        when(logTrapRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(logTrapMapper.toEntity(request)).thenReturn(entity);

        assertThatThrownBy(() -> logTrapService.createTrap(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("threshold");
    }

    @Test
    void testGetTrapsByTeam_returnsList() {
        LogTrap trap1 = createTrap("Trap1", TrapType.PATTERN);
        LogTrap trap2 = createTrap("Trap2", TrapType.FREQUENCY);
        List<LogTrap> traps = List.of(trap1, trap2);

        LogTrapResponse r1 = createResponse(trap1.getId(), "Trap1");
        LogTrapResponse r2 = createResponse(trap2.getId(), "Trap2");

        when(logTrapRepository.findByTeamId(TEAM_ID)).thenReturn(traps);
        when(logTrapMapper.toResponseList(traps)).thenReturn(List.of(r1, r2));

        List<LogTrapResponse> result = logTrapService.getTrapsByTeam(TEAM_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void testGetTrap_found_returnsWithConditions() {
        UUID trapId = UUID.randomUUID();
        LogTrap trap = createTrap("Found", TrapType.PATTERN);
        trap.setId(trapId);
        TrapCondition condition = new TrapCondition();
        condition.setConditionType(ConditionType.KEYWORD);
        condition.setField("message");
        condition.setPattern("error");
        trap.getConditions().add(condition);

        TrapConditionResponse condResp = new TrapConditionResponse(
                UUID.randomUUID(), "KEYWORD", "message", "error", null, null, null, null);
        LogTrapResponse response = new LogTrapResponse(
                trapId, "Found", null, "PATTERN", true, TEAM_ID, USER_ID,
                null, 0L, List.of(condResp), Instant.now(), Instant.now());

        when(logTrapRepository.findById(trapId)).thenReturn(Optional.of(trap));
        when(logTrapMapper.toResponse(trap)).thenReturn(response);

        LogTrapResponse result = logTrapService.getTrap(trapId);

        assertThat(result.name()).isEqualTo("Found");
        assertThat(result.conditions()).hasSize(1);
    }

    @Test
    void testGetTrap_notFound_throwsNotFound() {
        UUID trapId = UUID.randomUUID();
        when(logTrapRepository.findById(trapId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logTrapService.getTrap(trapId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Log trap not found");
    }

    @Test
    void testUpdateTrap_updatesFieldsAndConditions() {
        UUID trapId = UUID.randomUUID();
        LogTrap existing = createTrap("Old Name", TrapType.PATTERN);
        existing.setId(trapId);

        CreateTrapConditionRequest newCond = new CreateTrapConditionRequest(
                "KEYWORD", "message", "new_keyword", null, null, null, null);
        UpdateLogTrapRequest request = new UpdateLogTrapRequest(
                "New Name", "New Desc", null, null, List.of(newCond));

        TrapCondition condEntity = new TrapCondition();
        condEntity.setConditionType(ConditionType.KEYWORD);
        condEntity.setField("message");

        LogTrapResponse response = createResponse(trapId, "New Name");

        when(logTrapRepository.findById(trapId)).thenReturn(Optional.of(existing));
        when(logTrapMapper.toConditionEntity(any(CreateTrapConditionRequest.class)))
                .thenReturn(condEntity);
        when(logTrapRepository.save(any(LogTrap.class))).thenReturn(existing);
        when(logTrapMapper.toResponse(existing)).thenReturn(response);

        LogTrapResponse result = logTrapService.updateTrap(trapId, request, USER_ID);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDescription()).isEqualTo("New Desc");
    }

    @Test
    void testDeleteTrap_succeeds() {
        UUID trapId = UUID.randomUUID();
        LogTrap trap = createTrap("To Delete", TrapType.PATTERN);
        trap.setId(trapId);

        when(logTrapRepository.findById(trapId)).thenReturn(Optional.of(trap));

        logTrapService.deleteTrap(trapId);

        verify(logTrapRepository).delete(trap);
    }

    @Test
    void testToggleTrap_flipsActive() {
        UUID trapId = UUID.randomUUID();
        LogTrap trap = createTrap("Toggle Me", TrapType.PATTERN);
        trap.setId(trapId);
        trap.setIsActive(true);

        LogTrapResponse response = new LogTrapResponse(
                trapId, "Toggle Me", null, "PATTERN", false, TEAM_ID, USER_ID,
                null, 0L, List.of(), Instant.now(), Instant.now());

        when(logTrapRepository.findById(trapId)).thenReturn(Optional.of(trap));
        when(logTrapRepository.save(any(LogTrap.class))).thenReturn(trap);
        when(logTrapMapper.toResponse(trap)).thenReturn(response);

        logTrapService.toggleTrap(trapId);

        assertThat(trap.getIsActive()).isFalse();
        verify(logTrapRepository).save(trap);
    }

    @Test
    void testEvaluateEntry_patternTrap_fires() {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID());
        entry.setTeamId(TEAM_ID);
        entry.setServiceName("svc");
        entry.setLevel(LogLevel.ERROR);
        entry.setMessage("Connection timeout");

        LogTrap trap = createTrap("Error Trap", TrapType.PATTERN);
        trap.setTriggerCount(5L);

        when(logTrapRepository.findByTeamIdAndIsActiveTrue(TEAM_ID)).thenReturn(List.of(trap));
        when(evaluationEngine.evaluatePatternConditions(eq(entry), any())).thenReturn(true);
        when(logTrapRepository.save(any(LogTrap.class))).thenReturn(trap);

        List<UUID> firedIds = logTrapService.evaluateEntry(entry);

        assertThat(firedIds).hasSize(1).contains(trap.getId());
        assertThat(trap.getTriggerCount()).isEqualTo(6L);
        assertThat(trap.getLastTriggeredAt()).isNotNull();
    }

    @Test
    void testEvaluateEntry_noMatch_returnsEmpty() {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID());
        entry.setTeamId(TEAM_ID);

        LogTrap trap = createTrap("No Match", TrapType.PATTERN);

        when(logTrapRepository.findByTeamIdAndIsActiveTrue(TEAM_ID)).thenReturn(List.of(trap));
        when(evaluationEngine.evaluatePatternConditions(eq(entry), any())).thenReturn(false);

        List<UUID> firedIds = logTrapService.evaluateEntry(entry);

        assertThat(firedIds).isEmpty();
        verify(logTrapRepository, never()).save(any(LogTrap.class));
    }
}
