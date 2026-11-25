# Registre des Décisions d'Architecture & Optimisations — ShopLoc-Loyalty-Service

Ce document consigne l'ensemble des optimisations architecturales, techniques et de performance implémentées sur **`ShopLoc-Loyalty-Service`** (notamment dans le cadre des stories **SLE-266** et **SLE-270**), formalisées selon les standards d'architecture d'entreprise (**TOGAF Standard / Architecture Principles**, **AWS Well-Architected Framework**, **12-Factor App** et référentiels **OWASP / ISO/IEC 25010 / EIP**).

---

## Sommaire des Optimisations

| Réf. | Optimisation | Domaine Architectural | Référence Cadre / TOGAF | Statut |
|---|---|---|---|---|
| **OPT-01** | Traitement Kafka Concurrent, DLT & Backoff Exponentiel (`KafkaConsumerConfig`) | Intégration Événementielle & Résilience | TOGAF Principle: *Business Continuity & Reliability* / EIP (*Dead Letter Channel*) | ✅ Appliqué & Vérifié |
| **OPT-02** | Pagination Keyset & Métriques Micrometer (`VfpDailyRevisionJob`) | Scalabilité Batch & Observabilité | TOGAF Principle: *Resource Optimization & Operational Visibility* | ✅ Appliqué & Vérifié |
| **OPT-03** | Verrous Pessimistes & Idempotence sur les Points (`PointsAccountService`) | Intégrité des Données | TOGAF Principle 5: *Data is an Asset & Transactional Integrity (ACID)* | ✅ Appliqué & Vérifié |
| **OPT-04** | Optimisation du Catalogue Public (`GiftCatalogService` & `StoreVerificationPort`) | Performance & Pool HikariCP | TOGAF Principle: *Resource Optimization & Scalability* / Well-Architected (*Performance Efficiency*) | ✅ Appliqué & Vérifié |
| **OPT-05** | Uniformisation des Clients HTTP en HTTP/1.1 avec Pooling (`JdkClientHttpRequestFactory`) | Performance Réseau & Résilience | TOGAF Principle: *Technology Standardization & Resource Optimization* | ✅ Appliqué & Vérifié |
| **OPT-06** | Cache JWT M2M & Rafraîchissement Planifié des Révocations (`TokenRevocationService` / `ServiceTokenProvider`) | Performance & Sécurité | TOGAF Principle: *Low Latency & Security* / OWASP API2:2023 | ✅ Appliqué & Vérifié |
| **OPT-07** | Découplage HTTP/DB dans `UserVfpStatusService` (`TransactionTemplate`) | Scalabilité HikariCP | TOGAF Principle: *Resource Optimization & ACID Boundaries* | ✅ Appliqué & Vérifié |
| **OPT-08** | Préchargement Batch des Barèmes de Fidélité (`PointsAccountService.findByStoreUidIn`) | Performance Base de Données | TOGAF Principle: *Resource Optimization & Data Access Efficiency* | ✅ Appliqué & Vérifié |
| **OPT-09** | Modernisation Kafka 4.x (`JacksonJsonSerializer`/`Deserializer`) & Actuator `metrics` | Maintenabilité & Observabilité | TOGAF Principle: *Operational Visibility & Technical Debt Containment* | ✅ Appliqué & Vérifié |
| **OPT-10** | Optimisation du Build Conteneur Local (Spring Boot Layered JAR, `.dockerignore` & `ENTRYPOINT` exec) | Déploiement & Industrialisation (Docker) | TOGAF Principle: *Resource Optimization & Fast Delivery* / 12-Factor V & IX | ✅ Appliqué & Vérifié |
| **OPT-11** | Découplage HTTP/DB dans `PointsAccountService` (`executeInTransaction`) | Scalabilité HikariCP & Verrous SQL | TOGAF Principle: *Resource Optimization & ACID Boundaries* | ✅ Appliqué & Vérifié |
| **OPT-12** | Mise en Cache des Barèmes Marchands (`@EnableCaching` / `@Cacheable` / `@CacheEvict`) | Performance Base de Données | TOGAF Principle: *Resource Optimization & Fast Response Times* | ✅ Appliqué & Vérifié |
| **OPT-13** | Durcissement Cryptographique JWT M2M (RS256 Strict dans `ServiceTokenProvider`) | Sécurité & Confiance Numérique | TOGAF Principle 8: *Security & Defense in Depth* / OWASP API2:2023 | ✅ Appliqué & Vérifié |

---

## Fiches Détaillées par Décision d'Architecture

