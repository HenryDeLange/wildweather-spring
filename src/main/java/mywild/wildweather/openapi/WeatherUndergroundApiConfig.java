package mywild.wildweather.openapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import mywild.weatherunderground.openapi.client.ApiClient;
import mywild.weatherunderground.openapi.client.api.WeatherUndergroundApi;
import mywild.weatherunderground.openapi.client.auth.ApiKeyAuth;

@Slf4j
@Configuration
public class WeatherUndergroundApiConfig {

    @Value("${mywild.weather-underground.api-key}")
    private String apiKey;

    @Bean
    ApiClient weatherUndergroundApiClient() {
        ApiClient apiClient = mywild.weatherunderground.openapi.client.Configuration.getDefaultApiClient();
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) apiClient.getAuthentication("ApiKeyAuth");
        apiKeyAuth.setApiKey(apiKey);
        return apiClient;
    }

    @Bean
    WeatherUndergroundApi weatherUndergroundApi(ApiClient weatherUndergroundApiClient) {
        return new WeatherUndergroundApi(weatherUndergroundApiClient);
    }

}
