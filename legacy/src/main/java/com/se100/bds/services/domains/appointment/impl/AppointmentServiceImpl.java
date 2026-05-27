package com.se100.bds.services.domains.appointment.impl;

import com.se100.bds.dtos.requests.appointment.BookAppointmentRequest;
import com.se100.bds.dtos.responses.appointment.BookAppointmentResponse;
import com.se100.bds.dtos.responses.appointment.ViewingCardDto;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsCustomer;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsAdmin;
import com.se100.bds.dtos.responses.appointment.ViewingListItem;
import com.se100.bds.dtos.responses.user.simple.PropertyOwnerSimpleCard;
import com.se100.bds.dtos.responses.user.simple.SalesAgentSimpleCard;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.mappers.AppointmentMapper;
import com.se100.bds.models.entities.AbstractBaseEntity;
import com.se100.bds.models.entities.appointment.Appointment;
import com.se100.bds.models.entities.document.IdentificationDocument;
import com.se100.bds.models.entities.property.Media;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.models.entities.user.Customer;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceCareer;
import com.se100.bds.models.schemas.ranking.IndividualSalesAgentPerformanceMonth;
import com.se100.bds.repositories.domains.appointment.AppointmentRepository;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.repositories.domains.user.CustomerRepository;
import com.se100.bds.repositories.domains.user.SaleAgentRepository;
import com.se100.bds.services.domains.appointment.AppointmentService;
import com.se100.bds.services.domains.notification.NotificationService;
import com.se100.bds.services.domains.ranking.RankingService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final SaleAgentRepository saleAgentRepository;
    private final UserService userService;
    private final RankingService rankingService;
    private final AppointmentMapper appointmentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public BookAppointmentResponse bookAppointment(BookAppointmentRequest request) {
        User currentUser = userService.getUser();
        if (currentUser == null) {
            throw new NotFoundException("Current user not found");
        }

        Constants.RoleEnum currentRole = currentUser.getRole();
        boolean isCustomer = currentRole == Constants.RoleEnum.CUSTOMER;
        boolean isAdmin = currentRole == Constants.RoleEnum.ADMIN;
        boolean isAgent = currentRole == Constants.RoleEnum.SALESAGENT;

        if ((isAdmin || isAgent) && request.getCustomerId() == null) {
            throw new IllegalArgumentException("customerId is required when booking on behalf of a customer");
        }

        if (isCustomer && request.getCustomerId() != null && !request.getCustomerId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Customers cannot book appointments for other users");
        }

        if (request.getRequestedDate() == null || !request.getRequestedDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Requested date must be later than the current time");
        }

        // Determine the customer: use provided customerId (for admin/agent) or current user
        UUID targetCustomerId = request.getCustomerId() != null ? request.getCustomerId() : currentUser.getId();

        // Get customer entity
        Customer customer = customerRepository.findById(targetCustomerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + targetCustomerId));

        // Get property
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new NotFoundException("Property not found: " + request.getPropertyId()));

        // Check if property is available for viewing
        if (property.getStatus() == null || 
            property.getStatus() == Constants.PropertyStatusEnum.PENDING ||
            property.getStatus() == Constants.PropertyStatusEnum.REJECTED ||
            property.getStatus() == Constants.PropertyStatusEnum.SOLD ||
            property.getStatus() == Constants.PropertyStatusEnum.RENTED ||
            property.getStatus() == Constants.PropertyStatusEnum.REMOVED ||
            property.getStatus() == Constants.PropertyStatusEnum.DELETED) {
            throw new IllegalStateException("Property is not available for viewing");
        }

        // Check if customer already has a pending appointment for this property
        List<Appointment> existingAppointments = appointmentRepository.findAllByPropertyAndCustomer(property, customer);
        boolean hasPendingAppointment = existingAppointments.stream()
                .anyMatch(a -> a.getStatus() == Constants.AppointmentStatusEnum.PENDING ||
                               a.getStatus() == Constants.AppointmentStatusEnum.CONFIRMED);
        if (hasPendingAppointment) {
            throw new IllegalStateException("You already have a pending or confirmed appointment for this property");
        }

        // Optional: assign agent if provided
        SaleAgent assignedAgent = null;
        if (request.getAgentId() != null) {
            assignedAgent = saleAgentRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new NotFoundException("Sales agent not found: " + request.getAgentId()));
        }

        // Create the appointment
        Appointment.AppointmentBuilder appointmentBuilder = Appointment.builder()
            .property(property)
            .customer(customer)
            .requestedDate(request.getRequestedDate())
            .customerRequirements(request.getCustomerRequirements())
            .status(assignedAgent != null ? Constants.AppointmentStatusEnum.CONFIRMED : Constants.AppointmentStatusEnum.PENDING);
        
        if (assignedAgent != null) {
            appointmentBuilder.agent(assignedAgent)
                .confirmedDate(LocalDateTime.now());
        }
        
        Appointment appointment = appointmentBuilder.build();
        Appointment saved = appointmentRepository.save(appointment);
        
        log.info("Created appointment {} for customer {} on property {}, agent: {}", 
                saved.getId(), customer.getId(), property.getId(), 
                assignedAgent != null ? assignedAgent.getId() : "unassigned");

        // Track customer action for ranking
        rankingService.customerAction(customer.getId(), Constants.CustomerActionEnum.VIEWING_REQUESTED, null);

        // Send notifications
        String propertyTitle = property.getTitle();
        String customerName = customer.getUser().getFirstName() + " " + customer.getUser().getLastName();
        String imgUrl = property.getMediaList().isEmpty() ? null : property.getMediaList().get(0).getFilePath();

        // Notify property owner
        notificationService.createNotification(
            property.getOwner().getUser(),
            Constants.NotificationTypeEnum.APPOINTMENT_BOOKED,
            "New Viewing Appointment",
            String.format("Customer %s has booked a viewing for your property: %s", customerName, propertyTitle),
            Constants.RelatedEntityTypeEnum.APPOINTMENT,
            saved.getId().toString(),
            imgUrl
        );

        // Notify assigned agent if exists
        if (assignedAgent != null) {
            notificationService.createNotification(
                assignedAgent.getUser(),
                Constants.NotificationTypeEnum.APPOINTMENT_BOOKED,
                "New Viewing Assignment",
                String.format("You have been assigned to a viewing appointment for property: %s", propertyTitle),
                Constants.RelatedEntityTypeEnum.APPOINTMENT,
                saved.getId().toString(),
                imgUrl
            );
        }

        String confirmationMessage = assignedAgent != null
            ? "Appointment created and assigned to agent " + assignedAgent.getUser().getFirstName() + " " + assignedAgent.getUser().getLastName()
            : "Your viewing appointment has been booked. You will be notified when an admin confirms the appointment.";

        return appointmentMapper.buildBookingResponse(saved, confirmationMessage);
    }

    @Override
    @Transactional
    public boolean cancelAppointment(UUID appointmentId, String reason) {
        User currentUser = userService.getUser();
        if (currentUser == null) {
            throw new NotFoundException("Current user not found");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        // Verify ownership - only the customer who booked, assigned agent, or admin can cancel
        boolean isCustomer = appointment.getCustomer() != null && 
                             appointment.getCustomer().getId().equals(currentUser.getId());
        boolean isAgent = appointment.getAgent() != null && 
                          appointment.getAgent().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Constants.RoleEnum.ADMIN;

        if (!isCustomer && !isAgent && !isAdmin) {
            throw new IllegalStateException("You are not authorized to cancel this appointment");
        }

        // Check if already cancelled or completed
        if (appointment.getStatus() == Constants.AppointmentStatusEnum.CANCELLED) {
            throw new IllegalStateException("Appointment is already cancelled");
        }
        if (appointment.getStatus() == Constants.AppointmentStatusEnum.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed appointment");
        }

        // Update appointment
        appointment.setStatus(Constants.AppointmentStatusEnum.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancelledBy(currentUser.getRole());
        appointment.setCancelledReason(reason);

        appointmentRepository.save(appointment);
        log.info("Cancelled appointment {} by {} (role: {})", 
                appointmentId, currentUser.getId(), currentUser.getRole());

        // Track cancellation for ranking penalties (admin-triggered cancellations intentionally bypass ranking changes)
        if (isAgent && appointment.getAgent() != null) {
            rankingService.agentAction(appointment.getAgent().getId(), Constants.AgentActionEnum.APPOINTMENT_CANCELLED, null);
        }
        if (isCustomer && appointment.getCustomer() != null) {
            rankingService.customerAction(appointment.getCustomer().getId(), Constants.CustomerActionEnum.VIEWING_CANCELLED, null);
        }

        // Send cancellation notifications
        String propertyTitle = appointment.getProperty().getTitle();
        String reasonText = reason != null ? " Reason: " + reason : "";
        String imgUrl = appointment.getProperty().getMediaList().isEmpty() ? null :
                        appointment.getProperty().getMediaList().get(0).getFilePath();

        // Notify customer
        notificationService.createNotification(
            appointment.getCustomer().getUser(),
            Constants.NotificationTypeEnum.APPOINTMENT_CANCELLED,
            "Appointment Cancelled",
            String.format("Your viewing appointment for %s has been cancelled.%s", propertyTitle, reasonText),
            Constants.RelatedEntityTypeEnum.APPOINTMENT,
            appointment.getId().toString(),
            imgUrl
        );

        // Notify property owner
        notificationService.createNotification(
            appointment.getProperty().getOwner().getUser(),
            Constants.NotificationTypeEnum.APPOINTMENT_CANCELLED,
            "Appointment Cancelled",
            String.format("The viewing appointment for your property %s has been cancelled.%s", propertyTitle, reasonText),
            Constants.RelatedEntityTypeEnum.APPOINTMENT,
            appointment.getId().toString(),
            imgUrl
        );

        // Notify agent if assigned
        if (appointment.getAgent() != null) {
            notificationService.createNotification(
                appointment.getAgent().getUser(),
                Constants.NotificationTypeEnum.APPOINTMENT_CANCELLED,
                "Appointment Cancelled",
                String.format("The viewing appointment for property %s has been cancelled.%s", propertyTitle, reasonText),
                Constants.RelatedEntityTypeEnum.APPOINTMENT,
                appointment.getId().toString(),
                imgUrl
            );
        }

        return true;
    }

    @Override
    @Transactional
    public boolean completeAppointment(UUID appointmentId) {
        User currentUser = userService.getUser();
        if (currentUser == null) {
            throw new NotFoundException("Current user not found");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        boolean isCustomer = appointment.getCustomer() != null &&
                appointment.getCustomer().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Constants.RoleEnum.ADMIN;
        boolean isAgent = appointment.getAgent() != null &&
                appointment.getAgent().getId().equals(currentUser.getId());

        if (!isCustomer && !isAdmin && !isAgent) {
            throw new IllegalStateException("You are not authorized to complete this appointment");
        }

        if (appointment.getStatus() == Constants.AppointmentStatusEnum.CANCELLED) {
            throw new IllegalStateException("Cannot complete a cancelled appointment");
        }

        if (appointment.getStatus() == Constants.AppointmentStatusEnum.COMPLETED) {
            return false;
        }

        if (appointment.getRequestedDate() != null && appointment.getRequestedDate().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Appointment time has not occurred yet");
        }

        appointment.setStatus(Constants.AppointmentStatusEnum.COMPLETED);
        appointmentRepository.save(appointment);

        if (appointment.getAgent() != null) {
            rankingService.agentAction(appointment.getAgent().getId(), Constants.AgentActionEnum.APPOINTMENT_COMPLETED, null);
        }
        if (appointment.getCustomer() != null) {
            rankingService.customerAction(appointment.getCustomer().getId(), Constants.CustomerActionEnum.VIEWING_ATTENDED, null);
        }

        // Send completion notifications
        String propertyTitle = appointment.getProperty().getTitle();
        String imgUrl = appointment.getProperty().getMediaList().isEmpty() ? null :
                        appointment.getProperty().getMediaList().get(0).getFilePath();

        // Notify customer
        assert appointment.getCustomer() != null;
        notificationService.createNotification(
            appointment.getCustomer().getUser(),
            Constants.NotificationTypeEnum.APPOINTMENT_COMPLETED,
            "Appointment Completed",
            String.format("Your viewing appointment for %s has been completed. Please rate your experience!", propertyTitle),
            Constants.RelatedEntityTypeEnum.APPOINTMENT,
            appointment.getId().toString(),
            imgUrl
        );

        // Notify property owner
        notificationService.createNotification(
            appointment.getProperty().getOwner().getUser(),
            Constants.NotificationTypeEnum.APPOINTMENT_COMPLETED,
            "Appointment Completed",
            String.format("The viewing appointment for your property %s has been completed.", propertyTitle),
            Constants.RelatedEntityTypeEnum.APPOINTMENT,
            appointment.getId().toString(),
            imgUrl
        );

        // Notify agent if assigned
        if (appointment.getAgent() != null) {
            notificationService.createNotification(
                appointment.getAgent().getUser(),
                Constants.NotificationTypeEnum.APPOINTMENT_COMPLETED,
                "Appointment Completed",
                String.format("The viewing appointment for property %s has been completed.", propertyTitle),
                Constants.RelatedEntityTypeEnum.APPOINTMENT,
                appointment.getId().toString(),
                imgUrl
            );
        }

        return true;
    }

    @Override
    public Page<ViewingCardDto> myViewingCards(Pageable pageable, Constants.AppointmentStatusEnum statusEnum, Integer day, Integer month, Integer year) {
        User me = userService.getUser();
        List<Appointment> appointments;
        if (statusEnum == null) {
            appointments = appointmentRepository.findAllByCustomer_Id(me.getId());
        } else {
            appointments = appointmentRepository.findAllByStatusAndCustomer_Id(statusEnum, me.getId());
        }

        List<ViewingCardDto> viewingCardDtos = appointments.stream().filter(appointment -> {
            LocalDateTime requestedTime = appointment.getRequestedDate();
            if (year != null && requestedTime.getYear() != year) {
                return false;
            }
            if (month != null && requestedTime.getMonthValue() != month) {
                return false;
            }
            if (day != null && requestedTime.getDayOfMonth() != day) {
                return false;
            }
            return true;
        })

        .map(appointment -> {

            String thumbnailUrl = appointment.getProperty().getMediaList().get(0).getFilePath();
            String districtName = appointment.getProperty().getWard().getDistrict().getDistrictName();
            String cityName = appointment.getProperty().getWard().getDistrict().getCity().getCityName();

            ViewingCardDto viewingCardDto = appointmentMapper.mapTo(appointment, ViewingCardDto.class);

            viewingCardDto.setTitle(appointment.getProperty().getTitle());
            viewingCardDto.setThumbnailUrl(thumbnailUrl);
            viewingCardDto.setDistrictName(districtName);
            viewingCardDto.setCityName(cityName);
            viewingCardDto.setPriceAmount(appointment.getProperty().getPriceAmount());
            viewingCardDto.setArea(appointment.getProperty().getArea());

            return viewingCardDto;
        }
        ).collect(Collectors.toList());

        return new PageImpl<>(viewingCardDtos, pageable, viewingCardDtos.size());
    }

    @Override
    public ViewingDetailsCustomer getViewingDetails(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id"));

        ViewingDetailsCustomer viewingDetailsCustomer = appointmentMapper.mapTo(appointment, ViewingDetailsCustomer.class);

        List<String> imagesList = appointment.getProperty().getMediaList().stream()
                .map(Media::getFilePath)
                .collect(Collectors.toList());
        String fullAddress = appointment.getProperty().getFullAddress();
        List<String> documentList = appointment.getProperty().getDocuments().stream()
                .map(IdentificationDocument::getFilePath)
                .collect(Collectors.toList());

        viewingDetailsCustomer.setImagesList(imagesList);
        viewingDetailsCustomer.setImages(imagesList.size());
        viewingDetailsCustomer.setAttachedDocuments(documentList);
        viewingDetailsCustomer.setFullAddress(fullAddress);

        String ownerTier = rankingService.getCurrentTier(
                appointment.getProperty().getOwner().getId(),
                Constants.RoleEnum.PROPERTY_OWNER
        );
        PropertyOwnerSimpleCard ownerCard = appointmentMapper.buildOwnerCard(
                appointment.getProperty().getOwner(),
                ownerTier
        );
        viewingDetailsCustomer.setPropertyOwner(ownerCard);

        if (appointment.getAgent() != null) {
            IndividualSalesAgentPerformanceMonth agentRanking = rankingService.getSaleAgentCurrentMonth(
                    appointment.getAgent().getId()
            );

            IndividualSalesAgentPerformanceCareer agentRankingCareer = rankingService.getSaleAgentCareer(
                    appointment.getAgent().getId()
            );

            SalesAgentSimpleCard agentCard = appointmentMapper.buildAgentCard(
                    appointment.getAgent(),
                    agentRanking.getPerformanceTier().getValue(),
                    agentRankingCareer.getAvgRating().doubleValue(),
                    agentRankingCareer.getTotalRates()
            );
            viewingDetailsCustomer.setSalesAgent(agentCard);
        }

        return viewingDetailsCustomer;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewingListItem> getViewingListItems(
            Pageable pageable,
            String propertyName, List<UUID> propertyTypeIds,
            List<Constants.TransactionTypeEnum> transactionTypeEnums,
            String agentName, List<Constants.PerformanceTierEnum> agentTiers,
            String customerName, List<Constants.CustomerTierEnum> customerTiers,
            LocalDateTime requestDateFrom, LocalDateTime requestDateTo,
            Short minRating, Short maxRating,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds,
            List<Constants.AppointmentStatusEnum> statusEnums) {

        List<UUID> agentWithMatchedTiers = null;
        if (agentName != null || (agentTiers != null && !agentTiers.isEmpty())) {
            List<User> agents = userService.findAllByNameAndRole(agentName, Constants.RoleEnum.SALESAGENT);
            agentWithMatchedTiers = new ArrayList<>();

            if (agentTiers != null && !agentTiers.isEmpty()) {
                // Filter by both name and tier
                for (User agent : agents) {
                    Constants.PerformanceTierEnum agentTier = rankingService.getSaleAgentCurrentMonth(agent.getId()).getPerformanceTier();
                    for (Constants.PerformanceTierEnum desiredTier: agentTiers) {
                        if (desiredTier.equals(agentTier)) {
                            agentWithMatchedTiers.add(agent.getId());
                            break;
                        }
                    }
                }
            } else {
                // Filter by name only
                agentWithMatchedTiers = agents.stream()
                        .map(AbstractBaseEntity::getId)
                        .collect(Collectors.toList());
            }
        }

        // Handle customer filtering
        // If no customer name or tier filters provided, pass null to include all appointments
        List<UUID> customerWithMatchedTiers = null;
        if (customerName != null || (customerTiers != null && !customerTiers.isEmpty())) {
            // Customer filters are specified
            List<User> customers = userService.findAllByNameAndRole(customerName, Constants.RoleEnum.CUSTOMER);
            customerWithMatchedTiers = new ArrayList<>();

            if (customerTiers != null && !customerTiers.isEmpty()) {
                // Filter by both name and tier
                for (User customer : customers) {
                    Constants.CustomerTierEnum customerTier = rankingService.getCustomerCurrentMonth(customer.getId()).getCustomerTier();
                    for (Constants.CustomerTierEnum desiredTier: customerTiers) {
                        if (desiredTier.equals(customerTier)) {
                            customerWithMatchedTiers.add(customer.getId());
                            break;
                        }
                    }
                }
            } else {
                // Filter by name only
                customerWithMatchedTiers = customers.stream()
                        .map(AbstractBaseEntity::getId)
                        .collect(Collectors.toList());
            }
        }

        List<Appointment> appointments = appointmentRepository.findAllWithFilter(
                propertyName, propertyTypeIds,
                transactionTypeEnums,
                agentWithMatchedTiers,
                customerWithMatchedTiers,
                minRating, maxRating,
                cityIds, districtIds, wardIds,
                statusEnums
        );

        List<Appointment> finalAppointments = new ArrayList<>();
        for  (Appointment appointment : appointments) {
            if (requestDateFrom != null && appointment.getRequestedDate().isBefore(requestDateFrom))
                continue;
            if (requestDateTo != null && appointment.getRequestedDate().isAfter(requestDateTo))
                continue;
            finalAppointments.add(appointment);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), finalAppointments.size());
        List<Appointment> pagedAppointments = finalAppointments.subList(start, end);

        // Map appointments to ViewingListItemDto with enriched data
        List<ViewingListItem> viewingListItems = pagedAppointments.stream()
                .map(appointment -> {
                    // Map basic fields using the mapper
                    ViewingListItem dto = appointmentMapper.mapToViewingListItem(appointment);

                    // Get customer tier
                    String customerTier = rankingService.getCurrentTier(
                            appointment.getCustomer().getId(),
                            Constants.RoleEnum.CUSTOMER
                    );

                    // Get sales agent tier (null if no agent assigned)
                    String agentTier = null;
                    if (appointment.getAgent() != null) {
                        agentTier = rankingService.getSaleAgentCurrentMonth(
                                appointment.getAgent().getId()
                        ).getPerformanceTier().getValue();
                    }

                    // Enrich with customer, agent, and thumbnail data
                    appointmentMapper.enrichViewingListItem(dto, appointment, customerTier, agentTier);

                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(viewingListItems, pageable, finalAppointments.size());
    }

    @Override
    public ViewingDetailsAdmin getViewingDetailsAdmin(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id"));

        ViewingDetailsAdmin viewingDetails = appointmentMapper.mapTo(appointment, ViewingDetailsAdmin.class);

        // Build property card
        ViewingDetailsAdmin.PropertyCard propertyCard = appointmentMapper.buildPropertyCard(appointment);
        viewingDetails.setPropertyCard(propertyCard);

        // Build customer card with tier
        String customerTier = rankingService.getCurrentTier(
                appointment.getCustomer().getId(),
                Constants.RoleEnum.CUSTOMER
        );
        ViewingDetailsAdmin.UserSimpleCard customerCard = appointmentMapper.buildUserSimpleCard(
                appointment.getCustomer().getUser(),
                customerTier
        );
        viewingDetails.setCustomer(customerCard);

        // Build property owner card with tier
        String ownerTier = rankingService.getCurrentTier(
                appointment.getProperty().getOwner().getId(),
                Constants.RoleEnum.PROPERTY_OWNER
        );
        ViewingDetailsAdmin.UserSimpleCard ownerCard = appointmentMapper.buildUserSimpleCard(
                appointment.getProperty().getOwner().getUser(),
                ownerTier
        );
        viewingDetails.setPropertyOwner(ownerCard);

        // Build sales agent card with tier and rating (only if agent is assigned)
        if (appointment.getAgent() != null) {
            IndividualSalesAgentPerformanceMonth agentRanking = rankingService.getSaleAgentCurrentMonth(
                    appointment.getAgent().getId()
            );
            IndividualSalesAgentPerformanceCareer agentRankingCareer = rankingService.getSaleAgentCareer(
                    appointment.getAgent().getId()
            );
            ViewingDetailsAdmin.SalesAgentSimpleCard agentCard = appointmentMapper.buildSalesAgentSimpleCard(
                    appointment.getAgent().getUser(),
                    agentRanking.getPerformanceTier().getValue(),
                    agentRankingCareer.getAvgRating().doubleValue(),
                    agentRankingCareer.getTotalRates()
            );
            viewingDetails.setSalesAgent(agentCard);
        }

        return viewingDetails;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewingListItem> getMyViewingListItems(
            Pageable pageable,
            String customerName,
            Integer day, Integer month, Integer year,
            List<Constants.AppointmentStatusEnum> statusEnums) {

        List<User> customers = userService.findAllByNameAndRole(customerName, Constants.RoleEnum.CUSTOMER);
        List<UUID> customerIds = new  ArrayList<>();
        if (customers != null && !customers.isEmpty())
            customerIds = customers.stream().map(User::getId).toList();


        List<Appointment> appointments;
        if (statusEnums != null && !statusEnums.isEmpty())
            appointments = appointmentRepository
                    .findAllByCustomer_IdInAndStatusInAndAgent_Id(customerIds, statusEnums, userService.getUserId());
        else
            appointments = appointmentRepository
                    .findAllByCustomer_IdInAndAgent_Id(customerIds, userService.getUserId());

        List<Appointment> finalAppointments = new ArrayList<>();

        for (Appointment appointment : appointments) {
            if (day != null && appointment.getRequestedDate().getDayOfMonth() != day)
                continue;
            if (month != null && appointment.getRequestedDate().getMonthValue() != month)
                continue;
            if (year != null && appointment.getRequestedDate().getYear() != year)
                continue;
            finalAppointments.add(appointment);
        }

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), finalAppointments.size());
        List<Appointment> pagedAppointments = finalAppointments.subList(start, end);

        // Map appointments to ViewingListItem with enriched data
        List<ViewingListItem> viewingListItems = pagedAppointments.stream()
                .map(appointment -> {
                    // Map basic fields using the manual mapper (avoiding Hibernate proxy issues)
                    ViewingListItem dto = appointmentMapper.mapToViewingListItem(appointment);

                    // Get customer tier
                    String customerTier = rankingService.getCurrentTier(
                            appointment.getCustomer().getId(),
                            Constants.RoleEnum.CUSTOMER
                    );

                    String agentTier = null;
                    if (appointment.getAgent() != null) {
                        agentTier = rankingService.getSaleAgentCurrentMonth(appointment.getAgent().getId())
                                                    .getPerformanceTier()
                                                    .getValue();
                    }

                    // Enrich with customer, agent, and thumbnail data
                    appointmentMapper.enrichViewingListItem(dto, appointment, customerTier, agentTier);

                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(viewingListItems, pageable, finalAppointments.size());
    }

    @Override
    public int countByAgentId(UUID agentId) {
        Long count = appointmentRepository.countByAgent_Id(agentId);
        return count != null ? count.intValue() : 0;
    }

    @Override
    public void removeAgent(UUID appointmentId) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + appointmentId));

        if (appointment.getAgent() != null) {
            appointment.setAgent(null);
            appointment.setStatus(Constants.AppointmentStatusEnum.PENDING);
            appointment.setConfirmedDate(null);
            appointmentRepository.save(appointment);
            log.info("Removed agent from appointment: {}", appointmentId);
        }
    }

    @Override
    @Transactional
    public void assignAgent(@NotNull UUID agentId, UUID appointmentId) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + appointmentId));

        // Find the new agent
        User agentUser;
        try {
            agentUser = userService.findById(agentId);
        } catch (EntityNotFoundException e) {
            throw new NotFoundException("Sale agent not found with id: " + agentId);
        }

        if (validateAssignAgent(agentId, appointmentId, agentUser, appointment)) return;

        // Remove old agent if exists and assign new agent
        if (appointment.getAgent() != null) {
            log.info("Replacing agent {} with {} for appointment: {}",
                    appointment.getAgent().getId(), agentId, appointmentId);
        }

        if (appointment.getStatus() == Constants.AppointmentStatusEnum.PENDING) {
            appointment.setStatus(Constants.AppointmentStatusEnum.CONFIRMED);
            appointment.setConfirmedDate(LocalDateTime.now());
        }

        appointment.setAgent(agentUser.getSaleAgent());
        appointmentRepository.save(appointment);
        log.info("Assigned agent {} to appointment: {}", agentId, appointmentId);

        // Send assignment notifications
        String propertyTitle = appointment.getProperty().getTitle();
        String customerName = appointment.getCustomer().getUser().getFirstName() + " " +
                              appointment.getCustomer().getUser().getLastName();
        String imgUrl = appointment.getProperty().getMediaList().isEmpty() ? null :
                        appointment.getProperty().getMediaList().get(0).getFilePath();

        // Notify agent
        notificationService.createNotification(
            agentUser,
            Constants.NotificationTypeEnum.APPOINTMENT_ASSIGNED,
            "New Viewing Assignment",
            String.format("You have been assigned to a viewing appointment with %s for property: %s", customerName, propertyTitle),
            Constants.RelatedEntityTypeEnum.APPOINTMENT,
            appointment.getId().toString(),
            imgUrl
        );

        // Notify customer
        String agentName = agentUser.getFirstName() + " " + agentUser.getLastName();
        notificationService.createNotification(
            appointment.getCustomer().getUser(),
            Constants.NotificationTypeEnum.APPOINTMENT_ASSIGNED,
            "Agent Assigned to Your Appointment",
            String.format("Agent %s has been assigned to your viewing appointment for: %s", agentName, propertyTitle),
            Constants.RelatedEntityTypeEnum.APPOINTMENT,
            appointment.getId().toString(),
            imgUrl
        );

        // Track appointment assignment action for agent ranking
        rankingService.agentAction(agentId, Constants.AgentActionEnum.APPOINTMENT_ASSIGNED, null);
    }

    // Helper method to validate agent assignment, returns false if no assignment should be made
    // For each invalid case, throws an exception.
    @SuppressWarnings("D")
    private boolean validateAssignAgent(@NonNull UUID agentId, UUID appointmentId, User agentUser, Appointment appointment) {
        if (agentUser.getSaleAgent() == null) {
            throw new IllegalArgumentException("User is not a sales agent");
        }

        // if the agent is already assigned to the appointment, do nothing
        if (appointment.getAgent() != null && appointment.getAgent().getId().equals(agentId)) {
            log.info("Agent {} is already assigned to appointment: {}", agentId, appointmentId);
            return false;
        }

        // if the agent has reached max appointments per day, throw exception
        int currentAssignments = countByAgentId(agentId);

        if (currentAssignments >= agentUser.getSaleAgent().getMaxProperties()) {
            throw new IllegalStateException("Agent has reached the maximum number of concurrent appointments");
        }

        // if the appointment is cancelled or completed, cannot assign agent
        if (appointment.getStatus() == Constants.AppointmentStatusEnum.CANCELLED ||
            appointment.getStatus() == Constants.AppointmentStatusEnum.COMPLETED) {
            throw new IllegalStateException("Cannot assign agent to a cancelled or completed appointment");
        }

        // if the appointment's requested date is in the past, cannot assign agent
        if (appointment.getRequestedDate() != null && appointment.getRequestedDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot assign agent to an appointment with a past requested date");
        }

        // if the agent is already assigned to another appointment at the same requested date and time, throw exception
        // also ensure that there's a buffer time between appointments
        List<Appointment> agentAppointments = appointmentRepository.findAllByAgent_Id(agentId);
        for (Appointment agentAppointment : agentAppointments) {
            if (agentAppointment.getRequestedDate() == null || appointment.getRequestedDate() == null)
                continue;
            var requestedDate = appointment.getRequestedDate();
            var agentRequestedDate = agentAppointment.getRequestedDate();
            var diffInMinutes = Math.abs(Duration.between(requestedDate, agentRequestedDate).toMinutes());
            if (
                    agentAppointment.getStatus() != Constants.AppointmentStatusEnum.PENDING &&
                    agentAppointment.getStatus() != Constants.AppointmentStatusEnum.CONFIRMED
            ) continue;
            if (diffInMinutes < Constants.DEFAULT_APPOINTMENT_DURATION_MINUTES) {
                throw new IllegalStateException("Agent is already assigned to another appointment at the same "
                        + "requested date and time: " + appointment.getRequestedDate());
            }
        }
        return true;
    }

    @Override
    public boolean updateAppointmentDetails(
            UUID appointmentId,
            String agentNotes,
            String viewingOutcome,
            String customerInterestLevel,
            Constants.AppointmentStatusEnum status,
            String cancelledReason) {

        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + appointmentId));

        boolean updated = false;

        // Update agent notes if not null
        if (agentNotes != null) {
            appointment.setAgentNotes(agentNotes);
            updated = true;
        }

        // Update viewing outcome if not null
        if (viewingOutcome != null) {
            appointment.setViewingOutcome(viewingOutcome);
            updated = true;
        }

        // Update customer interest level if not null
        if (customerInterestLevel != null) {
            appointment.setCustomerInterestLevel(customerInterestLevel);
            updated = true;
        }

        // Update status if not null
        if (status != null) {
            appointment.setStatus(status);
            updated = true;

            // If status is COMPLETED, track for ranking
            if (status == Constants.AppointmentStatusEnum.COMPLETED) {
                if (appointment.getAgent() != null) {
                    rankingService.agentAction(appointment.getAgent().getId(), Constants.AgentActionEnum.APPOINTMENT_COMPLETED, null);
                }
                if (appointment.getCustomer() != null) {
                    rankingService.customerAction(appointment.getCustomer().getId(), Constants.CustomerActionEnum.VIEWING_ATTENDED, null);
                }

                // Send completion notifications
                String propertyTitle = appointment.getProperty().getTitle();
                String imgUrl = appointment.getProperty().getMediaList().isEmpty() ? null :
                                appointment.getProperty().getMediaList().get(0).getFilePath();

                // Notify customer
                assert appointment.getCustomer() != null;
                notificationService.createNotification(
                    appointment.getCustomer().getUser(),
                    Constants.NotificationTypeEnum.APPOINTMENT_COMPLETED,
                    "Appointment Completed",
                    String.format("Your viewing appointment for %s has been completed. Please rate your experience!", propertyTitle),
                    Constants.RelatedEntityTypeEnum.APPOINTMENT,
                    appointment.getId().toString(),
                    imgUrl
                );

                // Notify property owner
                notificationService.createNotification(
                    appointment.getProperty().getOwner().getUser(),
                    Constants.NotificationTypeEnum.APPOINTMENT_COMPLETED,
                    "Appointment Completed",
                    String.format("The viewing appointment for your property %s has been completed.", propertyTitle),
                    Constants.RelatedEntityTypeEnum.APPOINTMENT,
                    appointment.getId().toString(),
                    imgUrl
                );

                // Notify agent if assigned
                if (appointment.getAgent() != null) {
                    notificationService.createNotification(
                        appointment.getAgent().getUser(),
                        Constants.NotificationTypeEnum.APPOINTMENT_COMPLETED,
                        "Appointment Completed",
                        String.format("The viewing appointment for property %s has been completed.", propertyTitle),
                        Constants.RelatedEntityTypeEnum.APPOINTMENT,
                        appointment.getId().toString(),
                        imgUrl
                    );
                }
            }

            // If status is CANCELLED, update cancelled fields
            if (status == Constants.AppointmentStatusEnum.CANCELLED) {
                appointment.setCancelledAt(LocalDateTime.now());
                User currentUser = userService.getUser();
                if (currentUser != null) {
                    appointment.setCancelledBy(currentUser.getRole());
                    // TODO: If cancelled by Agent => Call the ranking service to penalize
                }

                // Update cancelled reason if provided
                if (cancelledReason != null) {
                    appointment.setCancelledReason(cancelledReason);
                }

                // Send cancellation notifications
                String propertyTitle = appointment.getProperty().getTitle();
                String reasonText = cancelledReason != null ? " Reason: " + cancelledReason : "";
                String imgUrl = appointment.getProperty().getMediaList().isEmpty() ? null :
                                appointment.getProperty().getMediaList().get(0).getFilePath();

                // Notify customer
                notificationService.createNotification(
                    appointment.getCustomer().getUser(),
                    Constants.NotificationTypeEnum.APPOINTMENT_CANCELLED,
                    "Appointment Cancelled",
                    String.format("Your viewing appointment for %s has been cancelled.%s", propertyTitle, reasonText),
                    Constants.RelatedEntityTypeEnum.APPOINTMENT,
                    appointment.getId().toString(),
                    imgUrl
                );

                // Notify property owner
                notificationService.createNotification(
                    appointment.getProperty().getOwner().getUser(),
                    Constants.NotificationTypeEnum.APPOINTMENT_CANCELLED,
                    "Appointment Cancelled",
                    String.format("The viewing appointment for your property %s has been cancelled.%s", propertyTitle, reasonText),
                    Constants.RelatedEntityTypeEnum.APPOINTMENT,
                    appointment.getId().toString(),
                    imgUrl
                );

                // Notify agent if assigned
                if (appointment.getAgent() != null) {
                    notificationService.createNotification(
                        appointment.getAgent().getUser(),
                        Constants.NotificationTypeEnum.APPOINTMENT_CANCELLED,
                        "Appointment Cancelled",
                        String.format("The viewing appointment for property %s has been cancelled.%s", propertyTitle, reasonText),
                        Constants.RelatedEntityTypeEnum.APPOINTMENT,
                        appointment.getId().toString(),
                        imgUrl
                    );
                }
            }
        } else if (cancelledReason != null) {
            // Update cancelled reason even if status is not provided (in case it's already cancelled)
            appointment.setCancelledReason(cancelledReason);
            updated = true;
        }

        // Save if any field was updated
        if (updated) {
            appointmentRepository.save(appointment);
            log.info("Updated appointment details for appointment: {}", appointmentId);
        }

        return updated;
    }

    @Override
    public boolean rateAppointment(UUID appointmentId, Short rating, String comment) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() != Constants.AppointmentStatusEnum.COMPLETED) {
            throw new IllegalStateException("Only completed appointments can be rated");
        }

        boolean updated = false;

        // Validate rating (should be between 1 and 5)
        if (rating != null) {
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }
            appointment.setRating(rating);
            updated = true;
        }

        // Update comment if not null
        if (comment != null) {
            appointment.setComment(comment);
            updated = true;
        }

        // Save if any field was updated
        if (updated) {
            appointmentRepository.save(appointment);
            log.info("Rated appointment {} with rating: {}", appointmentId, rating);

            // Track rating action for agent ranking
            if (rating != null && appointment.getAgent() != null) {
                rankingService.agentAction(appointment.getAgent().getId(), Constants.AgentActionEnum.RATED, BigDecimal.valueOf(rating));
            }
        }

        return updated;
    }
}
