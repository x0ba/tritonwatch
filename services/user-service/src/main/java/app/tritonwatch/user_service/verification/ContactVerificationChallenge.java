package app.tritonwatch.user_service.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "contact_verification_challenges",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_contact_verification_challenges_user_channel",
                columnNames = {"user_id", "channel"}
        )
)
public class ContactVerificationChallenge {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationChannel channel;

    @NotBlank
    @Size(max = 320)
    @Column(nullable = false, length = 320)
    private String destination;

    @NotBlank
    @Size(max = 128)
    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ContactVerificationChallenge create(
            UUID id,
            String userId,
            VerificationChannel channel,
            String destination,
            String codeHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        ContactVerificationChallenge challenge = new ContactVerificationChallenge();
        challenge.id = id;
        challenge.userId = userId;
        challenge.channel = channel;
        challenge.destination = destination;
        challenge.codeHash = codeHash;
        challenge.expiresAt = expiresAt;
        challenge.createdAt = createdAt;
        return challenge;
    }

    public void refresh(String destination, String codeHash, Instant expiresAt) {
        this.destination = destination;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.consumedAt = null;
    }

    public boolean isUsable(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }
}
