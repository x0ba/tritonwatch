package app.tritonwatch.ingestion_service.ucsd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UcsdCatalogClientConfig {

    @Bean
    RestClient ucsdRestClient(
            @Value("${tritonwatch.ucsd-api.base-url}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
