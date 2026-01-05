package mywild.wildweather.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import mywild.openmeteo.openapi.client.ApiClient;
import mywild.openmeteo.openapi.client.api.OpenMeteoApi;

@Slf4j
@Configuration
public class OpenMeteoApiConfig {

    @Bean
    ApiClient openMeteoApiClient() {
        ApiClient apiClient = mywild.openmeteo.openapi.client.Configuration.getDefaultApiClient();
        return apiClient;
    }

    @Bean
    OpenMeteoApi openMeteoApi(ApiClient openMeteoApiClient) {
        return new OpenMeteoApi(openMeteoApiClient);
    }

}
