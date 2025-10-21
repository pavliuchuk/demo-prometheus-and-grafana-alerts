package com.mydashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * Grafana API Client
 *
 * This class handles all interactions with the Grafana API, providing a clean
 * separation between dashboard generation and API operations.
 */
public class GrafanaApiClient {
    
    // Default configuration constants
    private static final String DEFAULT_GRAFANA_URL = "http://localhost:3000";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    
    private final String grafanaUrl;
    private final String authHeader;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public GrafanaApiClient() {
        this(
                getEnv("GRAFANA_URL", DEFAULT_GRAFANA_URL),
                getEnv("GRAFANA_USER", DEFAULT_USERNAME),
                getEnv("GRAFANA_PASSWORD", DEFAULT_PASSWORD)
        );
    }

    public GrafanaApiClient(String grafanaUrl, String username, String password) {
        this.grafanaUrl = grafanaUrl.endsWith("/") ? grafanaUrl : grafanaUrl + "/";
        this.authHeader = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }
    
    /**
     * Send dashboard JSON to Grafana via REST API
     *
     * The API call will create a new dashboard or update an existing one
     * based on the dashboard UID in the JSON.
     *
     */
    public boolean sendDashboard(String dashboardJson) {
        try {
            // Create request body
            ObjectNode requestBody = mapper.createObjectNode();
            requestBody.set("dashboard", mapper.readTree(dashboardJson));
            requestBody.put("overwrite", true);
            
            String requestBodyJson = mapper.writeValueAsString(requestBody);
            
            // Create HTTP request
            String apiUrl = grafanaUrl + "api/dashboards/db";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("\n Dashboard successfully sent to Grafana!");
                System.out.println("Dashboard URL: " + grafanaUrl);
                return true;
            } else {
                System.err.println("Failed to send dashboard to Grafana");
                System.err.println("Status Code: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error sending dashboard to Grafana: " + e.getMessage());
            return false;
        }
    }
    private static String getEnv(String varName, String defaultValue) {
        String value = System.getenv(varName);
        return (value != null) ? value : defaultValue;
    }
}
