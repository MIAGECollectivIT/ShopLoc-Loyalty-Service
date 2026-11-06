# Collection Bruno - Loyalty Service API

Collection de tests d'API pour le microservice **Loyalty Service** (`ShopLoc-Loyalty-Service`).

## Structure de la Collection

```text
Loyalty Service/
├── 00 - Authentification (Users Service)/
│   ├── 01 - Connexion Commercant (Suzanne) - 200.bru
│   ├── 02 - Connexion Administrateur (Marius) - 200.bru
│   └── 03 - Connexion Client (Pierre) - 200.bru
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

- **Local** : 
  - `baseUrl` : `http://localhost:8082` (Loyalty Service Docker)
  - `usersUrl` : `http://localhost:8081` (Users Service Docker)
- **Dev** : `https://dev.shop-loc.fr`

## Authentification automatique dans Bruno

Pour récupérer automatiquement vos tokens JWT sans les copier-coller :
1. Sélectionnez l'environnement **Local** dans Bruno.
2. Ouvrez le dossier **`00 - Authentification (Users Service)`**.
3. Renseignez vos identifiants dans le corps de la requête (remplacez `USER` et `PASSWORD`) puis cliquez sur **Send** :
   - `01 - Connexion Commercant` ➔ remplit automatiquement `{{merchantToken}}`
   - `02 - Connexion Administrateur` ➔ remplit automatiquement `{{adminToken}}`
   - `03 - Connexion Client` ➔ remplit automatiquement `{{clientToken}}`
4. Vous pouvez ensuite exécuter n'importe quelle requête authentifiée de la collection !

> [!IMPORTANT]
> **Sécurité des identifiants & Git (`skip-worktree`) :**
> Les fichiers de requêtes d'authentification contiennent par défaut `"email": "USER"` et `"password": "PASSWORD"` dans le dépôt distant.
> Afin d'éviter que vos identifiants ou mots de passe de test ne soient accidentellement commités lorsque vous les modifiez dans Bruno, ces fichiers sont protégés par la directive `skip-worktree` de Git :
> ```bash
> git update-index --skip-worktree "bruno/Loyalty Service/00 - Authentification (Users Service)/"*
> ```
> **Conséquence :** Vos identifiants locaux restent sur votre machine pour faire fonctionner Bruno, mais **Git ne prendra jamais en compte les mises à jour de mots de passe / emails** (`git status` reste propre et rien ne fuite dans les commits).
> *(Pour réactiver temporairement le suivi Git si vous souhaitez faire évoluer le template : `git update-index --no-skip-worktree <fichier>`)*.

## Variables d'environnement

| Variable | Description |
| :--- | :--- |
| `baseUrl` | URL de base de Loyalty Service (`http://localhost:8082` en Local) |
| `usersUrl` | URL de base de Users Service (`http://localhost:8081` en Local) |
| `storeId` | Identifiant du commerce cible (ex: `1`) |
| `merchantToken` | Token JWT valide avec le rôle `MERCHANT` (auto-rempli par la requête de login) |
| `otherMerchantToken` | Token JWT valide avec le rôle `MERCHANT` (non-propriétaire du `storeId` pour tester le BOLA) |
| `clientToken` | Token JWT valide avec le rôle `CLIENT` (auto-rempli par la requête de login) |
| `adminToken` | Token JWT valide avec le rôle `ADMIN` (auto-rempli par la requête de login) |

