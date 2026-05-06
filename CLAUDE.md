# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a full-stack e-commerce application (LockerKorea/Sneakers) with AI-powered features. The project consists of three main services:

1. **Backend** (Spring Boot 3.2.2, Java 17) - REST API with JWT authentication, payment gateways, and AI integration
2. **Frontend** (Angular 17) - Web UI with SSR support, Material Design, and PrimeNG components
3. **Python Embedding Service** - FastAPI service using CLIP for text/image embeddings

All services run in Docker containers orchestrated via docker-compose.

## Repository Structure

```
lockkorea_co_Ngoc/
├── Backend/                    # Spring Boot application
│   ├── src/main/java/com/example/Sneakers/
│   │   ├── controllers/       # REST controllers (21 controllers total)
│   │   │   ├── *Controller.java (18 standard controllers)
│   │   │   └── ai/controllers/ (3 AI-specific controllers)
│   │   ├── services/          # Business logic (interfaces + implementations)
│   │   ├── repositories/      # Spring Data JPA repositories
│   │   ├── models/            # JPA entities (User, Product, Order, etc.)
│   │   ├── dtos/              # Data transfer objects
│   │   ├── configurations/    # Spring configurations (Security, CORS, etc.)
│   │   ├── components/        # Utilities (JWT, email, VNPay, etc.)
│   │   ├── ai/                # AI/LangChain4J integration (services, configs, listeners)
│   │   ├── exceptions/        # Custom exceptions
│   │   ├── filters/           # JWT filter, logging filters
│   │   ├── responses/         # Standardized response wrappers
│   │   └── utils/             # Helper utilities
│   └── src/test/              # Comprehensive test suite
│       ├── integration/       # 18 service-layer integration tests (H2 DB, real repositories)
│       └── services/          # 17 unit tests (Mockito, isolated)
├── Frontend/                   # Angular 17 application
│   └── src/app/
│       ├── features/          # Feature modules (admin, auth, register, components)
│       ├── core/              # Core services, interceptors, guards, models
│       └── shared/            # Shared components, pipes, directives
├── python-embedding-service/  # FastAPI service for CLIP embeddings
├── scripts/                   # UML generation (PlantUML, VP, XMI)
├── docker-compose.yml         # Docker orchestration
├── docker-start.ps1           # Windows script to start all services
└── README.MD                  # Main setup documentation (Vietnamese)
```

## Common Development Tasks

### Full Stack Startup (Docker)

```powershell
# Start all services (MySQL, Chroma, Backend, Frontend, Python embedding)
.\docker-start.ps1
```

This starts:
- MySQL on port 3308 (host) / 3306 (container)
- phpMyAdmin on port 8100
- Chroma vector DB on port 8000
- Backend on port 8089
- Frontend on port 80
- Python embedding service on port 9001

**Alternative**: Use `docker-compose` directly:
```bash
docker-compose up -d
```

**Note**: The `docker-start.ps1` script is Windows PowerShell. On Linux/Mac, use:
```bash
docker-compose up -d
```

### Backend Development

```bash
cd Backend

# Build without tests
.\mvnw clean install -DskipTests

# Run application (port 8089)
.\mvnw spring-boot:run

# Run all tests
.\mvnw test

# Run a single test class (unit or integration)
.\mvnw test -Dtest=UserServiceTest

# Run a single test method
.\mvnw test -Dtest=UserServiceTest#shouldThrowExceptionWhenUserNotFound

# Run integration tests only (all tests in /integration folder)
.\mvnw test -Dtest=**/integration/*

# Run a specific integration test (service-layer)
.\mvnw test -Dtest=ProductServiceIntegrationTest

# Generate JaCoCo coverage report (runs with mvn test)
# Report location: target/site/jacoco/index.html

# Package JAR
.\mvnw clean package -DskipTests

# Clean build with tests
.\mvnw clean test package
```

**Testing Notes:**

