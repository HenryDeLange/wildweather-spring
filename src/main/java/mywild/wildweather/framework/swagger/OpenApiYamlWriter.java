package mywild.wildweather.framework.swagger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OpenApiYamlWriter {

    @Value("${mywild.app.dev-mode}")
    private boolean devMode;

    @Value("${server.port:8080}")
    private int port;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @EventListener
    public void onWebServerReady(WebServerInitializedEvent event) throws Exception {
        if (devMode) {
            try {
                String url = "http://localhost:" + port + contextPath + "/v3/api-docs.yaml";
                log.debug("DEV: Fetch OpenAPI YML from: {}", url);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                Path ymlPath = Paths.get("src/main/openapi/api.yml");
                log.debug("DEV: Write OpenAPI YML to: {}", ymlPath.toAbsolutePath());
                Files.createDirectories(ymlPath.getParent());
                Files.write(ymlPath, response.body().getBytes());
            }
            catch (IOException | InterruptedException ex) {
                log.error("Failed to write OpenAPI YML!", ex);
            }
        }
    }

}