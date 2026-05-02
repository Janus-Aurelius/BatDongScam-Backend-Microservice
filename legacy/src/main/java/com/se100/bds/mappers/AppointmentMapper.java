package com.se100.bds.mappers;

import com.se100.bds.dtos.responses.appointment.BookAppointmentResponse;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsCustomer;
import com.se100.bds.dtos.responses.appointment.ViewingDetailsAdmin;
import com.se100.bds.dtos.responses.appointment.ViewingListItem;
import com.se100.bds.dtos.responses.user.simple.PropertyOwnerSimpleCard;
import com.se100.bds.dtos.responses.user.simple.SalesAgentSimpleCard;
import com.se100.bds.models.entities.appointment.Appointment;
import com.se100.bds.models.entities.user.PropertyOwner;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper extends BaseMapper {
    @Autowired
    public AppointmentMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected void configureCustomMappings() {
        // NOTE: ModelMapper can occasionally mis-handle nested lambda getters inside addMappings
        // and end up invoking Appointment.getProperty() on the wrong receiver type.
        // For DTOs that read from nested associations heavily, prefer converters.

        Converter<Appointment, ViewingDetailsCustomer> viewingDetailsCustomerConverter =
                new Converter<>() {
                    @Override
                    public ViewingDetailsCustomer convert(MappingContext<Appointment, ViewingDetailsCustomer> context) {
                        Appointment src = context.getSource();
                        if (src == null) {
                            return null;
                        }

                        ViewingDetailsCustomer dst = new ViewingDetailsCustomer();

                        // Base response fields
                        dst.setId(src.getId());
                        dst.setCreatedAt(src.getCreatedAt());
                        dst.setUpdatedAt(src.getUpdatedAt());

                        // Appointment core
                        dst.setRequestedDate(src.getRequestedDate());
                        dst.setConfirmedDate(src.getConfirmedDate());
                        dst.setStatus(src.getStatus());
                        dst.setRating(src.getRating());
                        dst.setComment(src.getComment());
                        dst.setCustomerRequirements(src.getCustomerRequirements());

                        // Property-derived fields (defensively handle null)
                        if (src.getProperty() != null) {
                            dst.setTitle(src.getProperty().getTitle());
                            dst.setPriceAmount(src.getProperty().getPriceAmount());
                            dst.setArea(src.getProperty().getArea());
                            dst.setDescription(src.getProperty().getDescription());
                            dst.setRooms(src.getProperty().getRooms());
                            dst.setBathRooms(src.getProperty().getBathrooms());
                            dst.setBedRooms(src.getProperty().getBedrooms());
                            dst.setFloors(src.getProperty().getFloors());
                            dst.setHouseOrientation(src.getProperty().getHouseOrientation());
                            dst.setBalconyOrientation(src.getProperty().getBalconyOrientation());
                        }

                        dst.setNotes(src.getAgentNotes());
                        return dst;
                    }
                };

        modelMapper.typeMap(Appointment.class, ViewingDetailsCustomer.class)
                .setConverter(viewingDetailsCustomerConverter);

        Converter<Appointment, ViewingListItem> viewingListItemConverter =
                new Converter<>() {
                    @Override
                    public ViewingListItem convert(MappingContext<Appointment, ViewingListItem> context) {
                        Appointment src = context.getSource();
                        if (src == null) {
                            return null;
                        }

                        ViewingListItem dst = new ViewingListItem();

                        // Base response fields
                        dst.setId(src.getId());
                        dst.setCreatedAt(src.getCreatedAt());
                        dst.setUpdatedAt(src.getUpdatedAt());

                        // Core appointment fields
                        dst.setRequestedDate(src.getRequestedDate());
                        dst.setStatus(src.getStatus());

                        // Property-derived fields used by listing
                        if (src.getProperty() != null) {
                            dst.setPropertyName(src.getProperty().getTitle());
                            dst.setPrice(src.getProperty().getPriceAmount());
                            dst.setArea(src.getProperty().getArea());

                            if (src.getProperty().getWard() != null) {
                                dst.setWardName(src.getProperty().getWard().getWardName());
                                if (src.getProperty().getWard().getDistrict() != null) {
                                    dst.setDistrictName(src.getProperty().getWard().getDistrict().getDistrictName());
                                    if (src.getProperty().getWard().getDistrict().getCity() != null) {
                                        dst.setCityName(src.getProperty().getWard().getDistrict().getCity().getCityName());
                                    }
                                }
                            }
                        }

                        // Customer/agent display fields + thumbnail are enriched later in enrichViewingListItem()
                        // so we intentionally don't set them here.

                        return dst;
                    }
                };

        modelMapper.typeMap(Appointment.class, ViewingListItem.class)
                .setConverter(viewingListItemConverter);

        Converter<Appointment, BookAppointmentResponse> bookAppointmentResponseConverter =
                new Converter<>() {
                    @Override
                    public BookAppointmentResponse convert(MappingContext<Appointment, BookAppointmentResponse> context) {
                        Appointment src = context.getSource();
                        if (src == null) {
                            return null;
                        }

                        BookAppointmentResponse dst = new BookAppointmentResponse();
                        dst.setAppointmentId(src.getId());

                        if (src.getProperty() != null) {
                            dst.setPropertyId(src.getProperty().getId());
                            dst.setPropertyTitle(src.getProperty().getTitle());
                            dst.setPropertyAddress(src.getProperty().getFullAddress());
                        }

                        dst.setRequestedDate(src.getRequestedDate());
                        dst.setStatus(src.getStatus() != null ? src.getStatus().name() : null);
                        dst.setCustomerRequirements(src.getCustomerRequirements());

                        if (src.getAgent() != null) {
                            dst.setAgentId(src.getAgent().getId());
                            if (src.getAgent().getUser() != null) {
                                dst.setAgentName(src.getAgent().getUser().getFullName());
                            }
                        }

                        // Created/updated are available from AbstractBaseEntity
                        dst.setCreatedAt(src.getCreatedAt());
                        return dst;
                    }
                };

        modelMapper.typeMap(Appointment.class, BookAppointmentResponse.class)
                .setConverter(bookAppointmentResponseConverter);
    }

    /**
     * Build PropertyOwnerSimpleCard from PropertyOwner entity
     */
    public PropertyOwnerSimpleCard buildOwnerCard(PropertyOwner owner, String tier) {
        return PropertyOwnerSimpleCard.builder()
                // Base fields (AbstractBaseDataResponse)
                .id(owner.getId())
                .createdAt(owner.getCreatedAt())
                .updatedAt(owner.getUpdatedAt())

                // User fields
                .firstName(owner.getUser().getFirstName())
                .lastName(owner.getUser().getLastName())
                .phoneNumber(owner.getUser().getPhoneNumber())
                .zaloContact(owner.getUser().getZaloContact())
                .email(owner.getUser().getEmail())

                // Domain fields
                .tier(tier)
                .build();
    }

    /**
     * Build SalesAgentSimpleCard from SaleAgent entity
     */
    public SalesAgentSimpleCard buildAgentCard(SaleAgent agent, String tier, double rating, int totalRates) {
        return SalesAgentSimpleCard.builder()
                .id(agent.getId())
                .firstName(agent.getUser().getFirstName())
                .lastName(agent.getUser().getLastName())
                .phoneNumber(agent.getUser().getPhoneNumber())
                .zaloContact(agent.getUser().getZaloContact())
                .tier(tier)
                .rating(rating)
                .totalRates(totalRates)
                .build();
    }

    /**
     * I FUCKING SWEAR TO GOD I HAVE NO FUCKING IDEA WHY THIS SHIT WORKS BUT THE MAPPER DOESN'T
     * THE LAST TIME I USE MAPPER IN THIS SHIT LANGUAGE
     */
    public ViewingListItem mapToViewingListItem(Appointment appointment) {
        ViewingListItem dto = new ViewingListItem();

        // Map basic appointment fields
        dto.setId(appointment.getId());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setUpdatedAt(appointment.getUpdatedAt());
        dto.setRequestedDate(appointment.getRequestedDate());
        dto.setStatus(appointment.getStatus());

        // Map property fields (safely handle null)
        if (appointment.getProperty() != null) {
            dto.setPropertyName(appointment.getProperty().getTitle());
            dto.setPrice(appointment.getProperty().getPriceAmount());
            dto.setArea(appointment.getProperty().getArea());

            // Map location fields
            if (appointment.getProperty().getWard() != null) {
                dto.setWardName(appointment.getProperty().getWard().getWardName());

                if (appointment.getProperty().getWard().getDistrict() != null) {
                    dto.setDistrictName(appointment.getProperty().getWard().getDistrict().getDistrictName());

                    if (appointment.getProperty().getWard().getDistrict().getCity() != null) {
                        dto.setCityName(appointment.getProperty().getWard().getDistrict().getCity().getCityName());
                    }
                }
            }

            // Set thumbnail
            if (appointment.getProperty().getMediaList() != null &&
                !appointment.getProperty().getMediaList().isEmpty()) {
                dto.setThumbnailUrl(appointment.getProperty().getMediaList().get(0).getFilePath());
            }
        }

        return dto;
    }

    /**
     * Enrich ViewingListItemDto with customer and agent data
     * This must be called after the base mapping to populate lazy-loaded relationships
     */
    public void enrichViewingListItem(ViewingListItem dto, Appointment appointment, String customerTier, String agentTier) {
        if (appointment.getCustomer() != null && appointment.getCustomer().getUser() != null) {
            dto.setCustomerName(appointment.getCustomer().getUser().getFullName());
        }
        dto.setCustomerTier(customerTier);

        if (appointment.getAgent() != null && appointment.getAgent().getUser() != null) {
            dto.setSalesAgentName(appointment.getAgent().getUser().getFullName());
        }
        dto.setSalesAgentTier(agentTier);
    }

    /**
     * Build PropertyCard for ViewingDetailsAdmin
     */
    public ViewingDetailsAdmin.PropertyCard buildPropertyCard(Appointment appointment) {
        var propertyCard = new ViewingDetailsAdmin.PropertyCard();
        propertyCard.setId(appointment.getProperty().getId());
        propertyCard.setTitle(appointment.getProperty().getTitle());
        propertyCard.setTransactionType(appointment.getProperty().getTransactionType());
        propertyCard.setType(appointment.getProperty().getPropertyType().getTypeName());
        propertyCard.setFullAddress(appointment.getProperty().getFullAddress());
        propertyCard.setPrice(appointment.getProperty().getPriceAmount());
        propertyCard.setArea(appointment.getProperty().getArea());

        // Set thumbnail
        if (appointment.getProperty().getMediaList() != null && !appointment.getProperty().getMediaList().isEmpty()) {
            propertyCard.setThumbnailUrl(appointment.getProperty().getMediaList().get(0).getFilePath());
        }

        return propertyCard;
    }

    /**
     * Build UserSimpleCard for ViewingDetailsAdmin
     */
    public ViewingDetailsAdmin.UserSimpleCard buildUserSimpleCard(
            User user, String tier) {
        var userCard = new ViewingDetailsAdmin.UserSimpleCard();
        userCard.setId(user.getId());
        userCard.setFullName(user.getFullName());
        userCard.setTier(tier);
        userCard.setPhoneNumber(user.getPhoneNumber());
        userCard.setEmail(user.getEmail());
        return userCard;
    }

    /**
     * Build SalesAgentSimpleCard for ViewingDetailsAdmin
     */
    public ViewingDetailsAdmin.SalesAgentSimpleCard buildSalesAgentSimpleCard(
            User agentUser, String tier, Double rating, Integer totalRates) {
        var agentCard = new ViewingDetailsAdmin.SalesAgentSimpleCard();
        agentCard.setId(agentUser.getId());
        agentCard.setFullName(agentUser.getFullName());
        agentCard.setTier(tier);
        agentCard.setPhoneNumber(agentUser.getPhoneNumber());
        agentCard.setEmail(agentUser.getEmail());
        agentCard.setRating(rating);
        agentCard.setTotalRates(totalRates);
        return agentCard;
    }

    /**
     * Build BookAppointmentResponse using configured mappings
     */
    public BookAppointmentResponse buildBookingResponse(Appointment appointment, String message) {
        BookAppointmentResponse response = modelMapper.map(appointment, BookAppointmentResponse.class);
        response.setMessage(message);
        return response;
    }
}
