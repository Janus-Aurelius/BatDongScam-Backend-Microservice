package com.se100.bds.repositories.domains.contract;

import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.utils.Constants.PaymentStatusEnum;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
	Optional<Payment> findByPaywayPaymentId(String paywayPaymentId);
	
  @EntityGraph(attributePaths = {"contract", "property"})
	Page<Payment> findAllByProperty_Id(UUID propertyId, Pageable pageable);

	@Query("SELECT p FROM Payment p " +
	       "LEFT JOIN FETCH p.property prop " +
	       "LEFT JOIN FETCH prop.ward w " +
	       "LEFT JOIN FETCH w.district d " +
	       "LEFT JOIN FETCH d.city " +
	       "LEFT JOIN FETCH prop.propertyType " +
	       "WHERE YEAR(p.paidTime) = :year AND MONTH(p.paidTime) = :month " +
	       "AND p.status = :successStatus " +
	       "AND p.paymentType NOT IN :excludedTypes")
	List<Payment> findRevenuePaymentsInMonth(
		@Param("month") int month,
		@Param("year") int year,
		@Param("successStatus") PaymentStatusEnum successStatus,
		@Param("excludedTypes") List<PaymentTypeEnum> excludedTypes
	);
}
