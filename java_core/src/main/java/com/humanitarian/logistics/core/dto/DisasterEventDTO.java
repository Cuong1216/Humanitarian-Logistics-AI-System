package com.humanitarian.logistics.core.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for DisasterEvent.
 * Used for transferring data between Controller and Client to hide entity implementation details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisasterEventDTO {
    
    private UUID id;
    private String name;
    private String type;
    private String location;
    private String severity;
    private LocalDateTime reportedAt;
    private String description;
}
