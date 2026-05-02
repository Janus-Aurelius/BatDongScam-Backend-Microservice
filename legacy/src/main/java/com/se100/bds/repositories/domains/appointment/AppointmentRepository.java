package com.se100.bds.repositories.domains.appointment;

import com.se100.bds.models.entities.appointment.Appointment;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {
    Page<Appointment> findAllByStatus(Constants.AppointmentStatusEnum status, Pageable pageable);

    List<Appointment> findAllByPropertyAndCustomer(Property property, Customer customer);

    @EntityGraph(attributePaths = {"property", "property.ward", "property.ward.district", "property.ward.district.city", "property.mediaList"})
    List<Appointment> findAllByCustomer_Id(UUID customerId);

    @EntityGraph(attributePaths = {"property", "property.ward", "property.ward.district", "property.ward.district.city", "property.mediaList"})
    List<Appointment> findAllByStatusAndCustomer_Id(Constants.AppointmentStatusEnum status, UUID customerId);

    @EntityGraph(attributePaths = {"property", "property.ward", "property.ward.district", "property.ward.district.city", "property.mediaList", "property.documents", "property.propertyType", "agent", "agent.user", "customer", "customer.user", "property.owner", "property.owner.user"})
    Optional<Appointment> findById(UUID id);

    @EntityGraph(attributePaths = {"property", "property.ward", "property.ward.district", "property.ward.district.city", "property.mediaList", "property.propertyType", "agent", "agent.user", "customer", "customer.user", "property.owner", "property.owner.user"})
    @Query("""
        SELECT a
        FROM Appointment a
        JOIN a.property p
        JOIN p.ward w
        JOIN w.district d
        JOIN d.city c
        JOIN p.propertyType pt
        LEFT JOIN a.agent ag
        JOIN a.customer cu
        WHERE
            (COALESCE(:propertyName, '') = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :propertyName, '%')))
            AND (COALESCE(:propertyTypeIds, NULL) IS NULL OR pt.id IN :propertyTypeIds)
            AND (COALESCE(:transactionTypeEnums, NULL) IS NULL OR p.transactionType IN :transactionTypeEnums)
            AND (COALESCE(:agentIds, NULL) IS NULL OR ag.id IN :agentIds)
            AND (COALESCE(:customerIds, NULL) IS NULL OR cu.id IN :customerIds)
            AND (:minRating IS NULL OR a.rating >= :minRating)
            AND (:maxRating IS NULL OR a.rating <= :maxRating)
            AND (COALESCE(:cityIds, NULL) IS NULL OR c.id IN :cityIds)
            AND (COALESCE(:districtIds, NULL) IS NULL OR d.id IN :districtIds)
            AND (COALESCE(:wardIds, NULL) IS NULL OR w.id IN :wardIds)
            AND (COALESCE(:statusEnums, NULL) IS NULL OR a.status IN :statusEnums)
        """)
    List<Appointment> findAllWithFilter(
            @Param("propertyName") String propertyName,
            @Param("propertyTypeIds") List<UUID> propertyTypeIds,
            @Param("transactionTypeEnums") List<Constants.TransactionTypeEnum> transactionTypeEnums,
            @Param("agentIds") List<UUID> agentIds,
            @Param("customerIds") List<UUID> customerIds,
            @Param("minRating") Short minRating,
            @Param("maxRating") Short maxRating,
            @Param("cityIds") List<UUID> cityIds,
            @Param("districtIds") List<UUID> districtIds,
            @Param("wardIds") List<UUID> wardIds,
            @Param("statusEnums") List<Constants.AppointmentStatusEnum> statusEnums
    );

    Long countByAgent_Id(UUID agentId);

    @EntityGraph(attributePaths = {"property", "property.ward", "property.ward.district", "property.ward.district.city", "property.mediaList", "property.propertyType", "agent", "agent.user", "customer", "customer.user", "property.owner", "property.owner.user"})
    List<Appointment> findAllByCustomer_IdInAndStatusInAndAgent_Id(Collection<UUID> customerIds, Collection<Constants.AppointmentStatusEnum> statuses, UUID agentId);

    @EntityGraph(attributePaths = {"property", "property.ward", "property.ward.district", "property.ward.district.city", "property.mediaList", "property.propertyType", "agent", "agent.user", "customer", "customer.user", "property.owner", "property.owner.user"})
    List<Appointment> findAllByCustomer_IdInAndAgent_Id(Collection<UUID> customerIds, UUID agentId);

    List<Appointment> findAllByAgent_Id(UUID agentId);
}