### OPT-04 — Optimisation du Catalogue Public (`GiftCatalogService` & `StoreVerificationPort`)

* **Problème :** `GiftCatalogService.getPublicCatalog` était annoté `@Transactional(readOnly = true)` et effectuait deux appels HTTP distincts vers `Store-Service` (`findStoreName` et `findStoreCategory`) par commerce distinct, immobilisant une connexion HikariCP pendant les appels réseau.
* **Solution :**
  - Retrait de `@Transactional(readOnly = true)` englobant `getPublicCatalog` (les lectures JPA restent bornées au niveau du persistence adapter).
  - Ajout de `StoreVerificationPort.findStoreInfo(UUID)` implémenté dans `StoreServiceHttpAdapter` pour récupérer nom et catégorie en **1 seul appel HTTP** par commerce.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Resource Optimization & Scalability / AWS Well-Architected (Performance Efficiency)**  
  > *« Les frontières transactionnelles ne doivent pas englober d'appels réseau synchrones et les requêtes inter-services doivent être factorisées pour réduire la latence et la pression sur les pools de connexions. »*

---

### OPT-05 — Uniformisation des Clients HTTP en HTTP/1.1 avec Pooling

* **Problème :** `UsersServiceHttpAdapter`, `TransitGatewayHttpAdapter` et `ParkingGatewayHttpAdapter` utilisaient `SimpleClientHttpRequestFactory` (sans pooling), tandis que `StoreServiceHttpAdapter`, `SalesServiceHttpAdapter` et `JwtKeyProvider` négociaient potentiellement HTTP/2 (`h2c`) sur HTTP clair, exposant à des erreurs `RST_STREAM` avec WireMock.
* **Solution :**
  - Migration de tous les adapters REST et de `JwtKeyProvider` vers `JdkClientHttpRequestFactory` / `HttpClient` configuré explicitement en `HttpClient.Version.HTTP_1_1`.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Technology Standardization & Resource Optimization**  
  > *« Les communications HTTP inter-services internes doivent standardiser des clients à réutilisation de connexions TCP avec un protocole uniforme afin de garantir performance et compatibilité réseau. »*

---

### OPT-06 — Cache JWT M2M & Rafraîchissement Planifié des Révocations

* **Problème :** `TokenRevocationService` instanciat un nouveau `HttpClient` à chaque rafraîchissement et bloquait les threads de requêtes HTTP toutes les 15 secondes ; `ServiceTokenProvider` re-signait un jeton JWT à chaque appel inter-services.
* **Solution :**
  - Mutualisation d'un `HttpClient` (`HTTP_1_1`) et rafraîchissement asynchrone en arrière-plan via `@Scheduled(fixedDelay = 15_000L)` dans `TokenRevocationService`.
  - Mise en cache du jeton M2M dans `ServiceTokenProvider` avec marge de renouvellement de 30 secondes avant expiration.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Resource Optimization & Low Latency / OWASP API2:2023**  
  > *« Les contrôles transverses de sécurité ne doivent ni bloquer le chemin critique des requêtes métier ni répéter inutilement des opérations cryptographiques coûteuses. »*

---

### OPT-07 — Découplage HTTP/DB dans `UserVfpStatusService`

* **Problème :** `UserVfpStatusService.evaluateAndApply` était annoté `@Transactional` sur toute sa durée, maintenant une connexion PostgreSQL ouverte pendant l'appel HTTP synchrone vers `Sales-Service` (`evaluateActivity`).
* **Solution :**
  - Exécution de `evaluateUserActivityUseCase.evaluateActivity(...)` hors transaction DB, puis utilisation de `TransactionTemplate` uniquement pour la lecture et la mise à jour du statut VFP.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Resource Optimization & ACID Boundaries**  
  > *« Les verrous et connexions aux bases de données doivent être strictement limités aux opérations de persistance locale. »*

---

### OPT-08 — Préchargement Batch des Barèmes de Fidélité (`PointsAccountService`)

* **Problème :** Lors du traitement d'un événement `OrderPaidKafkaEvent` multi-commerces, `computeStoresBreakdownPoints` exécutait une requête SQL `findByStoreUid` par commerce.
* **Solution :**
  - Ajout de `findByStoreUidIn(Collection<UUID>)` sur `SpringDataMerchantLoyaltyRateRepository`, `MerchantLoyaltyRatePersistencePort` et `MerchantLoyaltyRatePersistenceAdapter` pour charger tous les barèmes d'une commande en 1 seule requête SQL.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Resource Optimization & Data Access Efficiency**  
  > *« Les accès aux référentiels de tarification ou de barèmes dans une boucle doivent être regroupés en requêtes ensemblistes afin d'éliminer les allers-retours SQL redondants. »*

