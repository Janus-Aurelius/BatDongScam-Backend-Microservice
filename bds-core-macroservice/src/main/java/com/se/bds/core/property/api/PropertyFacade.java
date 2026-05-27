package com.se.bds.core.property.api;

import com.se.bds.core.shared.dto.PropertySnapshot;
import com.se.bds.core.shared.dto.property.PropertyCard;
import com.se.bds.core.shared.ids.PropertyId;
import org.springframework.data.domain.Page;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

//the only way the transaction module is
// allowed to interact with properties
//return read-only dtos/snapshot from the shared kernel
public interface PropertyFacade {
    // transaction module
    //fetch a read only snapshot of property data needed for contract creation
    PropertySnapshot getPropertySnapshot(PropertyId propertyId);

    //throw a busniess exception if the property is not available for this type of contract
    void validatePropertyAvailableForContract(PropertyId propertyId,String contractType);

    //user module
    //replace propertyservice.getallbyuseridandstatus
    List<PropertySnapshot> getPropertySnapshotsByOwnerId(UUID ownerId, List<String> statuses);

    //replace propertyservice.getallbyuseridandstatus
    List<PropertySnapshot> getPropertySnapshotsByUserIdAndStatus(UUID customerId, List<String> statuses);

    //replace propertyservice.countbyassignedagentid
    int countByAssignedAgentId(UUID agentId);

    //customer module (favorites)
    /**
     * Replaces propertyService.getFavoritePropertyCards().
     */

    Page<PropertyCard> getFavoritePropertyCard(List<UUID> propertyIds, Pageable pageable);

    // report module (ref data)
    String getPropertyTypeName(UUID propertyTypeId);
    int countPropertiesByTypeId(UUID propertyTypeId);
    List<UUID> getAllAvailablePropertyTypeIds();

    // state mutation via facade (to replace direc repo access)
    //called by transaction module via event listener when service fee is paid
    void recordServiceFeePayment(PropertyId propertyId, BigDecimal amount);

}
