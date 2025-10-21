package com.mydashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Dashboard Panel Factory
 *
 * This class is responsible for the building Grafana panels.
 */
public class DashboardFactory {

    /**
     * Public method to create any supported panel.
     *
     * @param mapper Jackson ObjectMapper
     * @param config The PanelConfig from GenerateDashboard
     * @return A complete panel as an ObjectNode
     */
    public ObjectNode createPanel(ObjectMapper mapper, GenerateDashboard.PanelConfig config) {
        ObjectNode panel = mapper.createObjectNode();

        // Common panel setup
        setupDatasource(panel, mapper);
        setupGridPos(panel, mapper, config.x(), config.y(), config.w(), config.h());

        // Setup default query. This will be overridden by specific
        // panel setup methods if needed.
        setupDefaultTarget(panel, mapper);

        panel.put("id", config.id());
        panel.put("title", config.title());
        panel.put("type", getPanelTypeString(config.type()));

        // Apply panel-specific options and query modifications
        switch (config.type()) {
            case TIMESERIES -> setupTimeSeriesOptions(panel, mapper);
            case GAUGE -> setupGaugeOptions(panel, mapper);
            case BARCHART -> setupBarChartOptions(panel, mapper);
        }

        return panel;
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private void setupDatasource(ObjectNode panel, ObjectMapper mapper) {
        ObjectNode panelDatasource = mapper.createObjectNode();
        panelDatasource.put("type", "prometheus");
        panelDatasource.put("uid", "DS_PROMETHEUS_UID");
        panel.set("datasource", panelDatasource);
    }

    private void setupGridPos(ObjectNode panel, ObjectMapper mapper, int x, int y, int w, int h) {
        ObjectNode gridPos = mapper.createObjectNode();
        gridPos.put("h", h);
        gridPos.put("w", w);
        gridPos.put("x", x);
        gridPos.put("y", y);
        panel.set("gridPos", gridPos);
    }

    /**
     * Setup the default data target for a panel.
     */
    private void setupDefaultTarget(ObjectNode panel, ObjectMapper mapper) {
        ArrayNode targets = mapper.createArrayNode();
        ObjectNode target = mapper.createObjectNode();
        ObjectNode targetDatasource = mapper.createObjectNode();
        targetDatasource.put("type", "prometheus");
        targetDatasource.put("uid", "DS_PROMETHEUS_UID");
        target.set("datasource", targetDatasource);
        target.put("expr", "cpu_usage AND (time() - timestamp(cpu_usage) < 60)");
//        target.put("expr", "cpu_usage");           // Default Prometheus query
        target.put("legendFormat", "{{instance}}");
        target.put("refId", "A");
        targets.add(target);
        panel.set("targets", targets);
    }

    /**
     * Setup field configuration for a panel
     */
    private void setupFieldConfig(ObjectNode panel, ObjectMapper mapper, String colorMode,
                                  Integer min, Integer max) {
        ObjectNode fieldConfig = mapper.createObjectNode();
        ObjectNode defaults = mapper.createObjectNode();
        defaults.put("unit", "percent");  // All panels show percentage values

        defaults.put("noValue", 0);

        if (min != null) defaults.put("min", min);
        if (max != null) defaults.put("max", max);

        ObjectNode color = mapper.createObjectNode();
        color.put("mode", colorMode);
        defaults.set("color", color);
        fieldConfig.set("defaults", defaults);
        panel.set("fieldConfig", fieldConfig);
    }

    private void setupBasicFieldConfig(ObjectNode panel, ObjectMapper mapper, String colorMode) {
        setupFieldConfig(panel, mapper, colorMode, null, null);
    }

    /**
     * Setup thresholds
     */
    private void setupThresholds(ObjectNode panel, ObjectMapper mapper) {
        ObjectNode thresholds = mapper.createObjectNode();
        ArrayNode steps = mapper.createArrayNode();

        ObjectNode step1 = mapper.createObjectNode();
        step1.put("color", "green");
        step1.put("value", (String) null); // Base color
        steps.add(step1);

        ObjectNode step2 = mapper.createObjectNode();
        step2.put("color", "red");
        step2.put("value", 80); // Red at 80
        steps.add(step2);

        thresholds.set("steps", steps);

        ObjectNode fieldConfig = (ObjectNode) panel.get("fieldConfig");
        ObjectNode defaults = (ObjectNode) fieldConfig.get("defaults");
        defaults.set("thresholds", thresholds);
    }

    /**
     * Setup reduce options for panels that need a single summary value
     */
    private void setupReduceOptions(ObjectNode options, ObjectMapper mapper) {
        ObjectNode reduceOptions = mapper.createObjectNode();
        reduceOptions.put("values", false);
        ArrayNode calcs = mapper.createArrayNode();
        calcs.add("last");
        reduceOptions.set("calcs", calcs);
        reduceOptions.put("fields", "");
        options.set("reduceOptions", reduceOptions);
    }

    // ============================================================================
    // TYPE-SPECIFIC SETUP METHODS
    // ============================================================================

    /**
     * Setup options specific to time series panel
     * This panel has two queries:
     * A: cpu_usage (shows all instances)
     * B: avg(cpu_usage) (shows cluster average)
     */
    private void setupTimeSeriesOptions(ObjectNode panel, ObjectMapper mapper) {

        setupBasicFieldConfig(panel, mapper, "palette-classic");

        // Add custom configuration for time series
        ObjectNode fieldConfig = (ObjectNode) panel.get("fieldConfig");
        ObjectNode defaults = (ObjectNode) fieldConfig.get("defaults");

        ObjectNode custom = mapper.createObjectNode();
        custom.put("axisLabel", "CPU Usage (%)");
        custom.put("fillOpacity", 10);
        custom.put("lineWidth", 2);
        defaults.set("custom", custom);

        // Add a second query (Query B) for the average
        ArrayNode targets = (ArrayNode) panel.get("targets"); // Gets Query A

        ObjectNode targetB = mapper.createObjectNode();
        ObjectNode targetBDatasource = mapper.createObjectNode();
        targetBDatasource.put("type", "prometheus");
        targetBDatasource.put("uid", "DS_PROMETHEUS_UID");
        targetB.set("datasource", targetBDatasource);
        targetB.put("expr", "avg(cpu_usage AND (time() - timestamp(cpu_usage) < 60))");
        targetB.put("legendFormat", "Average");
        targetB.put("refId", "B");
        targets.add(targetB);

        // Options
        ObjectNode options = mapper.createObjectNode();
        ObjectNode legend = mapper.createObjectNode();
        ArrayNode calcs = mapper.createArrayNode();
        calcs.add("mean");
        calcs.add("last");
        calcs.add("max");
        legend.set("calcs", calcs);
        legend.put("displayMode", "table");
        legend.put("placement", "bottom");
        legend.put("showLegend", true);
        options.set("legend", legend);
        panel.set("options", options);
    }

    /**
     * Setup options for the Gauge panel.
     * This panel overrides the default query to show a single cluster-wide average.
     */
    private void setupGaugeOptions(ObjectNode panel, ObjectMapper mapper) {

        setupFieldConfig(panel, mapper, "thresholds", 0, 100);

        setupThresholds(panel, mapper);

        // OVERRIDE the query for this panel
        ArrayNode targets = (ArrayNode) panel.get("targets");
        ObjectNode target = (ObjectNode) targets.get(0); // Get the default target
        target.put("expr", "avg(cpu_usage AND (time() - timestamp(cpu_usage) < 60))");
        target.put("legendFormat", "Average");

        target.put("instant", true);

        // Options
        ObjectNode options = mapper.createObjectNode();
        options.put("orientation", "auto");
        options.put("showThresholdLabels", false);
        options.put("showThresholdMarkers", true);

        setupReduceOptions(options, mapper);
        panel.set("options", options);
    }

    /**
     * Setup options for the Bar Chart panel.
     * This panel uses the default query (`cpu_usage`) and adds a reduce
     * transformation to make it act as a Bar Gauge (showing one bar per server).
     */
    private void setupBarChartOptions(ObjectNode panel, ObjectMapper mapper) {

        setupBasicFieldConfig(panel, mapper, "palette-classic");

        ArrayNode targets = (ArrayNode) panel.get("targets");
        ObjectNode target = (ObjectNode) targets.get(0); // Get the default target
        target.put("instant", true);

        ObjectNode options = mapper.createObjectNode();
        options.put("orientation", "auto");
        options.put("xTickLabelRotation", 0);
        options.put("xTickLabelSpacing", 0);

        setupReduceOptions(options, mapper);

        panel.set("options", options);
    }

    private String getPanelTypeString(GenerateDashboard.PanelType type) {
        return switch (type) {
            case TIMESERIES -> "timeseries";
            case GAUGE -> "gauge";
            case BARCHART -> "barchart";
        };
    }
}