package com.humanitarian.logistics.core.controller;

import com.humanitarian.logistics.core.dto.DisasterEventDTO;
import com.humanitarian.logistics.core.service.DisasterEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Disaster Events.
 * The @RestController annotation combines @Controller and @ResponseBody.
 * The @RequestMapping sets the base path for all endpoints in this controller.
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class DisasterEventController {

    private final DisasterEventService service;

    /**
     * GET endpoint to retrieve all disaster events.
     * @return ResponseEntity containing a list of DisasterEventDTOs and HTTP 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<DisasterEventDTO>> getAllEvents() {
        List<DisasterEventDTO> events = service.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * GET endpoint to retrieve a specific disaster event by ID.
     * @param id The UUID of the event extracted from the URL path.
     * @return ResponseEntity containing the DisasterEventDTO and HTTP 200 OK.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DisasterEventDTO> getEventById(@PathVariable UUID id) {
        DisasterEventDTO event = service.getEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * POST endpoint to create a new disaster event.
     * The @RequestBody annotation maps the incoming JSON payload to the DisasterEventDTO.
     * @param dto The data transfer object containing event details.
     * @return ResponseEntity containing the created DisasterEventDTO and HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<DisasterEventDTO> createEvent(@RequestBody DisasterEventDTO dto) {
        DisasterEventDTO createdEvent = service.createEvent(dto);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }
}