**Unit tests** (`services/`):
- Location: `src/test/java/com/example/Sneakers/services/`
- Use Mockito, no database
- Test business logic in isolation
- 17 unit test classes covering all service interfaces

**Service-layer Integration tests** (`integration/`):
- Location: `src/test/java/com/example/Sneakers/integration/`
- Use H2 in-memory DB with `@SpringBootTest` and `@ActiveProfiles("test")`
- Test **service + repository** integration with real database operations
- All repositories are real (not mocked)
- External dependencies (Email, JWT, Payment APIs, etc.) are mocked with `@MockBean`
- Transactional with automatic rollback after each test (`@Transactional`)
- 5 comprehensive integration test classes covering major services:
  * `UserServiceIntegrationTest` (30 test cases)
  * `NewsServiceIntegrationTest` (26 test cases)
  * `ProductServiceIntegrationTest` (45 test cases)
  * `CartServiceIntegrationTest` (27 test cases)
  * `VoucherServiceIntegrationTest` (36 test cases)
- Tests include: CRUD operations, business logic, validation, edge cases, error handling, idempotency

### Frontend Development

```bash
cd Frontend

# Install dependencies (first time)
npm install --legacy-peer-deps

# Development server (port 4200 with proxy to backend)
npm start

# Build for production (output in dist/)
npm run build

# Run unit tests (Karma/Jasmine)
npm test

# Build with SSR
npm run build --configuration production

# Serve SSR application
npm run serve:ssr:Sneakers-Ui
```

**Angular Structure:**
- Feature modules in `src/app/features/` (admin, auth, register, components)
- Core services and guards in `src/app/core/`
- Shared components in `src/app/shared/`
- Uses Angular Material 17, PrimeNG 17, Bootstrap 5
- **Proxy config**: `proxy.conf.json` (for dev API proxy - only proxies `/api/provinces` to external API)
- Development server proxies unknown routes to backend via Angular's default proxy (configured in angular.json)
- SSR support via `@angular/ssr` with server bundle in `dist/sneakers-ui/server/`

### Python Embedding Service

```bash
cd python-embedding-service

# First time setup
python -m venv .venv
# Activate: .venv\Scripts\Activate.ps1 (Windows) or source .venv/bin/activate (Linux)
pip install torch --index-url https://download.pytorch.org/whl/cpu
pip install -r requirements.txt

# Run service (port 9001)
uvicorn app:app --host 0.0.0.0 --port 9001

# Or use the provided script
.\start.bat
```

**Endpoints:**
- `GET /health` - Health check
- `POST /embed/text` - Generate text embeddings
- `POST /embed/image` - Generate image embeddings (base64 input)

## Key Architecture & Patterns

### Backend

- **Spring Boot 3.2.2** with Java 17
- **Security**: JWT-based authentication with Spring Security (stateless)
- **Persistence**: MySQL (production), H2 (tests) with JPA/Hibernate
- **API**: REST controllers with DTOs; all responses wrapped in `Response<T>` utility class
- **Design**: Service layer pattern with interfaces (`I*Service`) and implementations
- **Third-party integrations**:
  - Payment: Stripe, PayPal, VNPay
  - Shipping: GHN (Giao Hang Nhanh) - Vietnamese shipping provider
  - Social: Facebook API for social login
  - SMS: Twilio
  - Email: JavaMail with Gmail SMTP
  - AI: LangChain4J, Google Vertex AI (Gemini), Chroma vector store
- **Internationalization**: Message bundles in `src/main/resources/i18n/` (multi-language support)
- **Logging**: Logback with file (`logs/sneakers-app.log`) and console appenders; debug levels configurable per package
- **File Handling**: Multipart uploads (max 10MB) for product images and user content
- **DTO Pattern**: Clear separation between entities and API contracts; uses ModelMapper for object mapping

### Frontend

- **Angular 17** with standalone components (in some areas) and modules
- **Routing**: Lazy-loaded feature modules via `app.routes.ts`
- **State**: Service-based with RxJS observables
- **Styling**: Bootstrap 5 + Angular Material + PrimeNG components
- **SSR**: Server-side rendering supported (`@angular/ssr`)
- **Interceptors**: JWT token injection, error handling

