# Repository name
<!-- Short description of the repository -->

## Table of contents
<!-- TOC -->
- [0. Prerequisites](#0-prerequisites)
- [1. Installation & Setup](#1-installation--setup)
  - [1.1. Husky](#11-husky)
- [2. Launch & Test](#2-launch--test)
- [3. Project structure](#3-project-structure)
- [4. Workflow](#4-workflow)
  - [4.1 Quality pipeline](#41-quality-pipeline)
    - [4.1.1 (Husky) Pre commit](#411-husky-pre-commit)
    - [4.1.2 GitHub Actions](#412-github-actions)
<!-- /TOC -->

## 0. Prerequisites
<!-- Prerequisites to use and launch the projet (java version, npm version ...) -->

## 1. Installation & Setup
<!-- Total description of the project installation and setup (.env and more)-->

## 1.1. Husky 
> Do not bypass the root initialization. Husky pre-commit hooks are mandatory. Your code will be rejected automatically if quality standards are not met.
```bash
# At the root of the project (Git hooks for linters)
npm install
```


## 2. Launch & Test

<!-- Total description of how to launch and test the project -->

## 3. Project structure
```
├── .github/           # GitHub Actions and templates
│   └── workflows/     # GitHub Actions
├── src/  
│   ├── xxx/   
│   ├── xxx/        
│   └── xxx/          
├── .env.example        
└── README.md
```

## 4. Workflow
<!-- An explanation of the existing workflow (Quality, CI, CD), the API contracts (swagger and more), and potential procedures specifics to the infrastrure (data base migrations and more)  -->

### 4.1 Quality pipeline 
#### 4.1.1 (Husky) Pre commit
A pre-commit hook runs automatically on every commit to format and lint your code.
If the pipeline rejects your commit, run manually:
<!-- The terminal command to use linters -->


#### 4.1.2 GitHub Actions 
<!-- All GitHub actions, what branches they work on, and how do they activates -->
CI: Run on pull request on branches: 
<!-- //TODO  -->

CD: Run on commit on branches: 
<!-- //TODO  -->


