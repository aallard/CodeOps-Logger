package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.AlertChannelMapper;
import com.codeops.logger.dto.request.CreateAlertChannelRequest;
import com.codeops.logger.dto.request.UpdateAlertChannelRequest;
import com.codeops.logger.dto.response.AlertChannelResponse;
import com.codeops.logger.entity.AlertChannel;
import com.codeops.logger.entity.enums.AlertChannelType;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.AlertChannelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AlertChannelService}.
 */
@ExtendWith(MockitoExtension.class)
class AlertChannelServiceTest {

    @Mock
    private AlertChannelRepository alertChannelRepository;

    @Mock
    private AlertChannelMapper alertChannelMapper;

    @Mock
    private RestTemplate restTemplate;

    private AlertChannelService alertChannelService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        alertChannelService = new AlertChannelService(
                alertChannelRepository, alertChannelMapper, restTemplate, objectMapper);
    }

    private AlertChannel createChannel(String name, AlertChannelType type) {
        AlertChannel channel = new AlertChannel();
        channel.setId(UUID.randomUUID());
        channel.setName(name);
        channel.setChannelType(type);
        channel.setConfiguration("{}");
        channel.setTeamId(TEAM_ID);
        channel.setCreatedBy(USER_ID);
        channel.setIsActive(true);
        return channel;
    }

    private AlertChannelResponse createResponse(UUID id, String name, String type) {
        return new AlertChannelResponse(id, name, type, "{}", true, TEAM_ID, USER_ID,
                Instant.now(), Instant.now());
    }

    @Test
    void testCreateChannel_email_valid() {
        String config = "{\"recipients\":[\"test@example.com\"],\"subject_prefix\":\"[Alert]\"}";
        CreateAlertChannelRequest request = new CreateAlertChannelRequest("Email Channel", "EMAIL", config);

        AlertChannel entity = createChannel("Email Channel", AlertChannelType.EMAIL);
        AlertChannelResponse response = createResponse(entity.getId(), "Email Channel", "EMAIL");

        when(alertChannelRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(alertChannelMapper.toEntity(request)).thenReturn(entity);
        when(alertChannelRepository.save(any(AlertChannel.class))).thenReturn(entity);
        when(alertChannelMapper.toResponse(entity)).thenReturn(response);

        AlertChannelResponse result = alertChannelService.createChannel(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("Email Channel");
        verify(alertChannelRepository).save(any(AlertChannel.class));
    }

    @Test
    void testCreateChannel_webhook_valid() {
        String config = "{\"url\":\"https://external.example.com/webhook\"}";
        CreateAlertChannelRequest request = new CreateAlertChannelRequest("Webhook", "WEBHOOK", config);

        AlertChannel entity = createChannel("Webhook", AlertChannelType.WEBHOOK);
        AlertChannelResponse response = createResponse(entity.getId(), "Webhook", "WEBHOOK");

        when(alertChannelRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(alertChannelMapper.toEntity(request)).thenReturn(entity);
        when(alertChannelRepository.save(any(AlertChannel.class))).thenReturn(entity);
        when(alertChannelMapper.toResponse(entity)).thenReturn(response);

        AlertChannelResponse result = alertChannelService.createChannel(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("Webhook");
    }

    @Test
    void testCreateChannel_teams_valid() {
        String config = "{\"webhook_url\":\"https://teams.microsoft.com/webhook/abc\"}";
        CreateAlertChannelRequest request = new CreateAlertChannelRequest("Teams", "TEAMS", config);

        AlertChannel entity = createChannel("Teams", AlertChannelType.TEAMS);
        AlertChannelResponse response = createResponse(entity.getId(), "Teams", "TEAMS");

        when(alertChannelRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(alertChannelMapper.toEntity(request)).thenReturn(entity);
        when(alertChannelRepository.save(any(AlertChannel.class))).thenReturn(entity);
        when(alertChannelMapper.toResponse(entity)).thenReturn(response);

        AlertChannelResponse result = alertChannelService.createChannel(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("Teams");
    }

    @Test
    void testCreateChannel_slack_valid() {
        String config = "{\"webhook_url\":\"https://hooks.slack.com/services/T00/B00/xxx\"}";
        CreateAlertChannelRequest request = new CreateAlertChannelRequest("Slack", "SLACK", config);

        AlertChannel entity = createChannel("Slack", AlertChannelType.SLACK);
        AlertChannelResponse response = createResponse(entity.getId(), "Slack", "SLACK");

        when(alertChannelRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(alertChannelMapper.toEntity(request)).thenReturn(entity);
        when(alertChannelRepository.save(any(AlertChannel.class))).thenReturn(entity);
        when(alertChannelMapper.toResponse(entity)).thenReturn(response);

        AlertChannelResponse result = alertChannelService.createChannel(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("Slack");
    }

    @Test
    void testCreateChannel_exceedsMaxChannels_throwsValidation() {
        when(alertChannelRepository.countByTeamId(TEAM_ID))
                .thenReturn((long) AppConstants.MAX_ALERT_CHANNELS);

        CreateAlertChannelRequest request = new CreateAlertChannelRequest(
                "Too Many", "EMAIL", "{\"recipients\":[\"a@b.com\"]}");

        assertThatThrownBy(() -> alertChannelService.createChannel(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum channel limit");
    }

    @Test
    void testCreateChannel_invalidType_throwsValidation() {
        when(alertChannelRepository.countByTeamId(TEAM_ID)).thenReturn(0L);

        CreateAlertChannelRequest request = new CreateAlertChannelRequest(
                "Bad Type", "PIGEON_CARRIER", "{\"recipients\":[\"a@b.com\"]}");

        assertThatThrownBy(() -> alertChannelService.createChannel(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid channel type");
    }

    @Test
    void testCreateChannel_emailMissingRecipients_throwsValidation() {
        when(alertChannelRepository.countByTeamId(TEAM_ID)).thenReturn(0L);

        CreateAlertChannelRequest request = new CreateAlertChannelRequest(
                "Bad Email", "EMAIL", "{\"subject_prefix\":\"[Test]\"}");

        assertThatThrownBy(() -> alertChannelService.createChannel(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("recipients");
    }

    @Test
    void testCreateChannel_webhookHttpNotHttps_throwsValidation() {
        when(alertChannelRepository.countByTeamId(TEAM_ID)).thenReturn(0L);

        CreateAlertChannelRequest request = new CreateAlertChannelRequest(
                "Bad Webhook", "WEBHOOK", "{\"url\":\"http://insecure.example.com/hook\"}");

        assertThatThrownBy(() -> alertChannelService.createChannel(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void testGetChannelsByTeam_returnsList() {
        AlertChannel ch1 = createChannel("Ch1", AlertChannelType.EMAIL);
        AlertChannel ch2 = createChannel("Ch2", AlertChannelType.SLACK);

        when(alertChannelRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(ch1, ch2));
        when(alertChannelMapper.toResponseList(any())).thenReturn(List.of(
                mock(AlertChannelResponse.class), mock(AlertChannelResponse.class)));

        List<AlertChannelResponse> result = alertChannelService.getChannelsByTeam(TEAM_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void testUpdateChannel_updatesFields() {
        UUID channelId = UUID.randomUUID();
        AlertChannel existing = createChannel("Old", AlertChannelType.EMAIL);
        existing.setId(channelId);
        existing.setConfiguration("{\"recipients\":[\"a@b.com\"]}");

        UpdateAlertChannelRequest request = new UpdateAlertChannelRequest(
                "New Name", null, false);

        AlertChannelResponse response = createResponse(channelId, "New Name", "EMAIL");

        when(alertChannelRepository.findById(channelId)).thenReturn(Optional.of(existing));
        when(alertChannelRepository.save(any(AlertChannel.class))).thenReturn(existing);
        when(alertChannelMapper.toResponse(existing)).thenReturn(response);

        AlertChannelResponse result = alertChannelService.updateChannel(channelId, request);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getIsActive()).isFalse();
    }

    @Test
    void testDeleteChannel_succeeds() {
        UUID channelId = UUID.randomUUID();
        AlertChannel channel = createChannel("Delete Me", AlertChannelType.WEBHOOK);
        channel.setId(channelId);

        when(alertChannelRepository.findById(channelId)).thenReturn(Optional.of(channel));

        alertChannelService.deleteChannel(channelId);

        verify(alertChannelRepository).delete(channel);
    }

    @Test
    void testValidateWebhookUrl_rejectsInternalAddresses() {
        assertThatThrownBy(() -> alertChannelService.validateWebhookUrl("https://127.0.0.1/hook"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("internal");

        assertThatThrownBy(() -> alertChannelService.validateWebhookUrl("https://localhost/hook"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("internal");

        assertThatThrownBy(() -> alertChannelService.validateWebhookUrl("https://10.0.0.1/hook"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("internal");

        assertThatThrownBy(() -> alertChannelService.validateWebhookUrl("https://192.168.1.1/hook"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("internal");

        assertThatThrownBy(() -> alertChannelService.validateWebhookUrl("http://example.com/hook"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("HTTPS");
    }
}