### AI/Embedding Pipeline

**Backend AI Features** (LangChain4J):
- AI Chat with context retrieval from Chroma vector store
- Vector-based product similarity search
- Google Vertex AI (Gemini) integration for generative responses
- AI initialization health check endpoint
- Message listeners for async AI processing

**Python Embedding Service**:
- FastAPI service using CLIP (Contrastive Language-Image Pre-training) model
- Generates 512-dimensional embeddings for both text and images
- Endpoints:
  - `POST /embed/text` - text embeddings
  - `POST /embed/image` - image embeddings (base64 input)
  - `GET /health` - health check
- Runs on port 9001, communicates with backend via REST

**Vector Database**:
- Chroma DB for storing and querying embeddings
- Collection: `sneakers-collection`
- Stores product embeddings and chat message history
- Used for semantic search and AI context retrieval

**AI Configuration**:
- Controlled by `ai.enabled` flag in application.yaml
- Requires Google Cloud credentials for Vertex AI (Gemini) - set up via GOOGLE_APPLICATION_CREDENTIALS
- Embedding service URL: `http://localhost:9001` (configurable via `AI_EMBEDDING_BASE_URL`)

### Data Models (Key Entities)

- `User` (with roles: ADMIN, CUSTOMER)
- `Product` (with images, features, reviews)
- `Order` (with status, details, vouchers)
- `Cart`, `Category`, `Banner`
- `News`, `Review`, `ReturnRequest`
- `Voucher`, `LockFeature`
- Social: `SocialAccount`
- Payments: `Stripe`, `Vnpay` (entities)

## Configuration

### Backend Configuration Files

- `Backend/src/main/resources/application.yaml` - Main config
  - Server port: 8089
  - JWT secret, expiration (30 days)
  - Database, Chroma, AI, Stripe, VNPay, GHN, Facebook settings
  - Logging levels and file output

- Security config: `Backend/src/main/java/com/example/Sneakers/configurations/`
- JWT utilities: `Backend/src/main/java/com/example/Sneakers/components/JwtTokenUtils.java`

### Environment Overrides (Docker)

Docker Compose sets environment variables:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `CHROMA_BASE_URL`
- `AI_EMBEDDING_BASE_URL`

### Frontend Environment

No environment-specific configs; API calls proxied through Angular CLI proxy configuration in `angular.json`. External API (provinces) is configured in `proxy.conf.json`.

## Testing Strategy

- **Unit tests** (Mockito): Service layer business logic (17 tests)
- **Integration tests** (H2): Full service + repository integration (18 tests)
- **Frontend tests**: Karma + Jasmine for component/service tests
- **Coverage**: JaCoCo for backend (runs with `mvn test`)

To run specific tests:
```bash
# Backend unit test
.\mvnw test -Dtest=ProductServiceTest

# Backend integration test
.\mvnw test -Dtest=ProductServiceIntegrationTest

# Run all integration tests only
.\mvnw test -Dtest=**/integration/*

# Frontend
cd Frontend && npm test
```

## Important Notes

1. **Database Migrations**: Hibernate DDL auto is set to `none`. Schema managed via SQL scripts (see `lockerkorea.sql`). Initial schema is auto-loaded on first Docker MySQL container start.

2. **File Uploads**: Backend handles uploads to `uploads/` and `Image_upload/` directories (relative to working directory). Configure max file size in `spring.servlet.multipart` settings.

3. **AI Features**: 
   - Controlled by `ai.enabled` flag; requires Google Cloud credentials for Vertex AI
   - Set `GOOGLE_APPLICATION_CREDENTIALS` environment variable pointing to service account JSON
   - Python embedding service must be running for AI features to work
   - Chroma DB must be accessible (default: `http://localhost:8000`)

4. **Payment Gateways**: All in test mode by default. Replace API keys in `application.yaml` for production use.

