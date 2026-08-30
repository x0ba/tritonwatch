package app.tritonwatch.user_service.consent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "sms_consents")
public class SmsConsent {

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @NotBlank
    @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$")
    @Size(max = 16)
    @Column(name = "phone_e164", nullable = false, updatable = false, length = 16)
    private String phoneE164;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private SmsConsentAction action;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, updatable = false, length = 40)
    private String source;

    @NotBlank
    @Size(max = 40)
    @Column(name = "policy_version", nullable = false, updatable = false, length = 40)
    private String policyVersion;

    @NotNull
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public static SmsConsent record(
            String userId,
            String phoneE164,
            SmsConsentAction action,
            String source,
            String policyVersion,
            Instant occurredAt
    ) {
        SmsConsent consent = new SmsConsent();
        consent.id = UUID.randomUUID();
        consent.userId = userId;
        consent.phoneE164 = phoneE164;
        consent.action = action;
        consent.source = source;
        consent.policyVersion = policyVersion;
        consent.occurredAt = occurredAt;
        return consent;
    }
}
