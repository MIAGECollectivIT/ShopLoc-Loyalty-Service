# Hexagonal Architecture (Ports and Adapters) - ShopLoc Loyalty Service

This document details the organization, guiding principles, and implementation of the **Hexagonal Architecture** (also known as the *Ports and Adapters* pattern, originally formulated by Alistair Cockburn) within the `ShopLoc-Loyalty-Service` microservice.

---

## 1. Core Principles

The primary objective of hexagonal architecture is to **isolate the core business logic (Domain)** from all technical and infrastructural concerns (frameworks, databases, communication protocols, third-party libraries).

```mermaid
flowchart TB
    subgraph DrivingAdapters["Primary Adapters (Driving / Inbound)"]
        direction TB
        WebCtrl["REST Controllers<br/>MerchantLoyaltyRateController<br/>PointsAccountController"]
        KafkaConsumer["Kafka Consumers<br/>KafkaPhysicalPaymentConsumer (SLE-173)<br/>KafkaTransactionApprovedConsumer"]
    end

    subgraph Core["Hexagon: Core Application and Domain"]
        direction TB
        subgraph PortsIn["Primary Ports (Inbound)"]
            InUseCase["ConfigureMerchantLoyaltyRateUseCase<br/>GetMerchantLoyaltyRateQuery<br/>ProcessPhysicalPaymentLoyaltyUseCase<br/>ConsultPointsBalanceUseCase"]
        end

        subgraph Application["Application Orchestration"]
            AppService["PointsAccountService<br/>(Application Service)"]
        end

        subgraph Domain["Pure Business Domain (POJO)"]
            DomainModel["MerchantLoyaltyRate<br/>UserPointsAccount<br/>UserPointsTransaction"]
            DomainExceptions["StoreAccessDeniedException<br/>StoreNotFoundException<br/>InvalidLoyaltyRateException"]
        end

        subgraph PortsOut["Secondary Ports (Outbound)"]
            PersistPort["MerchantLoyaltyRatePersistencePort"]
            UserPointsPort["UserPointsPersistencePort"]
            StorePort["StoreVerificationPort"]
        end
    end

    subgraph DrivenAdapters["Secondary Adapters (Driven / Outbound)"]
        direction TB
        subgraph Persistence["PostgreSQL Persistence"]
            PersistAdapter["MerchantLoyaltyRatePersistenceAdapter"]
            UserPointsAdapter["UserPointsPersistenceAdapter"]
            SpringDataRepo["SpringDataRepositories"]
            JpaEntities["JPA Entities (Rates, Accounts, Tx)"]
        end
        subgraph HttpClients["Inter-Service HTTP Clients"]
            StoreHttpAdapter["StoreServiceHttpAdapter<br/>(RestClient -> Store Service)"]
        end
    end

    WebCtrl -->|Invokes| InUseCase
    KafkaConsumer -->|Invokes| InUseCase
    InUseCase <|.. AppService
    AppService --> DomainModel
    AppService -->|Calls| PersistPort
    AppService -->|Calls| UserPointsPort
    AppService -->|Calls| StorePort
    PersistAdapter ..|> PersistPort
    UserPointsAdapter ..|> UserPointsPort
    StoreHttpAdapter ..|> StorePort
    PersistAdapter --> SpringDataRepo
    UserPointsAdapter --> SpringDataRepo
    SpringDataRepo --> JpaEntities
```

### The Dependency Inversion Principle
1. **Outer layers depend on inner layers**: Infrastructure knows about the domain, but the domain **never knows about the infrastructure**.
2. **Zero framework annotations in `domain/`**: The domain contains no `@Entity`, `@Table`, `@RestController`, `@Autowired`, nor any references to Spring, Hibernate, Jackson, or Jakarta.
3. **Inversion of control via Ports**: The application service interacts with the external world exclusively through interfaces (*Ports*). Concrete implementations (*Adapters*) are injected at runtime by Spring.

---

## 2. Package Structure

Source code under `src/main/java/fr/miage/collectivit/loyaltyservice/` strictly adheres to the hexagonal layout:

