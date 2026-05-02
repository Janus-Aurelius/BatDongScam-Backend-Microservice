# Architectural Analysis: Spring Boot vs. Plain Java MVC

## Overview

Although the objective was to explore a plain Java MVC codebase lacking Spring's auto-configuration and IoC, evidence from the `BatDongScam-Backend-Microservice` repository heavily indicates this is a fully-fledged **Spring Boot** application. 

Here is the concrete evidence from the workspace:
1. **`pom.xml`**: Inherits from `spring-boot-starter-parent` and explicitly declares dependencies like `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, and `spring-boot-starter-security`.
2. **Controllers**: Files like `AccountController.java` use standard Spring annotations (`@RestController`, `@RequestMapping`, `@GetMapping`). 
3. **Data Access**: Files like `UserRepository.java` extend Spring Data JPA interfaces (`JpaRepository`, `JpaSpecificationExecutor`).

To bridge your knowledge gap, this document contrasts the patterns currently used in your Spring Boot repository with how they would be manually implemented in a "Vanilla" Java EE (Jakarta EE) codebase.

---

## 1. The Request Lifecycle (MVC Flow)

### In your codebase (Spring Boot):
Every HTTP request goes through the standard Spring Boot Web flow. Spring Boot automatically configures a `DispatcherServlet` (the structural backbone of Spring Web MVC), which acts as a **Front Controller**. 

In `AccountController.java`, annotations like `@GetMapping("/me")` and `@PatchMapping("/{userId}")` map URLs to specific controller methods natively. Views are not explicitly rendered; the `@RestController` annotation ensures that returned objects automatically serialize into JSON using `HttpMessageConverters` (e.g., Jackson).

### In standard Plain Java MVC:
Without Spring, you would handle this via standard `HttpServlet` instances.
- **Routing**: You configure an XML file called `WEB-INF/web.xml`, mapping specific URL patterns directly to classes extending `HttpServlet`. Alternatively, in newer Servlets, you use `@WebServlet("/account/me")`.
- **Front Controller**: To avoid hundreds of servlets, "Classic" Java projects often manually implement the **Front Controller pattern**. A master `DispatcherServlet` inspects the request URI and delegates logic to basic Java interfaces (e.g., `Action` or `Handler` classes).
- **View Resolution**: Requests are forwarded to a JavaServer Page (JSP) using logic like `request.getRequestDispatcher("/views/account.jsp").forward(request, response)`.

---

## 2. Dependency Management

### In your codebase (Spring Boot):
Dependency injection is handled entirely by Spring’s `ApplicationContext` (the IoC container). Look at `AccountController.java`, which uses Lombok's `@RequiredArgsConstructor` to generate a constructor that accepts `UserService` and `UserMapper`. At runtime, Spring automatically resolves these dependencies and passes them into the controller during instantiation.

### In standard Plain Java MVC:
Without an IoC container, you manage dependencies manually.
- **Factory Pattern**: You would create a `ServiceFactory` that instantiates classes like `new UserServiceImpl(new UserRepositoryImpl())`.
- **Singleton Pattern**: The `ServiceFactory` (or the services themselves) enforces the Singleton pattern manually using `getInstance()` static methods with thread-safe locking mechanisms. 
- **Service Locator Pattern**: Lacking `@Autowired` or automatic constructor injection, you usually rely on a `ServiceLocator`: `UserService service = ServiceLocator.getService(UserService.class);`.

---

## 3. Data Access & State

### In your codebase (Spring Boot):
You rely heavily on **Spring Data JPA** for the persistence layer. 

In `UserRepository.java`, the class simply extends `JpaRepository<User, UUID>`. Operations are handled dynamically at runtime by Spring's proxies. Complex database joins are executed efficiently via `@EntityGraph(attributePaths = {"ward", "ward.district"})` to prevent N+1 select problems, and custom JPQL strings are housed inside `@Query` annotations.

### In standard Plain Java MVC:
Without Spring Data, Hibernate auto-configuration, or JPA, you manage the database interface directly.
- **Data Access Objects (DAOs)**: You write concrete classes implementing generic DAO interfaces. 
- **Raw JDBC**: Inside DAO implementations, you instantiate a `java.sql.Connection`, manually write SQL queries (`SELECT * FROM users WHERE email = ?`), construct `PreparedStatement` objects, map values to entity parameters via a `ResultSet`, and handle `SQLExceptions`.
- **Connection Pools & Transactions**: Instead of relying on `@Transactional`, you acquire connections from a pool (like HikariCP) and manage boundaries directly with `connection.setAutoCommit(false)`, `connection.commit()`, and `connection.rollback()`.

---

## 4. Summary of Architectural Comparisons

| Component | Spring Boot (Current System) | Plain Java MVC Equivalent |
| :--- | :--- | :--- |
| **Routing / Dispatch** | Spring `DispatcherServlet` | Custom **Front Controller** tracing mappings from `web.xml` |
| **Object Instantiation** | Spring Beans / **IoC Container** | Manual **Singleton** implementations & **Factory** classes |
| **Response Rendering** | `@RestController` (Jackson serialization) | Manual `HttpServletResponse.getWriter().write()` mapping |
| **Data Persistence** | Spring Data JPA Repositories | Manual **DAO** implementations executing **Raw JDBC** queries |

---

## 5. Notable GoF Patterns found in typical architectures

The codebase effectively leverages Gang of Four (GoF) patterns internally via the framework:
*   **Front Controller**: Handled transparently by Spring's `DispatcherServlet`.
*   **Proxy Pattern**: Prominent in `AccountController.java` with `@PreAuthorize("hasRole('ADMIN')")`. Spring wraps the controller in a dynamic proxy that checks authorization *before* invoking the method. JPA repositories and transactional bounds use proxies extensively underneath.
*   **Builder Pattern**: Used alongside Lombok `@Builder` annotations to cleanly assemble object state (e.g., standard Data entities in your application model).
*   **Strategy Pattern**: Evident in features like `JwtAuthenticationFilter`. Filter chains rely heavily on interface strategies to evaluate, map, and process incoming auth tokens cleanly outside core MVC logic.
