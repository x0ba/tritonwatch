package app.tritonwatch.user_service.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactVerificationChallengeRepository extends JpaRepository<ContactVerificationChallenge, UUID> {
    Optional<ContactVerificationChallenge> findByUserIdAndChannel(String userId, VerificationChannel channel);
}