```text
fr.miage.collectivit.loyaltyservice/
├── domain/                                      # 1. PURE BUSINESS CORE
│   ├── model/
│   │   ├── MerchantLoyaltyRate.java             # Rich domain entity (conversion calculations, validation)
│   │   ├── UserPointsAccount.java               # Customer points account balance
│   │   └── UserPointsTransaction.java           # Ledger transaction audit record
│   ├── port/
│   │   ├── in/                                  # Primary ports (exposed use cases / queries)
│   │   │   ├── ConfigureMerchantLoyaltyRateUseCase.java
│   │   │   ├── ConsultPointsBalanceUseCase.java
│   │   │   ├── GetMerchantLoyaltyRateQuery.java
│   │   │   └── ProcessPhysicalPaymentLoyaltyUseCase.java  # SLE-173: Kafka physical payment ingestion port
│   │   └── out/                                 # Secondary ports (contracts required by the domain)
│   │       ├── MerchantLoyaltyRatePersistencePort.java
│   │       ├── StoreVerificationPort.java
│   │       └── UserPointsPersistencePort.java   # Account balance and ledger persistence port
│   └── exception/                               # Pure domain exceptions
│       ├── InvalidLoyaltyRateException.java
│       ├── MerchantLoyaltyRateNotFoundException.java
│       ├── StoreAccessDeniedException.java
│       └── StoreNotFoundException.java
│
├── application/                                 # 2. APPLICATION ORCHESTRATION
│   └── service/
│       └── PointsAccountService.java            # Orchestration service implementing all inbound use cases
│
├── infrastructure/                              # 3. TECHNICAL ADAPTERS
│   └── adapter/
│       ├── in/
│       │   ├── web/                             # Primary Web REST adapters
│       │   │   ├── MerchantLoyaltyRateController.java
│       │   │   ├── PointsAccountController.java
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   └── dto/
│       │   └── event/                           # Primary Event-Driven Kafka adapters
│       │       ├── KafkaPhysicalPaymentConsumer.java      # SLE-173 / SLE-36: shoploc.physical-payments consumer
│       │       └── KafkaTransactionApprovedConsumer.java  # shoploc.transactions.approved consumer
│       └── out/
│           ├── persistence/                     # Secondary JPA persistence adapter
│           │   ├── MerchantLoyaltyRatePersistenceAdapter.java
│           │   ├── UserPointsPersistenceAdapter.java
│           │   ├── entity/
│           │   ├── mapper/
│           │   └── repository/
│           └── rest/                            # Secondary HTTP REST client adapter
│               ├── StoreServiceHttpAdapter.java
│               └── dto/
│
├── config/                                      # 4. INFRASTRUCTURE & BROKER CONFIGURATION
│   ├── KafkaConsumerConfig.java                 # Kafka ConsumerFactory, error handling & type mappings
│   ├── KafkaTopicConfig.java                    # Automated topic declaration via KafkaAdmin
│   └── OpenApiConfig.java                       # Swagger UI OpenAPI 3 configuration
│
└── security/                                    # 5. CROSS-CUTTING SECURITY
    ├── RequiresRole.java                        # Declarative RBAC annotation
    ├── JwtAuthInterceptor.java                  # Spring MVC JWT validation interceptor
    ├── JwtKeyProvider.java                      # RS256 asymmetric and HMAC secret key provider
    ├── JwtValidatorService.java                 # Decoding, verification, and revocation check
    └── TokenRevocationService.java              # Blacklist / token revocation management
```

---

## 3. Role of Each Component

### 3.1. Business Domain (`domain/`)
- **`MerchantLoyaltyRate`**: Domain model representing a merchant's loyalty rate (`storeId`, `euroAmount`, `pointsReward`, `createdAt`, `updatedAt`).
  - Encapsulates integrity rules: a rate is strictly tied to a store, `euroAmount > 0` with up to 2 decimals, and `pointsReward >= 0.5` in steps of 0.5.
  - Provides the core business method `calculatePointsForPurchase(BigDecimal purchaseAmount)` to calculate points earned with 0.5 half-point precision and floor rounding.
  - Standard default rate factory: `MerchantLoyaltyRate.defaultForStore(storeId)` (1.00 € = 1.0 pt).
