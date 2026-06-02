package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.domain.model.City;
import com.se.bds.core.property.internal.domain.model.District;
import com.se.bds.core.property.internal.domain.model.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

interface JpaCityRepository extends JpaRepository<City, UUID> {
}

interface JpaDistrictRepository extends JpaRepository<District, UUID> {
    @Query("SELECT d FROM District d WHERE d.city.id = :cityId")
    List<District> findAllByCityId(@Param("cityId") UUID cityId);
}

interface JpaWardRepository extends JpaRepository<Ward, UUID> {
    @Query("SELECT w FROM Ward w WHERE w.district.id = :districtId")
    List<Ward> findAllByDistrictId(@Param("districtId") UUID districtId);
}
