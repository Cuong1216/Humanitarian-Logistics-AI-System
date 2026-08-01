package com.humanitarian.logistics.core.repository;

import com.humanitarian.logistics.core.entity.DisasterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for DisasterEvent.
 * Extends JpaRepository to provide basic CRUD operations out of the box.
 */
@Repository
public interface DisasterEventRepository extends JpaRepository<DisasterEvent, UUID> {
    // Custom query methods can be defined here if needed, e.g.:
    // List<DisasterEvent> findBySeverity(String severity);
}
