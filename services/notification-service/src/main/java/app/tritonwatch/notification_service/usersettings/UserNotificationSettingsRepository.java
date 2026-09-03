package app.tritonwatch.notification_service.usersettings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, String> {
    List<UserNotificationSettings> findByUserIdIn(Collection<String> userIds);
}
