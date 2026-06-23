package com.se.bds.core.property.internal.v2.repository;

import com.se.bds.core.property.internal.v2.domain.PropertyEventEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Event Store entries.
 */
@Repository
public interface PropertyEventStoreRepository extends JpaRepository<PropertyEventEntry, UUID> {
    
    /**
     * Find all events of a specific property, ordered by their creation time ascending.
     */
    List<PropertyEventEntry> findByPropertyIdOrderByCreatedAtAsc(UUID propertyId);
}
