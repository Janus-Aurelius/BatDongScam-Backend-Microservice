package com.se100.bds.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.stream.Stream;

public final class Constants {
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    public static final String TOKEN_HEADER = "Authorization";

    public static final String TOKEN_TYPE = "Bearer";

    public static final BigDecimal DEFAULT_PROPERTY_COMMISSION_RATE = new BigDecimal("0.05");

    // Will be used to set default appointment end time = start time + duration
    // to ensure enough time is reserved when booking
    public static final int DEFAULT_APPOINTMENT_DURATION_MINUTES = 30;

    @Getter
    @AllArgsConstructor
    public enum RoleEnum {
        ADMIN("ADMIN"),
        SALESAGENT("SALESAGENT"),
        PROPERTY_OWNER("PROPERTY_OWNER"),
        CUSTOMER("CUSTOMER");

        private final String value;

        public static RoleEnum get(final String name) {
            return Stream.of(RoleEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum StatusProfileEnum {
        ACTIVE("ACTIVE"),
        SUSPENDED("SUSPENDED"),
        PENDING_APPROVAL("PENDING_APPROVAL"),
        DELETED("DELETED"),
        REJECTED("REJECTED");

        private final String value;
        public static StatusProfileEnum get(final String name) {
            return Stream.of(StatusProfileEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid status profile name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum CustomerTierEnum {
        BRONZE("BRONZE"),
        SILVER("SILVER"),
        GOLD("GOLD"),
        PLATINUM("PLATINUM");

        private final String value;

        public static CustomerTierEnum get(final String name) {
            return Stream.of(CustomerTierEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid customer tier name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PerformanceTierEnum {
        BRONZE("BRONZE"),
        SILVER("SILVER"),
        GOLD("GOLD"),
        PLATINUM("PLATINUM");

        private final String value;

        public static PerformanceTierEnum get(final String name) {
            return Stream.of(PerformanceTierEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid customer tier name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ContributionTierEnum {
        BRONZE("BRONZE"),
        SILVER("SILVER"),
        GOLD("GOLD"),
        PLATINUM("PLATINUM");

        private final String value;

        public static ContributionTierEnum get(final String name) {
            return Stream.of(ContributionTierEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid contribution tier name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum AppointmentStatusEnum {
        PENDING("PENDING"),
        CONFIRMED("CONFIRMED"),
        COMPLETED("COMPLETED"),
        CANCELLED("CANCELLED");

        private final String value;

        public static AppointmentStatusEnum get(final String name) {
            return Stream.of(AppointmentStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid appointment status name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ContractTypeEnum {
        DEPOSIT("DEPOSIT"),
        PURCHASE("PURCHASE"),
        RENTAL("RENTAL");

        private final String value;

        public static ContractTypeEnum get(final String name) {
            return Stream.of(ContractTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid contract type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum MainContractTypeEnum {
        PURCHASE("PURCHASE"),
        RENTAL("RENTAL");

        private final String value;

        public static ContractTypeEnum get(final String name) {
            return Stream.of(ContractTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid contract type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ContractStatusEnum {
        /// draft state
        DRAFT("DRAFT"),
        /// waiting for remaining payments to be made
        PENDING_PAYMENT("PENDING_PAYMENT"),
        /// all remaining balance paid, waiting for official documentations to be made and legalised
        WAITING_OFFICIAL("WAITING_OFFICIAL"),
        /// in effect with legal bindings (exists a signed contract document)
        ACTIVE("ACTIVE"),
        /// completed contract in case of rental
        COMPLETED("COMPLETED"),
        /// contract terminated before end date
        CANCELLED("CANCELLED");

        private final String value;

        public static ContractStatusEnum get(final String name) {
            return Stream.of(ContractStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid contract status name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SecurityDepositStatusEnum {
        NOT_PAID("NOT_PAID"),
        HELD("HELD"),
        RETURNED_TO_CUSTOMER("RETURNED_TO_CUSTOMER"),
        TRANSFERRED_TO_OWNER("TRANSFERRED_TO_OWNER");

        private final String value;

        public static SecurityDepositStatusEnum get(final String name) {
            return Stream.of(SecurityDepositStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid security deposit status: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PaymentTypeEnum {
        DEPOSIT("DEPOSIT"),
        ADVANCE("ADVANCE"),
        INSTALLMENT("INSTALLMENT"),
        FULL_PAY("FULL_PAY"),
        MONTHLY("MONTHLY"),
        PENALTY("PENALTY"),
        MONEY_SALE("MONEY_SALE"),
        MONEY_RENTAL("MONEY_RENTAL"),
        SALARY("SALARY"),
        BONUS("BONUS"),
        SERVICE_FEE("SERVICE_FEE"),
        PAYMENT_OVERDUE("PAYMENT_OVERDUE"),
        SECURITY_DEPOSIT("SECURITY_DEPOSIT");

        private final String value;

        public static PaymentTypeEnum get(final String name) {
            return Stream.of(PaymentTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid payment type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PaymentStatusEnum {
        PENDING("PENDING"),
        SUCCESS("SUCCESS"),
        FAILED("FAILED"),
        SYSTEM_PENDING("SYSTEM_PENDING"),
        SYSTEM_SUCCESS("SYSTEM_SUCCESS"),
        SYSTEM_FAILED("SYSTEM_FAILED");

        private final String value;

        public static PaymentStatusEnum get(final String name) {
            return Stream.of(PaymentStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid payment type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum VerificationStatusEnum {
        PENDING("PENDING"),
        VERIFIED("VERIFIED"),
        REJECTED("REJECTED");

        private final String value;

        public static VerificationStatusEnum get(final String name) {
            return Stream.of(VerificationStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid verification status name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum NotificationTypeEnum {
        APPOINTMENT_BOOKED("APPOINTMENT_BOOKED"),
        APPOINTMENT_CANCELLED("APPOINTMENT_CANCELLED"),
        APPOINTMENT_COMPLETED("APPOINTMENT_COMPLETED"),
        APPOINTMENT_ASSIGNED("APPOINTMENT_ASSIGNED"),
        APPOINTMENT_REMINDER("APPOINTMENT_REMINDER"),
        CONTRACT_UPDATE("CONTRACT_UPDATE"),
        PAYMENT_DUE("PAYMENT_DUE"),
        PAYMENT_OVERDUE("PAYMENT_OVERDUE"),
        VIOLATION_WARNING("VIOLATION_WARNING"),
        PROPERTY_APPROVAL("PROPERTY_APPROVAL"),
        PROPERTY_REJECTION("PROPERTY_REJECTION"),
        SYSTEM_ALERT("SYSTEM_ALERT");

        private final String value;

        public static NotificationTypeEnum get(final String name) {
            return Stream.of(NotificationTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid notification type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum RelatedEntityTypeEnum {
        PROPERTY("PROPERTY"),
        CONTRACT("CONTRACT"),
        PAYMENT("PAYMENT"),
        APPOINTMENT("APPOINTMENT"),
        USER("USER");

        private final String value;

        public static RelatedEntityTypeEnum get(final String name) {
            return Stream.of(RelatedEntityTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid related entity type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum NotificationStatusEnum {
        PENDING("PENDING"),
        SENT("SENT"),
        READ("READ"),
        FAILED("FAILED");

        private final String value;

        public static NotificationStatusEnum get(final String name) {
            return Stream.of(NotificationStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid notification status name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum MediaTypeEnum {
        IMAGE("IMAGE"),
        VIDEO("VIDEO"),
        DOCUMENT("DOCUMENT");

        private final String value;

        public static MediaTypeEnum get(final String name) {
            return Stream.of(MediaTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid media type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum TransactionTypeEnum {
        SALE("SALE"),
        RENTAL("RENTAL");

        private final String value;

        public static TransactionTypeEnum get(final String name) {
            return Stream.of(TransactionTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid transaction type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum LocationEnum {
        CITY("CITY"),
        WARD("WARD"),
        DISTRICT("DISTRICT");

        private final String value;

        public static LocationEnum get(final String name) {
            return Stream.of(LocationEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid orientation name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum OrientationEnum {
        NORTH("NORTH"),
        SOUTH("SOUTH"),
        EAST("EAST"),
        WEST("WEST"),
        NORTH_EAST("NORTH_EAST"),
        NORTH_WEST("NORTH_WEST"),
        SOUTH_EAST("SOUTH_EAST"),
        SOUTH_WEST("SOUTH_WEST"),
        UNKNOWN("UNKNOWN");

        private final String value;

        public static OrientationEnum get(final String name) {
            return Stream.of(OrientationEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid orientation name: %s", name)));
        }
    }

    ///  VIOLATION REPORT
    @Getter
    @AllArgsConstructor
    public enum ViolationReportedTypeEnum {
        CUSTOMER("CUSTOMER"),
        PROPERTY("PROPERTY"),
        SALES_AGENT("SALES_AGENT"),
        PROPERTY_OWNER("PROPERTY_OWNER"),;

        private final String value;

        public static ViolationReportedTypeEnum get(final String name) {
            return Stream.of(ViolationReportedTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid violation reported type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ViolationTypeEnum {
        FRAUDULENT_LISTING("FRAUDULENT_LISTING"),
        MISREPRESENTATION_OF_PROPERTY("MISREPRESENTATION_OF_PROPERTY"),
        SPAM_OR_DUPLICATE_LISTING("SPAM_OR_DUPLICATE_LISTING"),
        INAPPROPRIATE_CONTENT("INAPPROPRIATE_CONTENT"),
        NON_COMPLIANCE_WITH_TERMS("NON_COMPLIANCE_WITH_TERMS"),
        FAILURE_TO_DISCLOSE_INFORMATION("FAILURE_TO_DISCLOSE_INFORMATION"),
        HARASSMENT("HARASSMENT"),
        SCAM_ATTEMPT("SCAM_ATTEMPT");

        private final String value;

        public static ViolationTypeEnum get(final String name) {
            return Stream.of(ViolationTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid violation type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ViolationStatusEnum {
        PENDING("PENDING"),
        REPORTED("REPORTED"),
        UNDER_REVIEW("UNDER_REVIEW"),
        RESOLVED("RESOLVED"),
        DISMISSED("DISMISSED");

        private final String value;

        public static ViolationStatusEnum get(final String name) {
            return Stream.of(ViolationStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid violation status name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PenaltyAppliedEnum {
        WARNING("WARNING"),
        REMOVED_POST("REMOVED_POST"),
        SUSPENDED_ACCOUNT("SUSPENDED_ACCOUNT");

        private final String value;

        public static PenaltyAppliedEnum get(final String name) {
            return Stream.of(PenaltyAppliedEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid violation status name: %s", name)));
        }
    }

    ///

    @Getter
    @AllArgsConstructor
    public enum ReportTypeEnum { // Statistic report, not violation report
        FINANCIAL("FINANCIAL"),
        AGENT_PERFORMANCE("AGENT_PERFORMANCE"),
        PROPERTY_STATISTICS("PROPERTY_STATISTICS"),
        PROPERTY_OWNER_CONTRIBUTION("PROPERTY_OWNER_CONTRIBUTION"),
        CUSTOMER_ANALYTICS("CUSTOMER_ANALYTICS"),
        VIOLATION("VIOLATION");

        private final String value;

        public static ReportTypeEnum get(final String name) {
            return Stream.of(ReportTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid report type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PropertyStatusEnum {
        PENDING("PENDING"),
        REJECTED("REJECTED"),
        APPROVED("APPROVED"),
        SOLD("SOLD"),
        RENTED("RENTED"),
        AVAILABLE("AVAILABLE"),
        UNAVAILABLE("UNAVAILABLE"),
        REMOVED("REMOVED"), // Due to violation reported
        DELETED("DELETED");

        private final String value;

        public static PropertyStatusEnum get(final String name) {
            return Stream.of(PropertyStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid property status name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum LikeTypeEnum {
        PROPERTY("PROPERTY"),
        CITY("CITY"),
        DISTRICT("DISTRICT"),
        WARD("WARD"),
        PROPERTY_TYPE("PROPERTY_TYPE");

        private final String value;

        public static LikeTypeEnum get(final String name) {
            return Stream.of(LikeTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid like type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum SearchTypeEnum {
        PROPERTY("PROPERTY"),
        CITY("CITY"),
        DISTRICT("DISTRICT"),
        WARD("WARD"),
        PROPERTY_TYPE("PROPERTY_TYPE");

        private final String value;

        public static SearchTypeEnum get(final String name) {
            return Stream.of(SearchTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid like type name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum AgentActionEnum {
        PROPERTY_ASSIGNED("PROPERTY_ASSIGNED"),
        APPOINTMENT_ASSIGNED("APPOINTMENT_ASSIGNED"),
        APPOINTMENT_COMPLETED("APPOINTMENT_COMPLETED"),
        APPOINTMENT_CANCELLED("APPOINTMENT_CANCELLED"),
        CONTRACT_SIGNED("CONTRACT_SIGNED"),
        RATED("RATED");

        private final String value;

        public static AgentActionEnum get(final String name) {
            return Stream.of(AgentActionEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid agent action name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum CustomerActionEnum {
        VIEWING_REQUESTED("VIEWING_REQUESTED"),
        VIEWING_ATTENDED("VIEWING_ATTENDED"),
        VIEWING_CANCELLED("VIEWING_CANCELLED"),
        SPENDING_MADE("SPENDING_MADE"),
        PURCHASE_MADE("PURCHASE_MADE"),
        RENTAL_MADE("RENTAL_MADE"),
        CONTRACT_SIGNED("CONTRACT_SIGNED");

        private final String value;

        public static CustomerActionEnum get(final String name) {
            return Stream.of(CustomerActionEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid customer action name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PropertyOwnerActionEnum {
        PROPERTY_FOR_SALE_LISTED("PROPERTY_FOR_SALE_LISTED"),
        PROPERTY_FOR_RENT_LISTED("PROPERTY_FOR_RENT_LISTED"),
        PROPERTY_SOLD("PROPERTY_SOLD"),
        PROPERTY_RENTED("PROPERTY_RENTED"),
        MONEY_RECEIVED("MONEY_RECEIVED");

        private final String value;

        public static PropertyOwnerActionEnum get(final String name) {
            return Stream.of(PropertyOwnerActionEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid property owner action name: %s", name)));
        }
    }
}
