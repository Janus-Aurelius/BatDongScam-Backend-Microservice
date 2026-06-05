package microservices.appointmentservice.repositories;

import microservices.appointmentservice.entities.appointment.Appointment;
import microservices.appointmentservice.entities.property.Property;
import microservices.appointmentservice.utils.Constants;
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

    List<Appointment> findAllByPropertyAndCustomerId(Property property, UUID customerId);

    @EntityGraph(attributePaths = {"property", "property.mediaList"})
    List<Appointment> findAllByCustomerId(UUID customerId);

    @EntityGraph(attributePaths = {"property", "property.mediaList"})
    List<Appointment> findAllByStatusAndCustomerId(Constants.AppointmentStatusEnum status, UUID customerId);

    @EntityGraph(attributePaths = {"property", "property.mediaList"})
    Optional<Appointment> findById(UUID id);

    @EntityGraph(attributePaths = {"property", "property.mediaList"})
    @Query("""
        SELECT a
        FROM Appointment a
        JOIN a.property p
        JOIN Ward w ON p.wardId = w.id
        JOIN District d ON w.district.id = d.id
        JOIN City c ON d.city.id = c.id
        JOIN p.propertyType pt
        WHERE
            (COALESCE(:propertyName, '') = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :propertyName, '%')))
            AND (COALESCE(:propertyTypeIds, NULL) IS NULL OR pt.id IN :propertyTypeIds)
            AND (COALESCE(:transactionTypeEnums, NULL) IS NULL OR p.transactionType IN :transactionTypeEnums)
            AND (COALESCE(:agentIds, NULL) IS NULL OR a.agentId IN :agentIds)
            AND (COALESCE(:customerIds, NULL) IS NULL OR a.customerId IN :customerIds)
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

    Long countByAgentId(UUID agentId);

    @EntityGraph(attributePaths = {"property", "property.mediaList"})
    List<Appointment> findAllByCustomerIdInAndStatusInAndAgentId(Collection<UUID> customerIds, Collection<Constants.AppointmentStatusEnum> statuses, UUID agentId);

    @EntityGraph(attributePaths = {"property", "property.mediaList"})
    List<Appointment> findAllByCustomerIdInAndAgentId(Collection<UUID> customerIds, UUID agentId);

    List<Appointment> findAllByAgentId(UUID agentId);
}