# AUDIT_REPORT — ShopLoc-Loyalty-Service (`Loyalty`)

> Audit en lecture seule. Aucun fichier source modifié. Chemins relatifs à `src/main/java/fr/miage/collectivit/loyaltyservice/` (noté `…/`) ; `…/ia/` = `infrastructure/adapter/`.

## 1. Carte d'identité & Baseline

| Élément | Valeur |
|---|---|
| Build | Maven (`mvnw`), Java 21, Spring Boot **4.1.1** |
| Architecture | **Hexagonale** (`domain` / `application` / `infrastructure/adapter/{in,out}`) — la plus structurée des 4 services |
| Persistance | PostgreSQL + JPA (7 `*JpaEntity` séparées du domaine), Flyway |
| Kafka | `spring-kafka` : **consommateur** (`KafkaTransactionApprovedConsumer`, `KafkaOrderPaidConsumer`, + listener physique) avec `ErrorHandlingDeserializer` + `DefaultErrorHandler` + DLT |
| Clients HTTP | `RestClient` vers Users, Store, Sales, passerelle parking (timeouts présents) |
| Autres | S3/MinIO (images cadeaux), job planifié `VfpDailyRevisionJob`, Micrometer (`MeterRegistry`), actuator probes, PMD |
| Taille | ~7 950 lignes Java |
| Compilation | `./mvnw test-compile` : ✅ OK |
| Démarrage | **Non mesuré** (PostgreSQL + Kafka requis) ; analyse statique ci-dessous |

## 2. Problèmes détectés

### 🔴 Critique

**C1 — Les consommateurs Kafka avalent les exceptions → le `DefaultErrorHandler` et la DLT ne servent jamais**
- `…/ia/in/event/KafkaTransactionApprovedConsumer.java:20-47` : `try { evaluateAndApply(...) } catch (Exception ex) { log.error(...) }` — l'exception n'est pas relancée. Idem pour `…/ia/in/event/KafkaOrderPaidConsumer.java:18-` (même schéma `@SuppressWarnings("PMD.AvoidCatchingGenericException")`).
- Conséquence : une panne transitoire de BDD/API fait **perdre définitivement** l'événement (offset commité), alors que `config/KafkaConsumerConfig.java:85-110` a prévu 6 retries exponentiels (1 s → 30 s) + `DeadLetterPublishingRecoverer`. Les points de fidélité / statut VFP sont désynchronisés sans alerte. Combiné au C1 de `Sales` (publication sans garantie), la chaîne Sales→Loyalty n'a aucune garantie de livraison.
```java
// Avant
try { useCase.evaluateAndApply(id); } catch (Exception ex) { log.error(..., ex); }
// Après : laisser remonter pour retry/DLT (ne capturer que les erreurs "métier non retryables")
useCase.evaluateAndApply(id);   // DefaultErrorHandler → retries → .DLT
```
- Vérifier aussi l'**idempotence** du traitement (rejeu possible après retry) — contrôler une clé d'événement dans `UserPointsTransaction`.

**C2 — `spring.json.trusted.packages: "*"` + mapping de types couplé aux classes de `Sales`**
- `src/main/resources/application.yaml` (`spring.kafka.consumer.properties.spring.json.trusted.packages: "*"`) et `…/config/KafkaConsumerConfig.java:~58`. Le mapping nomme les FQCN du service Sales (`fr.miage.collectivit.salesservice.…`) : couplage de contrat par nom de classe Java, et confiance totale sur les packages (surface de désérialisation).
- Proposition : mapping par **alias logiques** (`transactionApproved:…`), `trusted.packages` restreint aux DTO attendus, schéma partagé (JSON Schema / Avro) ou module de contrats commun.

### 🟠 Majeur

**M1 — DLT incomplète**
- `…/config/KafkaTopicConfig.java:17-60` : seule `shoploc.physical-payments.DLT` est déclarée ; aucune DLT pour `shoploc.order-payments` ni `shoploc.transactions.approved`, qui n'a en outre aucun bean `NewTopic` (ni dans Loyalty ni dans Sales, hors création automatique du broker). Le `DeadLetterPublishingRecoverer` publie sur `<topic>.DLT` : si absent et auto-création désactivée, l'échec de publication DLT bloque la partition.
- `application.yaml` : mapping de types incomplet vs code (`OrderPaidKafkaEvent` présent dans `KafkaConsumerConfig.java:~57` mais absent du YAML) → deux sources de vérité.

