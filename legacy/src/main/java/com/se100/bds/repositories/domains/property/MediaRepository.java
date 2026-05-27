package com.se100.bds.repositories.domains.property;

import com.se100.bds.models.entities.property.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID>, JpaSpecificationExecutor<Media> {
}


