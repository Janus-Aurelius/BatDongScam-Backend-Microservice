# Post-Merge Gap Analysis Report: `search-service`

## Executive Summary
A comprehensive architecture audit and gap analysis was conducted on the newly migrated **`search-service`** to evaluate its alignment with the **Legacy Monolith (`/legacy`)**, integration with the **Core Macroservice (`/bds-core-macroservice`)**, and adherence to the **Shared Contracts (`/bds-common`)**.

### Health Check Status: 🔴 CRITICAL GAPS / INCOMPLETE SYNCHRONIZATION
The target service `search-service` has been initialized as a Spring Boot application using MongoDB, containing basic models, repositories, and a REST controller. However, **it is currently completely isolated** and fails to integrate with the rest of the microservices system:
1. **Critical business logic is missing:** The statistics generation scheduler from legacy has been omitted, and there is no runtime mechanism to aggregate search logs into statistics reports.
2. **Core integration touchpoints are broken:** `bds-core-macroservice` has no connection to `search-service`, causing total regression of search logging, popular property listings (Top-K popularity), and popular city queries.
3. **Contract standardization is violated:** The service uses duplicate local response wrappers rather than `bds-common` contracts, fails to register as a module in the root POM, and deviates from package naming conventions.
4. **Communication is un-architected:** No asynchronous messaging is configured for search logging or statistics updates, creating high performance and availability risks if synchronous calls are introduced.

---

## 1. Missing Features & Business Logic Gaps (Legacy vs. New)
Comparing the new service against the legacy codebase reveals significant logical omissions and regressions:

* **Omission of the Statistics Scheduler:**
  The legacy scheduler [PropertyStatisticsReportScheduler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/report/scheduler/PropertyStatisticsReportScheduler.java) was not migrated. This scheduler is responsible for:
  * Creating and initializing the [PropertyStatisticsReport](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/search-service/src/main/java/com/se100/bds/searchservice/models/schemas/report/PropertyStatisticsReport.java) document at the start of each month.
  * Ensuring the statistics report map keys (cities, districts, wards, property types) are pre-populated and kept in sync with active entities in the system.
  * *Status:* **Completely missing in `search-service`.**

* **Absence of Real-Time Aggregation Logic:**
  In the legacy monolith, search counts and popular metrics in `PropertyStatisticsReport` were only populated via [SearchLogDummyData.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/data/domains/SearchLogDummyData.java). Neither legacy nor the new `search-service` implementation has runtime logic to aggregate individual `SearchLog` documents into `PropertyStatisticsReport` counters. 
  * *Status:* **Design gap inherited.** Live search logs will accumulate in MongoDB, but popular queries/Top-K filters will return outdated or empty results because counts are never updated.

* **Missing Entity Metrics Synchronization:**
  The legacy statistics report tracked cumulative metrics like `totalActiveProperties`, `totalSoldProperties`, and `totalRentedProperties`. There is no mechanism in `search-service` to observe property lifecycle transitions (e.g., when a property is approved, sold, or rented in `bds-core-macroservice`) to update these numbers.
  * *Status:* **Functional regression.**

---

## 2. Integration Gaps (Core Macroservice)
The core macroservice (`bds-core-macroservice`) and `search-service` need to function cohesively. Currently, all touchpoints are completely disconnected:

