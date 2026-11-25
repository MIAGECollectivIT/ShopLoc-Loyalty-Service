# 🚀 Collection Bruno - Loyalty Service API (ShopLoc)

Cette collection de tests fonctionnels et d'intégration HTTP couvre l'intégralité des endpoints du microservice **Loyalty Service** (`ShopLoc-Loyalty-Service`).

---

## 📁 Structure de la Collection

```text
bruno/Loyalty Service/
├── bruno.json
├── environments/
│   ├── Local.bru              # Configuration pour localhost:8082 & Users sur 8081
│   └── Dev.bru                # Configuration pour https://dev.shop-loc.fr
│
├── 00 - Authentification (Users Service)/ # Connexion automatique pour alimenter les tokens
│   ├── 01 - Connexion Commercant (Suzanne) - 200.bru
│   ├── 02 - Connexion Administrateur (Marius) - 200.bru
│   └── 03 - Connexion Client (Pierre) - 200.bru
│
├── 01 - Statut et Sante/       # Healthcheck et statut de disponibilite
│   ├── 01 - Verifier le statut du service (Authentifie) - 200.bru
│   ├── 02 - Verifier Actuator Health (Public) - 200.bru
│   └── 03 - Rejet verification statut sans token - 401.bru
│
├── 02 - Bareme Fidelite Commercant (Loyalty Rates)/ # Baremes de points par commerce
│   ├── 01 - Consulter le bareme d un commerce (Public) - 200.bru
│   ├── 02 - Configurer le bareme standard 1 euro = 1 pt (MERCHANT) - 200.bru
│   ├── 03 - Configurer un bareme personnalise 5 euros = 10 pts (MERCHANT) - 200.bru
│   └── 04 - Configurer le bareme d un commerce (ADMIN) - 200.bru
│
├── 03 - Securite et Validations/ # Tests anti-usurpation (BOLA/IDOR), RBAC et Bean Validation
│   ├── 01 - Rejet configuration sans token - 401.bru
│   ├── 02 - Rejet configuration avec role CLIENT - 403.bru
│   ├── 03 - Rejet configuration par commercant non proprietaire (BOLA) - 403.bru
│   ├── 04 - Rejet configuration sur commerce inexistant - 404.bru
│   ├── 05 - Rejet configuration avec montant euro nul ou negatif - 400.bru
│   ├── 06 - Rejet configuration avec nombre de points nul ou negatif - 400.bru
│   ├── 07 - Rejet configuration avec plus de 2 decimales sur montant euro - 400.bru
│   ├── 08 - Rejet configuration avec corps vide (Validation) - 400.bru
│   ├── 09 - Rejet creation cadeau sans token - 401.bru
│   ├── 10 - Rejet creation cadeau avec role CLIENT - 403.bru
│   ├── 11 - Rejet creation cadeau par commercant non proprietaire (BOLA) - 403.bru
│   ├── 12 - Rejet creation cadeau pointsCost nul ou negatif - 400.bru
│   ├── 13 - Rejet creation cadeau corps vide (Validation) - 400.bru
│   ├── 14 - Rejet modification cadeau par commercant non proprietaire - 403.bru
│   ├── 15 - Rejet suppression cadeau par role CLIENT - 403.bru
│   └── 16 - Rejet ajustement stock cadeau par role CLIENT - 403.bru
│
├── 04 - Catalogue Cadeaux (Gift Catalog)/ # Catalogue de lots et cadeaux (SLE-60, SLE-39 & SLE-49)
│   ├── 01 - Creer un cadeau (MERCHANT - JSON) - 201.bru
│   ├── 02 - Creer un cadeau avec photo S3 (multipart) - 201.bru
│   ├── 03 - Creer un cadeau (ADMIN) - 201.bru
│   ├── 04 - Creer un cadeau sans stock (Defaut a 0) - 201.bru
│   ├── 05 - Lister tous les cadeaux du magasin (Public) - 200.bru
│   ├── 06 - Consulter un cadeau par ID (Public) - 200.bru
│   ├── 07 - Consulter le catalogue global des cadeaux (Public - SLE-39) - 200.bru
│   ├── 08 - Filtrer le catalogue global par commerce (Public - SLE-39) - 200.bru
│   ├── 09 - Filtrer le catalogue global par categorie (Public - SLE-39) - 200.bru
│   ├── 10 - Filtrer le catalogue global par commerce et categorie (Public - SLE-39) - 200.bru
│   ├── 11 - Mettre a jour un cadeau (MERCHANT - JSON) - 200.bru
│   ├── 12 - Modifier l image d un cadeau avec photo S3 (multipart) - 200.bru
│   ├── 13 - Consulter l image binaire S3 d un cadeau (Public) - 200.bru
│   ├── 14 - Supprimer un cadeau (MERCHANT) - 204.bru
│   ├── 15 - Consulter un cadeau supprime - 404.bru
│   └── 16 - Creer un cadeau exclusif VFP (MERCHANT - JSON - SLE-49) - 201.bru
│
└── 05 - Gestion des Stocks Cadeaux/ # Reapprovisionnement et decrement atomique
    ├── 01 - Ajuster le stock - Reapprovisionnement delta positif (MERCHANT) - 200.bru
    ├── 02 - Ajuster le stock - Decrementation delta negatif (MERCHANT) - 200.bru
    ├── 03 - Rejet decrement au-dela du stock (Rupture) - 400.bru
    └── 04 - Rejet ajustement sans quantite (body vide) - 400.bru
```

