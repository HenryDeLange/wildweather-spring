package mywild.wildweather.openapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import mywild.ambientweather.openapi.client.ApiClient;
import mywild.ambientweather.openapi.client.api.AmbientWeatherApi;
import mywild.ambientweather.openapi.client.auth.ApiKeyAuth;

@Slf4j
@Configuration
public class AmbientWeatherApiConfig {

    @Value("${mywild.ambient-weather.app-key}")
    private String appKey;

    @Value("${mywild.ambient-weather.api-key}")
    private String apiKey;

    @Bean
    ApiClient ambientWeatherApiClient() {
        ApiClient apiClient = mywild.ambientweather.openapi.client.Configuration.getDefaultApiClient();
        ApiKeyAuth appKeyAuth = (ApiKeyAuth) apiClient.getAuthentication("AppKeyAuth");
        appKeyAuth.setApiKey(appKey);
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) apiClient.getAuthentication("ApiKeyAuth");
        apiKeyAuth.setApiKey(apiKey);
        return apiClient;
    }

    @Bean
    AmbientWeatherApi ambientWeatherApi(ApiClient ambientWeatherApiClient) {
        return new AmbientWeatherApi(ambientWeatherApiClient);
    }

}
