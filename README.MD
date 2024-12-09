# Currency Application

Currency Application is a Spring Boot based application for managing and analyzing currency data.
It provides APIs for handling currencies, statistics, and other related operations.

## Features

- **CRUD Operations**: Create, read, update, and delete currency data.
- **Statistics**: Analyze and retrieve statistic data for currency.
- **Normalization**: Get normalized prices for currency for specific periods.
- **Swagger Documentation**: Automatically generated OpenAPI documentation for all endpoints.
- **Javadoc Documentation**: Automatically generated Javadoc documentation for all classes & interfaces for detailed
  review

## Architecture

The **Cryptos Application** is designed for scalability and portability:

- **Docker**: The application is packaged in Docker images, allowing easy deployment. Docker ensures that the
  application runs the same among all environments, on development and production.

- **Kubernetes**: The application is deployed using Kubernetes, providing orchestration, scaling, and management of
  Docker containers. Kubernetes ensures efficient resource allocation and high availability, automatically scaling the
  application on demand.

- **Database**: The application connects to a relational database (PostgreSQL), storing currency data and statistics.
  Kubernetes helps manage the database's containerized instance as well, ensuring proper resource management.

- **Code structure**: The project uses a simple three-layer architecture consisting of Persistence, Service, and
  API/Controllers layers. The Persistence layer handles data storage and retrieval, the Service layer contains the
  business logic, and the API/Controllers layer manages client requests and responses, ensuring separation of concerns
  and maintainability.

## Requirements

- Java 21+, tested on Amazon Coretto 21.0.4
- PostgreSQL (or any compatible database). Tested on 17.2 version
- Docker. Tested on 27.3.1 version, optional for local run.
- Kubernetes, optional for local run.

## Setup and Run

### Database local run

To launch the application instance, you need to start the database, as all the data is stored there.
If Kubernetes is not used, perform one of the following steps.

- Run the command to launch the database using docker-compose tool:\
  `docker-compose -f ./compose/docker-compose.yml up -d`
- Or simple docker command\
  `docker run --rm --name db -p 5432:5432 -e POSTGRES_PASSWORD=postgres_password -v db-vol:/var/lib/postgresql/data   postgres:alpine`
- Or run a local database instance on your PC and specify the credentials in the `application.yaml` file.

Once DB started you are ready to build & run the app.

### App local run using Gradle wrapper

- As per the project uses Gradle wrapper, navigate to project root and build the application:

  `./gradlew build`\
  Or if Windows user:\
  `./gradlew.bat build`
- Run the next command to populate some data in `currency` table.
  This profile inserts several currencies for testing: BTC, DOGE, ETH, LTC, XRP.
  Remember that this **profile should be used only once** on a separate instance, as it inserts data.
  After the database initialization, this profile should be disabled.\
  `./gradlew bootRun --args='--spring.profiles.active=local-init'`
- After the successful loading and initialization of the database, use the following command for multiple application
  launches:\
  `./gradlew bootRun`

### App local run using Kubectl & Minikube

- First you need to make Docker work in Minikube environment:\
  `eval $(minikube docker-env)`
- Assuming you have already built the application artifacts using the Gradle wrapper, build the Docker image :\
  `docker build . -t crypto/currencies:1` **remember the tag you used**.
- Using the tag you used, open the file `./kube/currencies/currencies-app-deployment.yml` and paste your tag
  in `spec.containers.image` section.
- Run the command to pre-create crucial Kubernetes resources like namespace, storage class and use the specific
  namespace:\
  ` kubectl apply -f ./kube/space2.yml && kubectl apply -f ./kube/storage-class.yml && kubectl config set-context --current --namespace=space2`
- Then create secret for PostgreSQL database:\
  `kubectl create secret generic currencies-db-secret --from-literal=POSTGRES_PASSWORD="postgres_password"`
- Run the command to start and initialize the database and application. \
  `kubectl apply -f ./kube -R`\
  Keep in mind that tables will be created and the `currencies` table will be populated. See
  the `currencies-db-configmap.yml` file for more details.\
- The next command makes the `currencies-app-service` accessible from outside Minikube:\
  `minikube service currencies-app-service -n space2`\
  This command will print the address and port where the application will be accessible. Remember the IP or visit it to
  access the application. Note that the application does not have a root resource, so append `/swagger-ui/index.html` to
  the end of the URL

## JavaDoc

