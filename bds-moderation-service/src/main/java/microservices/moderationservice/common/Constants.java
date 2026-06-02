package microservices.moderationservice.common;

public final class Constants {
    private Constants() {
    }

    public enum ViolationReportedTypeEnum {
        CUSTOMER,
        PROPERTY,
        SALES_AGENT,
        PROPERTY_OWNER
    }

    public enum ViolationTypeEnum {
        FRAUDULENT_LISTING,
        MISREPRESENTATION_OF_PROPERTY,
        SPAM_OR_DUPLICATE_LISTING,
        INAPPROPRIATE_CONTENT,
        NON_COMPLIANCE_WITH_TERMS,
        FAILURE_TO_DISCLOSE_INFORMATION,
        HARASSMENT,
        SCAM_ATTEMPT
    }

    public enum ViolationStatusEnum {
        PENDING,
        REPORTED,
        UNDER_REVIEW,
        RESOLVED,
        DISMISSED
    }

    public enum PenaltyAppliedEnum {
        WARNING,
        REMOVED_POST,
        SUSPENDED_ACCOUNT
    }
}