---

### OPT-09 — Modernisation Kafka 4.x & Actuator `metrics`

* **Problème :** `KafkaConsumerConfig` et `application.yaml` utilisaient les classes dépréciées `JsonSerializer` / `JsonDeserializer` ; Actuator n'exposait pas `metrics` (critère DoD **SLE-266**).
* **Solution :**
  - Migration vers `JacksonJsonSerializer` / `JacksonJsonDeserializer` (0 warning de compilation).
  - Ajout de `hibernate.default_batch_fetch_size: 50` et exposition de `metrics` dans `management.endpoints.web.exposure.include: health,info,metrics`.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Operational Visibility & Technical Debt Containment**  
  > *« Les composants techniques doivent éliminer les API dépréciées et exposer les métriques d'exécution standardisées pour permettre la validation continue des performances. »*

---

### OPT-10 — Optimisation du Build Conteneur Local (Layered JAR, `.dockerignore` & `ENTRYPOINT` exec)

* **Problème :** `.dockerignore` laissait passer `src/test/`, `docs/` et `bruno/` dans le contexte Docker ; `Dockerfile` compilait les tests, produisait un Fat JAR monolithique et utilisait `sh -c`.
* **Solution :**
  - Durcissement de `.dockerignore`, compilation ciblée (`COPY src/main`, `-Dmaven.test.skip=true`), extraction des 4 couches Spring Boot (`jarmode=tools`), `ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]` (`JAVA_TOOL_OPTIONS`) et `HEALTHCHECK` sur `/actuator/health/liveness`.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Resource Optimization & Fast Delivery / 12-Factor App V & IX**  
  > *« Les artefacts conteneurisés doivent séparer les couches immuables du code applicatif, minimiser le contexte de construction et exécuter la JVM directement en PID 1. »*

---

### OPT-11 — Découplage HTTP/DB dans `PointsAccountService`

* **Problème :** `processPhysicalPayment` et `processOrderPaid` étaient annotés `@Transactional` et exécutaient `resolveStoreName(storeUid)` (appel REST synchrone vers `Store-Service`) *pendant* que le verrou pessimiste `SELECT FOR UPDATE` et la connexion HikariCP étaient maintenus.
* **Solution :**
  - Résolution préalable du nom du commerce (`resolveStoreName` / `resolveSingleOrderStoreName`) **hors transaction DB**.
  - Exécution de la vérification d'idempotence, du verrou pessimiste et de l'écriture JPA via `executeInTransaction(...)` (`TransactionTemplate`).
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Resource Optimization & ACID Boundaries**  
  > *« Les verrous pessimistes et connexions DB ne doivent jamais être retenus pendant des appels réseau externes synchrones. »*

---

### OPT-12 — Mise en Cache des Barèmes Marchands (`@Cacheable` / `@CacheEvict`)

* **Problème :** Les barèmes de fidélité (`MerchantLoyaltyRate`) étaient relus en base à chaque événement Kafka de paiement et chaque consultation API.
* **Solution :**
  - Activation de `@EnableCaching` sur `LoyaltyServiceApplication`.
  - Ajout de `@Cacheable(value = "loyaltyRates", key = "#storeUid")` sur `MerchantLoyaltyRatePersistenceAdapter.findByStoreUid` et `@CacheEvict(value = "loyaltyRates", allEntries = true)` sur `save` et `deleteByStoreUid`.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle — Resource Optimization & Fast Response Times**  
  > *« Les données de configuration peu volatiles et fréquemment lues doivent être mises en cache avec invalidation explicite lors des écritures. »*

---

### OPT-13 — Durcissement Cryptographique JWT M2M (RS256 Strict)

* **Problème :** `ServiceTokenProvider` conservait un mode de repli symétrique HMAC (`application.security.jwt.secret-key`), créant une divergence de sécurité par rapport à `Sales-Service` et `Store-Service`.
* **Solution :**
  - Suppression complète du fallback HMAC (`secretKeyString`, `parseHmacSecretKey`) et signature strictement imposée en **RS256**.
* **Règle d'Architecture Associée :**
  > **TOGAF Principle 8 — Security & Defense in Depth / OWASP API2:2023**  
  > *« L'architecture de sécurité ne doit tolérer aucune rétrogradation vers des schémas cryptographiques symétriques ou dégradés. »*

