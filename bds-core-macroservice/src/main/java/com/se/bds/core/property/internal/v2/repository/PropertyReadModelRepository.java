package com.se.bds.core.property.internal.v2.repository;

import com.se.bds.core.property.internal.v2.domain.PropertyReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for reading property views.
 */
@Repository
public interface PropertyReadModelRepository extends JpaRepository<PropertyReadModel, UUID> {
}