5. **SECURITY WARNING**: 
   - `Backend/src/main/resources/application.yaml` contains **real credentials and API keys** (Gmail, Stripe, VNPay, GHN, Facebook, database passwords)
   - **NEVER commit these values** to version control
   - Use environment variables or externalized configuration for production
   - Consider using Spring Boot's profile-specific configurations (`application-prod.yaml`)
   - The `docker-start.ps1` script uses separate environment injection for Docker containers

6. **Ports**:
   - Backend: 8089
   - Frontend (dev): 4200, (Docker): 80
   - MySQL: 3308 (host) / 3306 (container)
   - Chroma: 8000
   - Python embedding: 9001
   - phpMyAdmin: 8100

## Code Style Conventions

- **Java**: Lombok for boilerplate (`@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`); package names lowercase (`com.example.Sneakers.*`); service interfaces prefixed with `I` (e.g., `IUserService`)
- **Angular**: TypeScript strict mode; components use `OnPush` change detection where applicable; RxJS for reactive state
- **Python**: PEP 8 compliance; type hints used in FastAPI service; async/await for I/O operations
- **Naming**: Services end with `Service`; Repositories end with `Repository`; Controllers end with `Controller`; DTOs end with `Request`/`Response`
- **DTOs**: Located in `dtos/` package; clear separation between request and response objects
- **Entity Auditing**: `BaseEntity` with `createdAt`, `updatedAt` timestamps (auto-managed by JPA listeners)
- **Event-Driven**: Product events via `ProductEventListener`; async order processing via `AsyncOrderService`

## Troubleshooting

### Backend won't start
- Check MySQL is running (port 3308) and accessible
- Verify `application.yaml` credentials match docker-compose settings
- Check logs: `logs/sneakers-app.log` (if running locally) or `docker-compose logs backend`
- Ensure no other process is using port 8089

### Frontend CORS or API errors
- Development server uses Angular CLI proxy - ensure `npm start` is used (not `ng serve --open`)
- For direct API calls, backend CORS is configured in `CorsConfig.java`
- Verify backend is running on port 8089 before starting frontend
- Check that proxy configuration in `angular.json` includes all backend API routes

### Tests fail with DB errors
- Integration tests use H2 automatically (profile `test`)
- Ensure no leftover data in MySQL that might conflict
- Check `src/test/resources/application-test.yaml` if present
- Run `.\mvnw clean test` to ensure fresh state

### Embedding service errors
- Verify Python dependencies installed: `pip install -r requirements.txt`
- PyTorch must be installed separately (see README in python-embedding-service)
- Check Chroma DB is running: `http://localhost:8000`
- Model downloads on first run may take time (CLIP model ~500MB)
- Verify service is running on port 9001: `curl http://localhost:9001/health`

### AI features not working
- Check `ai.enabled` flag is `true` in application.yaml
- Verify Google Cloud credentials are set up (service account JSON)
- Ensure embedding service is running and reachable
- Check Chroma DB connection and collection existence

## Helpful Commands Reference

```bash
# View backend logs (if running in Docker)
docker-compose logs -f backend

# Restart a specific service
docker-compose restart backend

# Stop all
docker-compose down

# View MySQL data
# Open http://localhost:8100, login: root / admin, server: mysql_db:3306

# Database SQL file
# Backend/lockerkorea.sql (import via phpMyAdmin)

# Generate UML diagrams (run from project root)
python scripts/generate_plantuml.py
python scripts/generate_vp_uml.py
python scripts/generate_xmi.py

# Rebuild Docker images (after code changes)
docker-compose build
docker-compose up -d

# Check service health
curl http://localhost:8089/api/v1/health  # Backend
curl http://localhost:9001/health        # Embedding service
curl http://localhost:8000/api/v1/heartbeat  # Chroma DB
```

## Git Workflow

- Main branch: `main`
- Feature branches: `feature/<name>`, `bugfix/<name>`
- Commit style: Conventional Commits recommended
- Tests required before committing (run locally first)
