# Collection Bruno - Loyalty Service API

Collection de tests d'API pour le microservice **Loyalty Service** (`ShopLoc-Loyalty-Service`).

## Structure de la Collection

```text
Loyalty Service/
├── 01 - Statut et Sante/
│   ├── 01 - Verifier le statut du service (Authentifie) - 200.bru
│   ├── 02 - Verifier Actuator Health (Public) - 200.bru
│   └── 03 - Rejet verification statut sans token - 401.bru
├── 02 - Bareme Fidelite Commercant (Loyalty Rates)/
│   ├── 01 - Consulter le bareme d un commerce (Public) - 200.bru
│   ├── 02 - Configurer le bareme standard 1 euro = 1 pt (MERCHANT) - 200.bru
│   ├── 03 - Configurer un bareme personnalise 5 euros = 10 pts (MERCHANT) - 200.bru
│   └── 04 - Configurer le bareme d un commerce (ADMIN) - 200.bru
├── 03 - Securite et Validations/
│   ├── 01 - Rejet configuration sans token - 401.bru
│   ├── 02 - Rejet configuration avec role CLIENT - 403.bru
│   ├── 03 - Rejet configuration par commercant non proprietaire (BOLA) - 403.bru
│   ├── 04 - Rejet configuration sur commerce inexistant - 404.bru
│   ├── 05 - Rejet configuration avec montant euro nul ou negatif - 400.bru
│   ├── 06 - Rejet configuration avec nombre de points nul ou negatif - 400.bru
│   ├── 07 - Rejet configuration avec plus de 2 decimales sur montant euro - 400.bru
│   └── 08 - Rejet configuration avec corps vide (Validation) - 400.bru
└── environments/
    ├── Local.bru
    └── Dev.bru
```

## Environnements

- **Local** : `http://localhost:8080`
- **Dev** : `https://dev.shop-loc.fr`

## Variables requises

| Variable | Description |
| :--- | :--- |
| `baseUrl` | URL de base du microservice (`http://localhost:8080` ou `https://dev.shop-loc.fr`) |
| `storeId` | Identifiant du commerce cible (ex: `1`) |
| `merchantToken` | Token JWT valide avec le rôle `MERCHANT` (et propriétaire du `storeId`) |
| `otherMerchantToken` | Token JWT valide avec le rôle `MERCHANT` (non-propriétaire du `storeId` pour tester le BOLA) |
| `clientToken` | Token JWT valide avec le rôle `CLIENT` |
| `adminToken` | Token JWT valide avec le rôle `ADMIN` |

