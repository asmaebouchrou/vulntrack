# VulnTrack

**A full-stack vulnerability scanner for Maven dependencies.**

VulnTrack is a cybersecurity application that analyzes Java project dependencies, detects known vulnerabilities using the **OSV API**, and presents the results through a clean, easy-to-use web interface.

It also stores scan results in PostgreSQL, allowing users to review previous analyses.

## Technologies & Tools

**Backend**

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)

Spring MVC, Spring Data JPA, Jackson and REST APIs are used for request handling, persistence and data processing.

**Frontend**

![React](https://img.shields.io/badge/React-20232A?style=flat-square&logo=react&logoColor=61DAFB)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat-square&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=flat-square&logo=css&logoColor=white)

**Database, Infrastructure & DevOps**

![PostgreSQL](https://img.shields.io/badge/PostgreSQL_17-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white)
![Git](https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

Docker Compose manages the application services. GitHub Actions automates backend tests and frontend build checks.

**Testing & Vulnerability Intelligence**

![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white)
![Mockito](https://img.shields.io/badge/Mockito-Testing-5B8C5A?style=flat-square)
![OSV](https://img.shields.io/badge/OSV_API-Vulnerability_Data-405F89?style=flat-square)

Testing includes JUnit, Mockito, MockMvc and Spring Security Test.

## Features

- Upload Maven `pom.xml` files.
- Extract project dependencies automatically.
- Detect known vulnerabilities through the OSV API.
- View CVE and GHSA identifiers, descriptions and severity information.
- Display available fixed versions and security advisory links.
- Save and retrieve scan history.
- Protect API endpoints with Spring Security and CSRF protection.
- Run the full application with Docker Compose.

## Screenshots

### Dashboard

![VulnTrack Dashboard](docs/screenshots/dashboard.png)

### Vulnerability Analysis

![Vulnerability Analysis Results](docs/screenshots/vulnerability-results.png)


### Requirements

Install **Docker Desktop** with Docker Compose support and make sure Docker is running.

### 1. Clone the repository

```bash
git clone https://github.com/asmaebouchrou/vulntrack.git
cd vulntrack
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```env
DB_PASSWORD=your_strong_local_password
```

Replace the example value with your own password.

If you already have a PostgreSQL volume, use its existing database credentials. Changing `.env` does not automatically change the password stored in an initialized database.

### 3. Start VulnTrack

Run:

```bash
docker compose up -d --build
```

Docker Compose starts the application services:

| Service | Description | Local address |
|---|---|---|
| Frontend | React + Nginx | http://localhost:8081 |
| Backend | Spring Boot REST API | http://localhost:8080 |
| Database | PostgreSQL | Port 5432 |

### 4. Open the application

Visit:

**http://localhost:8081**

Sign in using the development credentials configured for Spring Security. If the application uses Spring Boot's generated development password, check the backend startup logs:

```bash
docker compose logs backend
```

### 5. Analyze dependencies

1. Select a Maven `pom.xml` file.
2. Click **Analizar dependencias**.
3. Review detected vulnerabilities and security advisories.
4. Open the scan history to revisit previous results.

A sample Maven file is available at `test-files/pom.xml`.

### Useful Docker commands

Check running services:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs -f backend
```

Stop the application:

```bash
docker compose down
```

The configured PostgreSQL volume remains available for subsequent runs.

## Testing & Continuous Integration

Backend tests:

```bash
cd backend
mvn clean verify
```

Frontend build:

```bash
cd frontend
npm ci
npm run build
```

**GitHub Actions** runs automated checks to verify backend tests and frontend compilation during the repository's CI workflow.

## What This Project Demonstrates

VulnTrack brings together several important software engineering and cybersecurity skills:

- Building REST APIs with Java and Spring Boot.
- Integrating an external vulnerability intelligence service.
- Implementing authentication and CSRF protection.
- Working with relational databases and database migrations.
- Developing a functional React interface.
- Writing automated tests and configuring continuous integration.
- Packaging and running a full-stack application with Docker.

The result is a working end-to-end application that connects **frontend, backend, vulnerability intelligence and persistent storage** in a single project.

---

**VulnTrack — Full-stack development meets cybersecurity.**◊