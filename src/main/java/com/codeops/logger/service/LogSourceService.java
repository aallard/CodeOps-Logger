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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing log source lifecycle (CRUD) operations.
 * Log sources represent registered services or applications that send logs to Logger.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogSourceService {

    private final LogSourceRepository logSourceRepository;
    private final LogSourceMapper logSourceMapper;

    /**
     * Registers a new log source for a team.
     *
     * @param request the source details
     * @param teamId  the team scope
     * @return the created source response
     * @throws ValidationException if a source with the same name already exists for the team
     */
    public LogSourceResponse createSource(CreateLogSourceRequest request, UUID teamId) {
        if (logSourceRepository.existsByTeamIdAndName(teamId, request.name())) {
            throw new ValidationException("Log source with name '" + request.name() + "' already exists");
        }

        LogSource entity = logSourceMapper.toEntity(request);
        entity.setTeamId(teamId);

        LogSource saved = logSourceRepository.save(entity);
        log.info("Created log source '{}' for team {}", saved.getName(), teamId);
        return logSourceMapper.toResponse(saved);
    }

    /**
     * Returns all log sources for a team.
     *
     * @param teamId the team scope
     * @return list of source responses
     */
    public List<LogSourceResponse> getSourcesByTeam(UUID teamId) {
        List<LogSource> sources = logSourceRepository.findByTeamId(teamId);
        return logSourceMapper.toResponseList(sources);
    }

    /**
     * Returns paginated log sources for a team.
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated source responses
     */
    public PageResponse<LogSourceResponse> getSourcesByTeamPaged(UUID teamId, int page, int size) {
        Page<LogSource> springPage = logSourceRepository.findByTeamId(teamId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<LogSourceResponse> content = logSourceMapper.toResponseList(springPage.getContent());
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
     * Returns a single log source by ID.
     *
     * @param sourceId the source ID
     * @return the source response
     * @throws NotFoundException if not found
     */
    public LogSourceResponse getSource(UUID sourceId) {
        LogSource source = logSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Log source not found: " + sourceId));
        return logSourceMapper.toResponse(source);
    }

    /**
     * Updates an existing log source.
     *
     * @param sourceId the source ID
     * @param request  the update data
     * @return the updated source response
     * @throws NotFoundException if not found
     */
    public LogSourceResponse updateSource(UUID sourceId, UpdateLogSourceRequest request) {
        LogSource source = logSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Log source not found: " + sourceId));

        if (request.name() != null) {
            source.setName(request.name());
        }
        if (request.description() != null) {
            source.setDescription(request.description());
        }
        if (request.environment() != null) {
            source.setEnvironment(request.environment());
        }
        if (request.isActive() != null) {
            source.setIsActive(request.isActive());
        }

        LogSource saved = logSourceRepository.save(source);
        return logSourceMapper.toResponse(saved);
    }

    /**
     * Deletes a log source.
     *
     * @param sourceId the source ID
     * @throws NotFoundException if not found
     */
    public void deleteSource(UUID sourceId) {
        LogSource source = logSourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Log source not found: " + sourceId));
        logSourceRepository.delete(source);
        log.info("Deleted log source '{}' ({})", source.getName(), sourceId);
    }
}
