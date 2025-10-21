package com.mydashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Grafana Dashboard Generator
 *
 * This class defines the layout and content of the dashboard.
 * It is responsible for what panels to create, but not how to create them.
 * It uses a DashboardFactory to handle the panel JSON generation.
 */
public class GenerateDashboard {

    /**
     * Enum for the panel types used in this dashboard
     */
    public enum PanelType {
        TIMESERIES,
        GAUGE,
        BARCHART
    }

    /**
     * Configuration record for panel creation
     * Holds all the information needed to create a panel
     */
    public record PanelConfig(PanelType type, int id, String title, int x, int y, int w, int h) {
    }

    /**
     * Main entry point for the dashboard generator
     * This method is responsible for the dashboard creation process:
     * 1. Creates the dashboard structure
     * 2. Generates 3 panels
     * 3. Outputs the JSON
     * 4. Optionally sends to Grafana via API (if "send" argument provided)
     */
    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        DashboardFactory factory = new DashboardFactory(); // Instantiate the factory

        // Create the dashboard root object
        ObjectNode dashboard = mapper.createObjectNode();
        dashboard.put("editable", true);

        // Create panels array to hold all visualization panels
        ArrayNode panels = mapper.createArrayNode();

        // Defines 3-panel layout
        PanelConfig[] panelConfigs = {
                new PanelConfig(PanelType.GAUGE, 1, "Average Cluster CPU", 0, 0, 8, 8),
                new PanelConfig(PanelType.BARCHART, 2, "CPU per Server", 8, 0, 16, 8),
                new PanelConfig(PanelType.TIMESERIES, 3, "CPU Trends", 0, 8, 24, 10)
        };

        // Create panels using the factory
        for (PanelConfig config : panelConfigs) {
            ObjectNode panel = factory.createPanel(mapper, config);
            panels.add(panel);
        }

        dashboard.set("panels", panels);
        dashboard.put("refresh", "10s");

        ArrayNode tags = mapper.createArrayNode();
        tags.add("cpu_usage");
        tags.add("custom");
        dashboard.set("tags", tags);

        ObjectNode time = mapper.createObjectNode();
        time.put("from", "now-5m");
        time.put("to", "now");
        dashboard.set("time", time);

        dashboard.put("title", "CPU Usage Dashboard");
        dashboard.put("uid", "custom-dashboard");

        String dashboardJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dashboard);
        System.out.println(dashboardJson);

        // Send to Grafana if requested
        if (args.length > 0 && "send".equals(args[0])) {
            GrafanaApiClient apiClient = new GrafanaApiClient();
            apiClient.sendDashboard(dashboardJson);
        }
    }
}