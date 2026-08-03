package com.humanitarian.logistics.core.service;

import com.humanitarian.logistics.core.dto.DisasterEventDTO;
import com.humanitarian.logistics.core.entity.DisasterEvent;
import com.humanitarian.logistics.core.exception.ResourceNotFoundException;
import com.humanitarian.logistics.core.mapper.DisasterEventMapper;
import com.humanitarian.logistics.core.repository.DisasterEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for handling DisasterEvent business logic.
 * The @Transactional annotation ensures that the database operations are executed within a transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisasterEventService {

    private final DisasterEventRepository repository;
    private final DisasterEventMapper mapper;

    /**
     * Retrieves all disaster events.
     * @return List of DisasterEventDTOs
     */
    @Transactional(readOnly = true)
    public List<DisasterEventDTO> getAllEvents() {
        log.info("Fetching all disaster events");
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific disaster event by ID.
     * @param id The UUID of the event
     * @return DisasterEventDTO
     */
    @Transactional(readOnly = true)
    public DisasterEventDTO getEventById(UUID id) {
        log.info("Fetching disaster event with id: {}", id);
        DisasterEvent event = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisasterEvent not found with id: " + id));
        return mapper.toDTO(event);
    }

    /**
     * Creates a new disaster event.
     * @param dto The data transfer object containing event details
     * @return The created DisasterEventDTO
     */
    @Transactional
    public DisasterEventDTO createEvent(DisasterEventDTO dto) {
        log.info("Creating new disaster event: {}", dto.getName());
        DisasterEvent event = mapper.toEntity(dto);
        // Set reported time if not provided
        if (event.getReportedAt() == null) {
            event.setReportedAt(LocalDateTime.now());
        }
        DisasterEvent savedEvent = repository.save(event);
        return mapper.toDTO(savedEvent);
    }
}
