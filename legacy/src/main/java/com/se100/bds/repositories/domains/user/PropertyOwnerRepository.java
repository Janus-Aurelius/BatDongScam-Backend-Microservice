package com.se100.bds.repositories.domains.user;

import com.se100.bds.models.entities.user.PropertyOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyOwnerRepository extends JpaRepository<PropertyOwner, UUID>, JpaSpecificationExecutor<PropertyOwner> {
}
