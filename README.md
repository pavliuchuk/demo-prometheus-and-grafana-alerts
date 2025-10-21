# Custom Grafana Dashboard for cpu_usage

The Repository contains the *observability-as-code* project using the original Grafana demo environment, Java and Maven. It creates a Grafana dashboard to visualise the **cpu_usage** using the Grafana Foundation SDK for Java.

### Project Structure & Added Components

**New Components Added:**
- **`src/main/java/com/mydashboard/GenerateDashboard.java`** - Main dashboard generator class, defines the dashboard's layout and panels.
- **`src/main/java/com/mydashboard/DashboardFactory.java`** - A reusable factory. It encapsulates all the JSON logic for building panels.
- **`src/main/java/com/mydashboard/GrafanaApiClient.java`** - API client for Grafana integration  
- **`.github/workflows/pipeline-dashboard.yml`** - Pipeline configuration for automated dashboard deployment.
- **`pom.xml`** - Maven configuration dependencies for Jackson and Grafana Foundation SDK

**Key Features Added:**
- **Observability as Code** - A complete workflow where the dashboard is defined in Java, version-controlled in Git and automatically built and deployed to Grafana via the GitHub Actions CI/CD pipeline.
- **Clean Architecture** - Separation between the dashboard's definition and the panel creation.
- **Visualization** - Creation of a 3-panel dashboard showing Avarage Value, Per Server Breakdown and Over Time Usage.
- **Real-time Data Filtering** - Panels are implemented using a PromQL query to filter out any data older than 60 seconds. This ensures all panels correctly drop to 0 when the data stream stops.
- **API Integration** - Direct deployment to Grafana via REST API

### How to Run the Project

#### Path 1: Local-Only Quick Test

1. **Clone and Navigate**
   ```bash
   git clone <repository-url>
   cd demo-prometheus-and-grafana-alerts
   ```

2. **Run Original Demo Environment**
   ```bash
   docker compose up -d
   ```
3. **Generate .json File** (Optional)
    ```bash
   mvn clean install exec:java > generated-dashboard.json
   ```
   run this command in case you wanna see the json file, it will generate the file with name generated-dashboard.json 

   better proceed to step 4.


4. **Generate and Deploy to Grafana**
   ```bash
   mvn exec:java -Dexec.mainClass="com.mydashboard.GenerateDashboard" -Dexec.args="send"
   ```
   This command will automatically generate the json and send it to Grafana


5. **Generate Test Data and View Dashboard**
   ```bash
   k6 run --duration 10m testdata/3.add-instances.js
   ```
   This command runs three servers for 10 minutes and provides the test data. (This is taken from original repository) 
   
   After that, open http://localhost:3000 in your browser, go to Dashboards, choose the generated dashboard **CPU Usage Dashboard**, and you will see the **cpu_usage** data visualised there.

#### Path 2: Full Pipeline Test

This path demonstrates the complete *observability-as-code* workflow.

1. **Fork and Clone Your Fork**


2. **Run Original Demo Environment**
   ```bash
   docker compose up -d
   ```
3. **Get a Public URL** (using **`ngrok`**)
  * Download and run ngrok.
  * In terminal run this command to create a public URL for Grafana:
    ```bash
     ngrok http 3000
    ```
  * Copy the public URL
  * Keep this terminal open

4. **Set Up GitHub Secrets in Your Fork**

   *`GRAFANA_URL`*: The ngrok URL

   *`GRAFANA_USER`*: admin

   *`GRAFANA_PASSWORD`*: admin


5. **Trigger the Pipeline**
 * Create and push a new branch
 * Open a New Pull Request to merge your branch into *`main`*

6. **Verify the Pipeline**
 * The *`deploy-dashboard`* job will run, connect to your ngrok URL and deploy the dashboard to Grafana
7. **Generate Test Data**
    ```bash
    k6 run --duration 10m testdata/3.add-instances.js
    ```
   This command runs three servers for 10 minutes and provides the test data. (This is taken from original repository)


8. **View the Final Result**
 * Open public ngrok URL in browser 
 * Go to Dashboards and view the "CPU Usage Dashboard"

### Dashboard Features

The generated dashboard includes **3 different visualizations** of the CPU usage data:

1. **Gauge** (Average Cluster CPU) - Shows the average CPU usage across all servers, with color coded thresholds.


2. **Bar Chart** (CPU per Server) - Provides the current CPU usage for each server.


3. **Time Series** (CPU Trends) - Gives the graph showing three CPU lines for each Server and the cluster average line.

---
Original Readme

# Demo Alerting in Prometheus and Grafana 

Grafana Alerting is built on the Prometheus Alerting model. This demo project showcases the similarities between Prometheus and Grafana alerting systems, covering topics such as:

- Creating alerts in Prometheus
- Recreating the same alerts using Grafana
- Setting up alerts based on Loki logs
- Exploring alerting components like evaluation groups and notification policies
- Creating template notifications
- And more!

This project pairs well with this [Alerting Presentation Template](https://docs.google.com/presentation/d/1XvJnBlNnXUjiS409ABN4NxNkFZoYDmoRKKoJqsvln-g/edit?usp=sharing). Together, they provide an excellent starting point for presenting the Prometheus Alerting model and demonstrating its use in Grafana.

## Run the demo environment

This repository includes a [Docker Compose setup](./docker-compose.yaml) that runs Grafana, Prometheus, Prometheus Alertmanager, Loki, and an SMTP server for testing email notifications.

To run the demo environment:

```bash
docker compose up
```

You can then access:
- Grafana: [http://localhost:3000](http://localhost:3000/)
- Prometheus web UI: [http://localhost:9090](http://localhost:9090/)
- Alertmanager web UI: [http://localhost:9093](http://localhost:9093/)

### Generating test data

This demo uses [Grafana k6](https://grafana.com/docs/k6) to generate test data for Prometheus and Loki.

The [k6 tests in the `testdata` folder](./testdata/) inject Prometheus metrics and Loki logs that you can use to define alert queries and conditions. 

1. Install **k6 v1.2.0** or later.

2. Run a k6 test with the following command:

    ```bash
    k6 run testdata/<FILE>.js
    ```

You can modify and run the k6 scripts to simulate different alert scenarios.
For details on inserting data into Prometheus or Loki, see the `xk6-client-prometheus-remote` and `xk6-loki` APIs.

### Receive webhook notifications

One of the simplest ways to receive alert notifications is by using a Webhook.  You can use [`webhook.site`](https://webhook.site/) to create Webhook URLs and view the incoming messages.

- For Prometheus alertmanager: 
  
  Set the Webhook URL to the [alertmanager.yml](./alertmanager/alertmanager.yml) configuration file.

- For Grafana:
  
  Create a Webhook contact point and assign it to the notification policy.

### Receive mail notifications

You can also configure notifications to be sent via your Gmail account using an [App Password](https://support.google.com/accounts/answer/185833?hl=en). After creating your App password:

- For Prometheus Alertmanager:

  Replace `your_mail@gmail` with your Gmail address in the [alertmanager.yml](./alertmanager/alertmanager.yml) configuration file.

  Copy `alertmanager/smtp_auth_password.example` to `alertmanager/smtp_auth_password` and set your password.

- For Grafana:

  Copy `environments/smpt.env.example` to `environments/smpt.env` and set the appropriate environment variables values.