Run the command `gradlew javadoc` to generate the JavaDoc, then open the file in your browser to view the main page
at `./build/docs/javadoc/index.html`

## API endpoints

### Endpoins list

The application has 5 main endpoints for working with currencies. Please refer to Swagger UI to get full specification
and error codes.

- POST `/currencies`\
  Creates a new currency.\
  Request body example: `{"symbol": "USD"}`\
  Response: Status 201 Created.
- GET `/currencies/{name}`\
  Retrieves information about a specific currency by name.\
  Path parameter example: `/currencies/USD`\
  Response: `{"symbol": "BTC"}`
- GET `/currencies`\
  Retrieves a list of all currencies.\
  Response example: `[{"symbol": "BTC"},{"symbol": "DOGE"},`
- POST `/currencies/stats`\
  Upload a file with currency statistics
  Request body CSV example:
  `timestamp,symbol,price`\
  `1641013200000,DOGE,0.1702`\
  `1641074400000,DOGE,0.1722`\
  `1641078000000,DOGE,0.1727`\
  Response: Status 201 Created.
- GET `/currencies/stats/{name}`\
  Retrieves statistics for a specific currency.\
  Path parameter example: `/currencies/USD`\
  Query parameters: `startDateTime`, `endDateTime` (optional). Example:
  `/currencies/USD?startDateTime=2022-01-02T20:00:01&endDateTime=2022-02-02T10:00:00`\
  Response
  example:
  `{"symbol": "BTC","oldestDate": "2022-01-03T02:00:00","newestDate": "2022-02-01T00:00:00","minPrice": 33276.5900000,"maxPrice": 47336.9800000}`
- GET `/currencies/stats/`\
  Get all currencies with normalized prices.\
  Query parameters: `startDateTime`, `endDateTime` (optional). Example:
  `/currencies/stats?startDateTime=2022-01-02T20:00:01&endDateTime=2022-02-02T10:00:00`\
  Response example: `[{"symbol": "BTC", "normalizedPrice": 0.42253097447785365027}]`
- GET `/currencies/stats/highest`\
  Retrieves the cryptocurrency with the highest normalized price for a given period.
  Query parameter: day (optional). Example: `/currencies/stats/highest?day=2022-01-02`\
  Response example: `{"symbol": "BTC", "normalizedPrice": 0.08 }`

### Swagger UI

The Swagger API documentation is available at the following URL:\
http://localhost:8080/swagger-ui/index.html

Swagger provides an interactive interface to explore and test the API endpoints of the application.
Make sure the application is running before accessing the Swagger documentation.

## Info

### Tools

#### Jacoco test report

Code coverage is measured using Jacoco during test execution. After running the tests with the command:\
`./gradlew clean test`\
The generated coverage reports can be found in the ./build/reports directory.

#### Insomnia HTTP Client collection

Insomnia collection for testing the API is provided and located in `./insomnia` folder. It includes predefined requests
to help you test the endpoints easily.

### Planned Improvements

- Implement HATEOAS for endpoints. Add HATEOAS support to API endpoints for better discoverability and flexibility with
  hyperlinked responses.

- Integrate Actuator and Kubernetes Life Probe. Connect Spring Boot Actuator to Kubernetes life probe for enhanced
  monitoring and health checks.

- Add support for multi-instance Application and Database. Run multiple application instances and databases to improve
  scalability and availability.

- Apply pageable approach for large data. Use pageable support to improve handle large datasets and improve performance.

- Optimize Docker image size with Dive & JLink, reduce Docker image size using Dive for layer analysis and JLink for
  creating a minimal Java runtime.
- Add Helm for deployment management. Migrate to Helm for managing deployments will optimize the deployment process,
  enhance maintainability.
- Introduce Kubernetes ingress will provide a unified approach to manage external access to services, simplify routing
  configurations, and improve scalability and flexibility.
- Enhance security by implementing IP-based restrictions will help to prevent unauthorized access and reduce the risk of
  malicious activities.

### Known Issues

- Separate execution of itegration and unit Tests. Integration tests should be run separately from unit tests for better
  clarity and performance.

- Integration tests not included in Jacoco report. Integration tests are not currently included in the Jacoco coverage
  report.

- Timezone conversion issue. There is a problem with timezone conversion, all dates need to be unified to a single
  timezone.
  This issue affects CSV files in integration tests. The corresponding test, getHighestNormalizedPriceForDay() is
  disabled and incomplete.