- **Domain Exceptions**: Pure exceptions independent of HTTP status codes (`StoreNotFoundException`, `StoreAccessDeniedException`, `InvalidLoyaltyRateException`, `MerchantLoyaltyRateNotFoundException`).

### 3.2. Ports (`domain/port/`)
- **Primary Ports (`port/in/`)**:
  - `ConfigureMerchantLoyaltyRateUseCase`: Method `configureRate(Long storeId, BigDecimal euroAmount, BigDecimal pointsReward, Long callerUserId, String callerRole)`.
  - `GetMerchantLoyaltyRateQuery`: Method `getRateByStoreId(Long storeId)`.
- **Secondary Ports (`port/out/`)**:
  - `MerchantLoyaltyRatePersistencePort`: Persistence contract (`save`, `findByStoreId`, `existsByStoreId`, `deleteByStoreId`).
  - `StoreVerificationPort`: Verification contract to validate store existence and merchant ownership (`isStoreOwnedBy(storeId, userId)`, `storeExists(storeId)`).

### 3.3. Application Service (`application/service/`)
- **`PointsAccountService`**:
  - Application core prescribed by enterprise architecture models.
  - Implements `ConfigureMerchantLoyaltyRateUseCase` and `GetMerchantLoyaltyRateQuery`.
  - Enforces BOLA (*Broken Object Level Authorization*) controls:
    - If caller has the `ADMIN` role, ownership verification is bypassed.
    - If caller is a `MERCHANT`, the service queries `StoreVerificationPort` to confirm legitimate store ownership before allowing updates.
  - Calls `MerchantLoyaltyRatePersistencePort` to persist changes.
  - *Future extension*: Will process Kafka order-placed events to credit customer point accounts.

### 3.4. Secondary Adapters (`infrastructure/adapter/out/`)
- **Persistence Adapter (`adapter/out/persistence/`)**:
  - `MerchantLoyaltyRateJpaEntity`: Relational model mapped to the PostgreSQL `merchant_loyalty_rates` table.
  - `MerchantLoyaltyRateMapper`: Spring component isolating the domain from the JPA entity with bidirectional conversion (`toDomain` / `toJpaEntity`).
  - `MerchantLoyaltyRatePersistenceAdapter`: Implements `MerchantLoyaltyRatePersistencePort` delegating to `SpringDataMerchantLoyaltyRateRepository`.
- **HTTP REST Adapter (`adapter/out/rest/`)**:
  - `StoreServiceHttpAdapter`: Implements `StoreVerificationPort`.
  - Uses Spring `RestClient` to query Store Service: `GET /v1/stores/{storeId}`.
  - Converts HTTP responses (404, 200 with owner ID) into boolean checks or domain exceptions.

### 3.5. Primary Web Adapter (`infrastructure/adapter/in/web/`)
- **`MerchantLoyaltyRateController`**:
  - Handles incoming JSON HTTP requests, validates payloads using Jakarta Bean Validation (`@Valid`, `@Positive`, `@Digits`).
  - Extracts caller identity (`authenticatedUserDbId`) and role injected by security interceptor.
  - Invokes `GetMerchantLoyaltyRateQuery` and `ConfigureMerchantLoyaltyRateUseCase`.
  - Maps domain model to `MerchantLoyaltyRateResponse` DTO.
- **`GlobalExceptionHandler`**:
  - Catches domain and validation exceptions, mapping them to standard JSON error responses with appropriate HTTP statuses:
    - `StoreNotFoundException`, `MerchantLoyaltyRateNotFoundException` -> HTTP 404 Not Found.
    - `StoreAccessDeniedException` -> HTTP 403 Forbidden.
    - `InvalidLoyaltyRateException`, `MethodArgumentNotValidException`, `HandlerMethodValidationException` -> HTTP 400 Bad Request.

---

## 4. Sequence Diagram: Configuring a Rate (PUT)

The following diagram illustrates the complete flow for updating a loyalty rate with RBAC security and BOLA protection:

