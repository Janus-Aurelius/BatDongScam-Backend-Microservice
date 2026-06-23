package microservices.appointmentservice.services.ranking;

import microservices.appointmentservice.schemas.ranking.*;
import microservices.appointmentservice.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface RankingService {

    ///  Internal
    String getTier(UUID userId, Constants.RoleEnum role, int month, int year);
    String getCurrentTier(UUID userId, Constants.RoleEnum role);
    IndividualSalesAgentPerformanceMonth getSaleAgentCurrentMonth(UUID agentId);
    IndividualCustomerPotentialMonth getCustomerCurrentMonth(UUID customerId);
    IndividualPropertyOwnerContributionMonth getPropertyOwnerCurrentMonth(UUID propertyOwnerId);

    ///  Rest endpoint
    IndividualSalesAgentPerformanceMonth getSaleAgentMonth(UUID agentId, int month, int year);
    IndividualSalesAgentPerformanceCareer getSaleAgentCareer(UUID agentId);
    IndividualCustomerPotentialMonth getCustomerMonth(UUID customerId, int month, int year);
    IndividualCustomerPotentialAll getCustomerAll(UUID customerId);
    IndividualPropertyOwnerContributionMonth getPropertyOwnerMonth(UUID propertyOwnerId, int month, int year);
    IndividualPropertyOwnerContributionAll getPropertyOwnerAll(UUID propertyOwnerId);

    java.util.Map<UUID, IndividualSalesAgentPerformanceMonth> getSaleAgentsMonthBatch(java.util.List<UUID> agentIds, int month, int year);
    java.util.Map<UUID, IndividualSalesAgentPerformanceCareer> getSaleAgentsCareerBatch(java.util.List<UUID> agentIds);
    java.util.Map<UUID, IndividualCustomerPotentialMonth> getCustomersMonthBatch(java.util.List<UUID> customerIds, int month, int year);
    java.util.Map<UUID, IndividualCustomerPotentialAll> getCustomersAllBatch(java.util.List<UUID> customerIds);
    java.util.Map<UUID, IndividualPropertyOwnerContributionMonth> getPropertyOwnersMonthBatch(java.util.List<UUID> ownerIds, int month, int year);
    java.util.Map<UUID, IndividualPropertyOwnerContributionAll> getPropertyOwnersAllBatch(java.util.List<UUID> ownerIds);

    IndividualSalesAgentPerformanceMonth getMySaleAgentMonth(int month, int year);
    IndividualSalesAgentPerformanceCareer getMySaleAgentCareer();
    IndividualCustomerPotentialMonth getMyCustomerMonth(int month, int year);
    IndividualCustomerPotentialAll getMyCustomerAll();
    IndividualPropertyOwnerContributionMonth getMyPropertyOwnerMonth(int month, int year);
    IndividualPropertyOwnerContributionAll getMyPropertyOwnerAll();

    /// Action methods
    void agentAction(UUID agentId, Constants.AgentActionEnum actionType, BigDecimal amount);
    void customerAction(UUID customerId, Constants.CustomerActionEnum actionType, BigDecimal amount);
    void propertyOwnerAction(UUID ownerId, Constants.PropertyOwnerActionEnum actionType, BigDecimal amount);
}
