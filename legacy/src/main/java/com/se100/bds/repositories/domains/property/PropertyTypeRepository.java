package com.se100.bds.repositories.domains.property;

import com.se100.bds.models.entities.property.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyTypeRepository extends JpaRepository<PropertyType, UUID>, JpaSpecificationExecutor<PropertyType> {

    @Query("""
        SELECT p.id
        FROM PropertyType p
""")
    List<UUID> getAllIds();

    @Query("""
        SELECT p.typeName
        FROM PropertyType p
        WHERE p.id = :id
    """)
    String getPropertyTypeNameById(@Param("id") UUID id);
}