```mermaid
sequenceDiagram
    autonumber
    actor Merchant as Merchant (HTTP Client)
    participant Interceptor as JwtAuthInterceptor
    participant Controller as MerchantLoyaltyRateController
    participant Service as PointsAccountService
    participant StoreAdapter as StoreServiceHttpAdapter
    participant StoreService as Store Service (External)
    participant PersistAdapter as MerchantLoyaltyRatePersistenceAdapter
    participant DB as PostgreSQL (Loyalty DB)

    Merchant->>Interceptor: PUT /v1/loyalty/merchants/10/rates (Bearer JWT)
    Note over Interceptor: Validates JWT signature & role @RequiresRole("MERCHANT", "ADMIN")
    Interceptor->>Controller: Authenticated request (userId=42, role=MERCHANT)
    Controller->>Service: configureRate(storeId=10, 2.50€, 5.0pts, callerUserId=42, role=MERCHANT)
    
    rect rgb(240, 248, 255)
        Note over Service,StoreService: BOLA Verification (Store Ownership Check)
        Service->>StoreAdapter: storeExists(10)
        StoreAdapter->>StoreService: GET /v1/stores/10
        StoreService-->>StoreAdapter: 200 OK (store data)
        StoreAdapter-->>Service: true
        Service->>StoreAdapter: isStoreOwnedBy(10, 42)
        StoreAdapter->>StoreService: GET /v1/stores/10
        StoreService-->>StoreAdapter: 200 OK (owner userId: 42)
        StoreAdapter-->>Service: true (Ownership confirmed)
    end

    rect rgb(245, 255, 245)
        Note over Service,DB: Rate Persistence
        Service->>PersistAdapter: save(MerchantLoyaltyRate)
        PersistAdapter->>PersistAdapter: Mapper toJpaEntity()
        PersistAdapter->>DB: INSERT/UPDATE merchant_loyalty_rates
        DB-->>PersistAdapter: Entity saved
        PersistAdapter->>PersistAdapter: Mapper toDomain()
        PersistAdapter-->>Service: Updated MerchantLoyaltyRate
    end

    Service-->>Controller: MerchantLoyaltyRate
    Controller-->>Merchant: 200 OK (MerchantLoyaltyRateResponse JSON)
```

---

## 5. Sequence Diagram: Physical In-Store Payment & Loyalty Points Ingestion (SLE-36 / SLE-173 / SLE-174 / SLE-175)

The following diagram illustrates the event-driven inbound flow when a physical sale completed event is received from Kafka:

```mermaid
sequenceDiagram
    autonumber
    participant Kafka as Kafka Topic (shoploc.physical-payments)
    participant Consumer as KafkaPhysicalPaymentConsumer
    participant PortIn as ProcessPhysicalPaymentLoyaltyUseCase
    participant Service as PointsAccountService
    participant RatePort as MerchantLoyaltyRatePersistencePort
    participant PointsPort as UserPointsPersistencePort
    participant DB as PostgreSQL (Loyalty DB)

    Kafka->>Consumer: Deliver PhysicalPaymentCompletedEvent
    Note over Consumer: Deserialized with TypeMapping
    Consumer->>PortIn: processPhysicalPayment(event)
    PortIn->>Service: processPhysicalPayment(event)

    Note over Service,DB: 1. Idempotency Check
    Service->>PointsPort: existsTransactionByReference(txRef)
    PointsPort->>DB: SELECT count(*) FROM points_transaction WHERE reference = txRef
    DB-->>PointsPort: 0 (not processed yet)
    PointsPort-->>Service: false

    Note over Service,DB: 2. Merchant Rate Lookup & Math Calculation
    Service->>RatePort: findByStoreId(storeId)
    RatePort->>DB: SELECT * FROM merchant_loyalty_rates WHERE store_id = storeId
    DB-->>RatePort: MerchantLoyaltyRate (euroAmount=1.00, pointsReward=1.0)
    RatePort-->>Service: Optional[MerchantLoyaltyRate]
    Note over Service: Points = amount * (pointsReward / euroAmount)

    Note over Service,DB: 3. Customer Account Resolution & Atomic Ledger Update
    Service->>PointsPort: findAccountByUserId(customerId) / findAccountByClientCode(clientCode)
    PointsPort->>DB: SELECT * FROM points_account WHERE user_id = customerId
    DB-->>PointsPort: UserPointsAccount (currentBalance = 0.0)
    PointsPort-->>Service: UserPointsAccount
    
    Note over Service: Compute new balance = currentBalance + pointsEarned
    Service->>PointsPort: saveAccount(newBalance)
    PointsPort->>DB: UPDATE points_account SET balance = 35.5, updated_at = NOW()
    DB-->>PointsPort: Updated account

    Service->>PointsPort: saveTransaction(UserPointsTransaction: EARN, points=35.5, txRef)
    PointsPort->>DB: INSERT INTO points_transaction (reference, store_id, type='EARN', points=35.5, ...)
    DB-->>PointsPort: Transaction committed
    Service-->>Consumer: Finished successfully
```

