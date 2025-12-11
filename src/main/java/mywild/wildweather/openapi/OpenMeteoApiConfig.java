package mywild.wildweather.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import mywild.climateserv.openapi.client.ApiClient;
import mywild.climateserv.openapi.client.api.ClimateServApi;

@Slf4j
@Configuration
public class OpenMeteoApiConfig {

    @Bean
    ApiClient climateServApiClient() {
        ApiClient apiClient = mywild.climateserv.openapi.client.Configuration.getDefaultApiClient();
        return apiClient;
    }

    @Bean
    ClimateServApi climateServApi(ApiClient climateServApiClient) {
        return new ClimateServApi(climateServApiClient);
    }

}
