# Reform Scan Notification Service

![](https://github.com/hmcts/reform-scan-notification-service/workflows/CI/badge.svg)
[![](https://github.com/hmcts/reform-scan-notification-service/workflows/Publish%20Swagger%20Specs/badge.svg)](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/reform-scan-notification-service.json)

## Purpose

Retrieves service bus queue messages from notifications queue and then processes them by notifying external supplier's API and finally saves results to database in notifications table.

## Features
  - Retrieves messages from service bus queue
  - Notifies external supplier via API
  - Persists results in notifications table
  - Health endpoint for monitoring application status

## Getting Started
### Prerequisites

- [JDK 21](https://www.oracle.com/java)
- Project requires Spring Boot v3.x to be present

## Quick Start
An alternative faster way getting started is by using the automated setup script. This script will help set up all
bulk scan/print repos including reform-scan-notification-service and its dependencies.
See [common-dev-env-bsbp](https://github.com/hmcts/common-dev-env-bsbp) repository for more information.

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/reform-scan-notification-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `8585` in this app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8585/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