---

## 6. Gift Catalog & VFP-Exclusive Gifts (SLE-60 / SLE-39 / SLE-49)

The gift catalog domain follows the same hexagonal isolation:

- **Domain Models (`domain/model/`)**:
  - `LoyaltyGift`: Pure domain aggregate carrying `uid`, `storeUid`, `name`, `description`, `pointsCost`, `stock`, `imageUrl`, `active`, and `vfpExclusive` (defaults to `false`). Validates mandatory fields, positive `pointsCost`, and non-negative `stock`.
  - `CatalogGiftItem`: Read-only domain projection enriched with partner store metadata (`storeName`, `category`) and `vfpExclusive` for public catalog queries.
- **Inbound Ports & Application Service**:
  - `GiftCreateCommand` / `GiftUpdateCommand` propagate the `vfpExclusive` flag from the web layer into `GiftCatalogService` while keeping backward-compatible constructors (`vfpExclusive = false`).
  - `GiftCatalogService` enforces BOLA store-ownership checks (`MERCHANT` vs `ADMIN`) and delegates persistence to `GiftPersistencePort`.
- **Web & Persistence Adapters**:
  - `GiftCreateRequest`, `GiftUpdateRequest`, and `GiftResponse` expose `vfpExclusive` in the REST/OpenAPI contract (`/v1/loyalty/stores/{storeUid}/gifts` and `/v1/loyalty/gifts`).
  - `LoyaltyGiftJpaEntity` and `LoyaltyGiftMapper` map `vfpExclusive` to the PostgreSQL `vfp_exclusive` column introduced by Flyway migration `V11__add_vfp_exclusive_to_gift_catalog.sql` (indexed via `idx_loyalty_gifts_vfp_exclusive`).

---

## 7. Architectural Benefits

1. **Fast, Isolated Testing**:
   - Domain business rules (`MerchantLoyaltyRateTest`) execute purely in-memory in under 50 ms without loading Spring or external containers.
   - Application service (`PointsAccountServiceTest`) is tested in isolation with Mockito mocks for `StoreVerificationPort`, `MerchantLoyaltyRatePersistencePort`, and `UserPointsPersistencePort`.
   - Web adapters (`MerchantLoyaltyRateControllerIntegrationTest`, `PointsAccountControllerIntegrationTest`) and REST adapter (`StoreServiceHttpAdapterTest`) are verified with targeted integration tests.
   - Event-driven Kafka consumers (`PhysicalPaymentKafkaIntegrationTest`) run against real embedded brokers with full message lifecycle assertion.
2. **Infrastructure Independence**:
   - Swapping persistence to another store or using Redis/Kafka for store verification only requires changing secondary adapters; domain models and controllers remain completely untouched.
3. **Security by Design**:
   - Strict use-case orchestration guarantees that no merchant can modify a competitor's rate without passing through BOLA checks in the application service.
4. **Resilience & Asynchronous Decoupling**:
   - Decoupled event consumption ensures that high-volume in-store checkout lines are never blocked or throttled by loyalty ledger operations.
