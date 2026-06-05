package com.se361.iam_service.repository;

import com.se361.iam_service.entity.PropertyOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyOwnerRepository extends JpaRepository<PropertyOwner, UUID> {
}
