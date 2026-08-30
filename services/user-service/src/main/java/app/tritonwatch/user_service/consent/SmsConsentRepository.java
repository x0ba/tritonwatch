package app.tritonwatch.user_service.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SmsConsentRepository extends JpaRepository<SmsConsent, UUID> {
}
