# Reform Scan Notification Service

![](https://github.com/hmcts/reform-scan-notification-service/workflows/CI/badge.svg)
[![](https://github.com/hmcts/reform-scan-notification-service/workflows/Publish%20Swagger%20Specs/badge.svg)](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/reform-scan-notification-service.json)
[![codecov](https://codecov.io/gh/hmcts/reform-scan-notification-service/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/reform-scan-notification-service)

## Purpose.

Retrieves service bus queue messages from notifications queue and then processes them by notifying external supplier's API and finally saves results to database in notifications table.
 
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