---

## 🛠️ Utilisation dans Bruno

1. Ouvrez l'application **Bruno**.
2. Cliquez sur **Open Collection** et sélectionnez le dossier `bruno/Loyalty Service`.
3. En haut à droite, sélectionnez l'environnement :
   - `Local` pour pointer vers `http://localhost:8082` (Loyalty) et `http://localhost:8081` (Users).
   - `Dev` pour pointer vers `https://dev.shop-loc.fr`.
4. Ouvrez le dossier **`00 - Authentification (Users Service)`**, remplacez `USER` et `PASSWORD` par vos identifiants dans la requête choisie (Commerçant, Admin ou Client) et cliquez sur **Send** : le token JWT est **automatiquement injecté** dans les variables d'environnement (`merchantToken`, `adminToken`, `clientToken`).
5. Cliquez sur **Run** sur un dossier pour exécuter toute la suite de tests fonctionnels avec assertions automatisées (`res.status`, `res.body`).

> [!IMPORTANT]
> **Sécurité des identifiants & Git (`skip-worktree`) :**
> Les fichiers de requêtes d'authentification contiennent par défaut `"email": "USER"` et `"password": "PASSWORD"` dans le dépôt distant.
> Afin d'éviter que vos identifiants ou mots de passe de test ne soient accidentellement commités lorsque vous les modifiez dans Bruno, ces fichiers sont protégés par la directive `skip-worktree` de Git :
> ```bash
> git update-index --skip-worktree "bruno/Loyalty Service/00 - Authentification (Users Service)/"*
> ```
> **Conséquence :** Vos identifiants locaux restent sur votre machine pour faire fonctionner Bruno, mais **Git ne prendra jamais en compte les mises à jour de mots de passe / emails** (`git status` reste propre et rien ne fuite dans les commits).
> *(Pour réactiver temporairement le suivi Git si vous souhaitez faire évoluer le template : `git update-index --no-skip-worktree <fichier>`)*.

---

## 📋 Variables d'environnement

| Variable | Description |
| :--- | :--- |
| `baseUrl` | URL de base de Loyalty Service (`http://localhost:8082` en Local) |
| `usersUrl` | URL de base de Users Service (`http://localhost:8081` en Local) |
| `storeId` | Identifiant du commerce cible (ex: `1`) |
| `giftId` | Identifiant de l'article cadeau cible (ex: `1`) |
| `merchantToken` | Token JWT valide avec le rôle `MERCHANT` (auto-rempli par la requête de login) |
| `otherMerchantToken` | Token JWT valide avec le rôle `MERCHANT` (non-propriétaire du `storeId` pour tester le BOLA) |
| `clientToken` | Token JWT valide avec le rôle `CLIENT` (auto-rempli par la requête de login) |
| `adminToken` | Token JWT valide avec le rôle `ADMIN` (auto-rempli par la requête de login) |

---

## 💻 Exécution en ligne de commande (CLI)

```bash
# Exécution de toute la collection en local
bru run --env Local

# Exécution ciblée du catalogue de cadeaux
bru run "04 - Catalogue Cadeaux (Gift Catalog)" --env Local

# Exécution des tests de sécurité et validation
bru run "03 - Securite et Validations" --env Local
```