**M2 — Concurrence Kafka non configurée**
- Aucun `concurrency` sur les `@KafkaListener` ni `factory.setConcurrency(...)` ; 1 thread par listener quel que soit le nombre de partitions. À dimensionner selon `DEFAULT_PARTITIONS` (et garantir l'ordre par clé `userId`).
- `spring.kafka.admin.fail-fast` / `auto-create` non explicitement définis (valeurs par défaut Boot) ; `factory.setAutoStartup` piloté par `@Value` (`KafkaConsumerConfig.java`) — OK.

**M3 — Double entrée pour le même événement (REST webhook + Kafka)**
- `…/ia/in/web/TransactionEventWebhookController.java:33-43` (`POST /v1/loyalty/events/transaction-approved`) en parallèle du consommateur Kafka : risque de **double comptage** si les deux sont utilisés ; vérifier idempotence et sécurité du webhook.

**M4 — Gestion d'erreurs non conforme RFC 7807**
- `…/ia/in/web/GlobalExceptionHandler.java:33-60` : `ResponseEntity<Map<String,Object>>`, pas de `ProblemDetail`.

**M5 — Pagination absente sur le catalogue**
- `…/ia/in/web/GiftCatalogController.java:78`, `…/ia/in/web/PublicGiftCatalogController.java:41` (`List<GiftResponse>`) ; Spring Data `Pageable` n'est utilisé que dans `SpringDataUserVfpStatusRepository.java:15-17` (job VFP).

**M6 — Démarrage non optimisé / config**
- `Dockerfile` : pas d'AOT/AppCDS. `application.yaml` : pas de `spring.datasource.hikari.*`, ni `hibernate.temp.use_jdbc_metadata_defaults=false`, pas de `lazy-initialization`. Positif : `ddl-auto: validate`, `open-in-view: false`.
- `management.endpoint.health.show-details: always` → exposition publique des détails d'infra.
- `VfpDailyRevisionJob` (`…/ia/in/scheduled/VfpDailyRevisionJob.java`) : sans verrou distribué (ShedLock) → exécuté par chaque réplique si > 1 pod (`@EnableScheduling` à confirmer).

**M7 — Pas de tracing distribué** : ni `micrometer-tracing`, ni propagation dans les headers Kafka, alors que ce service est le bout de chaîne Sales → Kafka → Loyalty → Users/Store.

**M8 — Cohérence de l'API**
- Mélange de préfixes : certains contrôleurs en `{"/v1/…","/api/v1/…"}` (balance, VFP, webhook), d'autres en `/v1/loyalty/…` seul (`GiftCatalogController.java:51`, `MerchantLoyaltyRateController.java:39`, `PublicGiftCatalogController.java:25`) ; `GET /customers/{customerId}/points` (`UserPointsBalanceController.java:108`) duplique `/users/{userId}/balance`.

### 🟡 Mineur / Ergonomie

**m1 — `@Autowired`** (principalement sur constructeurs ; ne pas confondre avec injection par champ) : `…/ParkingSessionService.java:50`, `UserActivityEvaluationService.java:26`, `UserVfpStatusService.java:32`, `UserPointsBalanceService.java:21`, `PointsAccountService.java:56`, `…/ia/out/rest/*HttpAdapter.java` (`@Autowired(required=false) ServiceTokenProvider`), `VfpDailyRevisionJob.java:28-32`, `S3GiftImageStorageAdapter.java:45`. Plusieurs constructeurs par classe (prod + test) → utiliser un constructeur unique + `ObjectProvider`.

**m2 — `@Value` : 36 occurrences, aucun `@ConfigurationProperties`** (S3, `services.*`, Kafka, JWT, timeouts) — c'est le service avec le plus de `@Value`.

**m3 — `catch (Exception)`** : `…/ia/out/storage/S3GiftImageStorageAdapter.java:81,111,135,180` (le `:180` est un `catch (Exception ignored)` qui renvoie une valeur de repli silencieusement).

**m4 — Dépendances** : « used undeclared » à déclarer : `kafka-clients`, `flyway-core`, `micrometer-core`, `spring-data-jpa`, `awssdk:{regions,auth,sdk-core,aws-core}` ; « unused » = starters/runtime (faux positifs). `awaitility` utilisé en test non déclaré. springdoc 2.8.5 à valider avec Boot 4.1. Pas de doublon fonctionnel.

**m5 — Points conformes ✅** : entités JPA isolées dans `infrastructure/.../persistence/entity` (aucune fuite vers contrôleurs, ports/adapters) ; pas d'association JPA (donc pas de N+1) ; timeouts sur les clients HTTP (`StoreServiceHttpAdapter`, `UsersServiceHttpAdapter`, `SalesServiceHttpAdapter`, `ParkingGatewayHttpAdapter` : connect + read) ; `ErrorHandlingDeserializer` + backoff exponentiel + `addNotRetryableExceptions` ; pas de secret en dur (`s3.access-key: ${S3_ACCESS_KEY:}` sans valeur par défaut ✅ — modèle à suivre) ; aucun `printStackTrace`.

## 3. Plan d'action priorisé

| # | Action | Gain | Effort |
|---|---|---|---|
| 1 | Relancer les exceptions dans les `@KafkaListener` + idempotence (C1) | Plus aucune perte d'événement, DLT effective | Faible |
| 2 | Alias de types + `trusted.packages` restreint (C2) | Contrat découplé, sécurité | Moyen |
| 3 | Créer toutes les DLT + `NewTopic` manquants, aligner YAML/Java (M1) | Robustesse Kafka | Faible |
| 4 | Fixer `concurrency` selon partitions (M2) | Débit | Faible |
| 5 | Choisir un seul canal (Kafka vs webhook) ou garantir l'idempotence (M3) | Évite le double comptage | Moyen |
| 6 | `ProblemDetail` (M4) | Cohérence API | Faible |
| 7 | Paginer le catalogue (M5) | Scalabilité | Moyen |
| 8 | AOT/AppCDS, Hikari, metadata JDBC, ShedLock, `show-details` (M6) | −30-40 % démarrage, job sûr | Moyen |
| 9 | `micrometer-tracing` + observation Kafka (M7) | Traçabilité bout en bout | Faible |
| 10 | Uniformiser préfixes d'URL (M8) | API cohérente | Moyen |
| 11 | `@ConfigurationProperties` + constructeur unique (m1, m2) | Maintenabilité | Moyen |
| 12 | Déclarer les dépendances « used undeclared » (m4) | Build propre | Faible |


## 4. Annexe — Build Docker (mesuré, build à froid `--no-cache`)

| Étape | Durée |
|---|---|
| `RUN mvn dependency:go-offline -B` (builder 4/6) | **71.2 s** (≈ 1663 artefacts téléchargés) |
| `RUN mvn clean package -DskipTests` (builder 6/6) | 5.1 s |
| Copie JAR + image finale | ~2 s (image 486 Mo) |
| **Total** | **80 s** |

- 🟠 **D1 — `dependency:go-offline` re-télécharge tout à chaque invalidation du cache** (`Dockerfile`, `RUN mvn dependency:go-offline -B`) : il ramène aussi des plugins/dépendances jamais utilisés au build. Aucun cache Maven persistant entre builds. Le cache de couche saute dès que `pom.xml` change, ou après `--no-cache` / prune.
- Proposition (gain estimé : ~50-70 s → ~5-10 s sur rebuild après modif du `pom.xml`) :
```dockerfile
# syntax=docker/dockerfile:1.7
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:resolve-plugins dependency:resolve
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -o clean package -DskipTests
```
- `mvnw`/`.mvn` copiés dans le contexte mais non utilisés : le conteneur utilise l'image `maven:3.9` (OK, mais version Maven non épinglée au wrapper).
- Compilation seule : ~4 s → le build applicatif n'est pas le problème, seul le téléchargement des dépendances l'est.