* **Missing Search Logging Trigger:**
  * *Legacy Behavior:* When a user filtered properties in the landing page, [PropertyServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/legacy/src/main/java/com/se100/bds/services/domains/property/impl/PropertyServiceImpl.java#L133) triggered search logging asynchronously via `searchService.addSearchList(...)`.
  * *New Behavior:* The migrated [PropertyServiceImpl.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/service/PropertyServiceImpl.java#L297) does not make any call to log search metrics. 
  * *Gap:* Searches are never logged.

* **Broken Top-K Popularity Search:**
  * *Legacy Behavior:* When the `topK` flag was true during property searches, `PropertyServiceImpl` fetched popular property IDs via `searchService.getMostSearchedPropertyIds(...)` and applied them as a database query filter.
  * *New Behavior:* The new [SearchPropertyCommand](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/command/SearchPropertyCommand.java) and [JpaPropertyRepository](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/adapter/out/persistence/JpaPropertyRepository.java#L42) completely omit the `topK` filter, and have no way to receive popular property IDs.
  * *Gap:* Popularity-based search filtering is completely broken.

* **Missing "Top Cities" Endpoint:**
  * *Legacy Behavior:* `PublicController.java` exposed `GET /public/locations/cities/top` which called `locationService.topMostSearchedCities(pageable)` (delegated to search service's `topMostSearchByUser`).
  * *New Behavior:* The new [LocationController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/adapter/in/web/LocationController.java) does not contain this endpoint.
  * *Gap:* The front-end cannot query the most popular cities.

* **Data Access Gaps for Statistics Report:**
  To populate/initialize statistics maps (e.g. valid ward, district, city, and property type IDs), the `search-service` scheduler needs to query location metadata. In a microservices architecture, it cannot query the relational database of `bds-core-macroservice` directly.
  * *Gap:* No communication mechanism exists to fetch valid IDs from the core macroservice.

---

## 3. Contract Violations (bds-common)
The `search-service` does not adhere to the shared contract standards:

* **Duplicated Response Wrappers (Local DTOs):**
  Instead of utilizing the standard envelope response `ApiResponse<T>` from [bds-common](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/bds-common/src/main/java/com/se/bds/common/dto/ApiResponse.java), `search-service` duplicates and defines local custom response structures under `com.se100.bds.searchservice.dtos.responses`:
  * `AbstractBaseResponse.java`
  * `SingleResponse.java`
  * `ErrorResponse.java`
  * `DetailedErrorResponse.java`
  * Consequently, the controller [SearchController.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/search-service/src/main/java/com/se100/bds/searchservice/controllers/SearchController.java) and exception handler [AppExceptionHandler.java](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/search-service/src/main/java/com/se100/bds/searchservice/exceptions/AppExceptionHandler.java) do not match the system's global JSON contract.

* **Maven Structure & POM Violations:**
  * **No parent linkage:** The `search-service` [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/search-service/pom.xml) inherits directly from Spring Boot parent rather than the root POM `batdongsan-platform`. This causes version mismatches (e.g., using Java 17 instead of Java 21).
  * **Not registered in root POM:** The root [pom.xml](file:///home/annguyen/master_projects/sem2_year3_projects/BatDongSan/BatDongScam-Backend-Microservice/pom.xml) does not declare `<module>search-service</module>`.
  * **No dependency on `bds-common`:** The service is completely decoupled from the shared library and cannot access its utilities or event definitions.

* **Naming Conventions:**
  * The packages in `search-service` are prefixed with `com.se100.bds.searchservice` while all other components use `com.se.bds` prefix.

---

## 4. Inter-Service Communication Risks
The lack of architectural communication boundaries poses serious resilience risks:

* **Synchronous Search Logging Anti-Pattern:**
  Logging searches is a non-blocking write operation that should be decoupled from the user's primary request. Calling search service endpoints (`/api/search/log`) synchronously via HTTP from `bds-core-macroservice` would add overhead to property queries and risk throwing errors if the search service is unavailable.
  * *Correction:* Search logging should be event-driven. `bds-core-macroservice` should publish a `PropertySearchedEvent` to Kafka, which `search-service` consumes asynchronously.

* **Lack of Resilience in Synchronous Queries:**
  Querying popular cities and popular properties are read operations that must happen synchronously from `bds-core-macroservice` to `search-service`. 
  * *Risk:* There are no circuit breakers (Resilience4j), retries, or timeout configurations defined. If `search-service` fails, it could cascade and crash the property search flow in the core service.

* **Lack of Message Broker Integration:**
  `search-service` has no Kafka dependencies or listener configuration, making it unable to process asynchronous events.

---

## 5. Actionable Remediation Steps

### Phase 1: Build & Contract Standardization (High Priority)
- [ ] **Align POM configurations:**
  - Update `search-service/pom.xml` to use the parent POM `batdongsan-platform` (version `0.0.1-SNAPSHOT`).
  - Upgrade Java version to `21`.
  - Add `search-service` module to root `pom.xml`.
- [ ] **Add `bds-common` dependency:**
  - Add the `bds-common` dependency to `search-service/pom.xml`.
- [ ] **Refactor API Contract Envelopes:**
  - Remove custom response wrappers (`AbstractBaseResponse`, `SingleResponse`, `ErrorResponse`, `DetailedErrorResponse`) from the `search-service` package.
  - Refactor `SearchController` and `AppExceptionHandler` to use `com.se.bds.common.dto.ApiResponse`.
- [ ] **Align package naming structure:**
  - Rename package prefix from `com.se100.bds.searchservice` to `com.se.bds.search`.

### Phase 2: Asynchronous & Event-Driven Communication Setup (High Priority)
- [ ] **Add Kafka messaging dependency:**
  - Add `spring-kafka` dependency to `search-service/pom.xml`.
- [ ] **Implement Kafka events in `bds-common`:**
  - Define `PropertySearchedEvent` containing search criteria (user ID, city IDs, district IDs, ward IDs, property type IDs, property ID).
- [ ] **Integrate asynchronous logging in core macroservice:**
  - In `bds-core-macroservice`'s `PropertyServiceImpl.searchProperties`, publish a `PropertySearchedEvent` to Kafka whenever a search is executed.
- [ ] **Add Kafka Consumer in `search-service`:**
  - Implement a consumer class in `search-service` that listens for `PropertySearchedEvent` and processes logs asynchronously.
- [ ] **Sync Property Status Changes:**
  - Make `search-service` consume `PropertyStatusChangedEvent` from Kafka to increment/decrement `totalActiveProperties`, `totalSoldProperties`, and `totalRentedProperties` dynamically.

### Phase 3: Business Logic Parity & Schedulers (Medium Priority)
- [ ] **Re-implement Statistics Scheduler:**
  - Create a scheduler class in `search-service` equivalent to the legacy `PropertyStatisticsReportScheduler`.
  - Expose internal REST endpoints in `bds-core-macroservice` to allow `search-service` to query valid location/property-type IDs, and call them from the scheduler.
- [ ] **Implement Real-time / Scheduled Aggregation:**
  - Add an aggregation pipeline or incremental update mechanism to update counts inside `PropertyStatisticsReport` dynamically during search logging.

### Phase 4: Core Macroservice Integration & Resilience (Medium Priority)
- [ ] **Define Feign Clients in `bds-core-macroservice`:**
  - Create a Feign Client interface `SearchServiceClient` pointing to `search-service`.
  - Expose methods: `getMostSearchedPropertyIds(limit, year, month)` and `topMostSearchByUser(...)`.
- [ ] **Apply Resilience Configurations:**
  - Configure Resilience4j Circuit Breakers, Retries, and Timeouts on the Feign Client methods.
- [ ] **Restore Core Functionality:**
  - Re-integrate `topK` popularity filtering in core service's `PropertyServiceImpl` and `JpaPropertyRepository`.
  - Re-implement `/public/locations/cities/top` in `bds-core-macroservice`'s `LocationController` using the Feign Client.
