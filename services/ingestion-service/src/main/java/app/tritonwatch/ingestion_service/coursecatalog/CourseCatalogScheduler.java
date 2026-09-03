package app.tritonwatch.ingestion_service.coursecatalog;

import app.tritonwatch.ingestion_service.ucsd.UcsdCatalogClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseCatalogScheduler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CourseCatalogScheduler.class);

    private final CourseCatalogSyncService courseCatalogSyncService;
    private final CourseCatalogRepository courseCatalogRepository;
    private final UcsdCatalogClient ucsdCatalogClient;

    @Value("${tritonwatch.catalog.sync-on-startup:true}")
    private boolean syncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnStartup) {
            return;
        }

        try {
            String term = ucsdCatalogClient.getCurrentTerm();
            if (courseCatalogRepository.countByTerm(term) > 0) {
                log.info("Course catalog for term {} already populated; skipping startup sync", term);
                return;
            }
            log.info("Course catalog empty for term {}; running startup sync", term);
            courseCatalogSyncService.syncTerm(term);
        } catch (RuntimeException exception) {
            log.error("Startup course catalog sync failed", exception);
        }
    }

    @Scheduled(cron = "${tritonwatch.catalog.sync-cron:0 0 5 * * MON}")
    public void syncWeekly() {
        try {
            courseCatalogSyncService.syncCurrentTerm();
        } catch (RuntimeException exception) {
            log.error("Weekly course catalog sync failed", exception);
        }
    }
}
