# BatDongScam Microservices Platform: Security & Runner Guide

This guide details the security configurations, fail-fast environment checks, asymmetric key transition, and container boot-ordering mechanisms introduced to the BatDongScam microservices architecture.

---

## 1. Fail-Fast Environment Validation

To prevent "ghost bugs" during local development and deployment (where services boot but fail silently or behave insecurely due to missing configurations), we have implemented strict **fail-fast startup checks** in the gateway and identity provider.

### The Behavior
If the application is started without a `.env` file or if the configurations match the default insecure values:
- The Spring Application Context will fail to initialize.
- A clear, descriptive `IllegalStateException` is thrown, halting the JVM:
  ```
  java.lang.IllegalStateException: CRITICAL CONFIGURATION ERROR: APP_SECRET is missing or using the insecure default fallback! Please copy '.env.example' to '.env' and set custom values before starting the application.
  ```

### How to Fix
Always ensure you have copied the environment template and loaded the variables before starting:
```bash
cp .env.example .env
# Fill in your custom secret keys in .env
```

---

## 2. Asymmetric JWT Transition (RS256)

To eliminate symmetric key sharing (where both the gateway and IAM service required the exact same `APP_SECRET` to sign/verify), the system has transitioned to **RS256 asymmetric signatures**.

```
  ┌─────────────────┐                 ┌───────────────┐
  │   iam-service   │                 │  api-gateway  │
  └────────┬────────┘                 └───────┬───────┘
           │                                  │
           │ Expose JWKS                      │
           │ (Public Key Modulus & Exponent)  │
           │─────────────────────────────────>│ (Queries & caches public key)
           │                                  │
           │                                  │ [Verifies client Bearer JWT
           │                                  │  using cached Public Key]
```

### Key Management Lifecycle
1. **Signing (Private Key):** The `bds-iam-service` signs JWTs using an RSA Private Key.
   - **Production:** Must be loaded from `.env` via the `APP_PRIVATE_KEY` variable as a PKCS#8 PEM-encoded string.
   - **Local / Dev / Test:** If `APP_PRIVATE_KEY` is not provided, the IAM service will automatically generate a new 2048-bit RSA KeyPair in-memory on startup. It will print a warning:
     ```
     [WARN] app.private-key is empty. Generating a temporary in-memory RSA key pair. Tokens will be invalidated upon application restart!
     ```
2. **Verification (Public Key):** The public key is derived automatically from the private key (using Java's `RSAPrivateCrtKey` interface to reconstruct the exponent and modulus) in the IAM service.
3. **Strict Production Enforcement:** If the active profile is **not** local/dev/test (e.g. `prod`), the IAM service will throw an exception if `APP_PRIVATE_KEY` is empty, refusing to start with insecure fallbacks.

---

## 3. Token Validation & JWKS Endpoint

Instead of hardcoding or copying public keys to the gateway, the system uses a **JSON Web Key Set (JWKS)** endpoint.

### JWKS Endpoint
- **URL:** `/api/auth/jwks` (mapped in `AuthController` under `bds-iam-service`).
- **Security:** Publicly accessible (permitted in `SecurityConfig.java`).
- **Format:**
  ```json
  {
    "keys": [
      {
        "kty": "RSA",
        "use": "sig",
        "alg": "RS256",
        "kid": "bds-key-id",
        "n": "<modulus-base64url>",
        "e": "<exponent-base64url>"
      }
    ]
  }
  ```

### Gateway Reactive Verification & Caching
The API Gateway's `JwtAuthenticationFilter` resolves tokens using this endpoint reactively:
1. **Non-blocking Request:** Uses Spring WebFlux's non-blocking `WebClient` to fetch the JWKS from `app.jwks-uri`.
2. **Reconstruction:** Converts Modulus `n` and Exponent `e` back to a Java `PublicKey` using standard `RSAPublicKeySpec` and `KeyFactory`.
3. **Caching:** Caches the public key in a `volatile` memory field.
4. **Key Rotation & Resilience:** If a request fails with a `SignatureException` (indicating the IAM service restarted and regenerated a key), the gateway **invalidates the cached key, fetches the new key from JWKS, and retries the signature verification once** before denying the request.

---

## 4. Container Boot Sequencing & Resilience

A Spring Boot container can take 10–20 seconds to boot. To prevent dependent services from throwing connection errors at startup, we enforce strict **boot sequencing**.

### Actuator Probes
Actuator is enabled in the gateway (`bds-api-gateway/src/main/resources/application.yaml`) to expose health probes:
```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
```
This exposes `/actuator/health/liveness` and `/actuator/health/readiness`.

### Healthcheck Declarations in Docker Compose
The `docker-compose.yml` configures healthchecks for core infrastructure:
- **Postgres:** Uses `pg_isready` to verify database readiness.
- **MongoDB:** Evaluates `db.adminCommand('ping')` using `mongosh`.
- **Kafka:** Employs `cub kafka-ready` (Confluent tool) to check broker status.
- **Eureka Server:** Evaluates actuator endpoint:
  ```yaml
  test: ["CMD-SHELL", "curl -f http://localhost:8761/actuator/health || wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1"]
  ```

### Boot Sequencing Rules
Every microservice's `depends_on` configuration has been upgraded from `condition: service_started` to `condition: service_healthy`:
```yaml
  api-gateway:
    depends_on:
      builder:
        condition: service_completed_successfully
      eureka-server:
        condition: service_healthy
```
This guarantees that Eureka, Postgres, MongoDB, and Kafka are fully initialized and listening before microservice JVMs begin booting.
