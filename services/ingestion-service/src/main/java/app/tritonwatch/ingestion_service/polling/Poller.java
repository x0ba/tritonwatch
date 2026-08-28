package app.tritonwatch.ingestion_service.polling;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Poller {

    private final PollerService pollerService;

    @Scheduled(fixedDelayString = "${tritonwatch.ingestion.poll-interval}")
    public void poll() {

    }
}
