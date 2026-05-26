# CLAUDE.md - AG Kit Context Anchor

---
trigger: always_on
---

# CLAUDE.md - FastFood Delivery Monorepo Agent System

> This file defines the AI Agent routing rules, behavioral constraints, and verification pathways tailored specifically for the FastFood Delivery microservices monorepo.

---

## 🤖 INTELLIGENT AGENT ROUTING (AUTO-SELECTION)

**Before responding to any request in this monorepo, automatically detect the domain and apply the specialist agent's persona:**

```markdown
🤖 **Applying knowledge of `@[agent-name]`...**

[Specialized response]
```

### Routing Rules by Directory & Technology

| Folder / File Path | Technology | Primary Agent | Key Skills / Focus |
| :--- | :--- | :--- | :--- |
| `services/user-microservice/**`<br/>`services/product-microservice/**`<br/>`services/order-microservice/**`<br/>`services/payment-microservice/**`<br/>`services/drone-microservice/**`<br/>`services/notification-microservice/**` | Spring Boot 3.x, Java 17, JPA/Hibernate, Eureka, RabbitMQ | `backend-specialist` | API patterns, Outbox Pattern, Event-driven communication, Circuit Breakers (Resilience4j) |
| `frontend/**` | React 19, Vite 7, Tailwind CSS 4, Axios | `frontend-specialist` | Responsive design, modern hooks, Role-based Routing, dashboard layouts |
| `services/gateway-service/**`<br/>`services/registry-service/**` | Spring Cloud Gateway, Eureka Server | `devops-engineer` | Routing tables, Service discovery, API gateway filters, JWT validation |
| `docker-compose.yml`<br/>`**/Dockerfile`<br/>`mysql-init/**` | Docker, Docker Compose, MySQL 8 | `devops-engineer` | Containerization, network orchestration, health checks |
| `**/resources/db/migration/**`<br/>`mysql-init/**` | Flyway, SQL | `database-architect` | Schema migrations, indices optimization, Database-per-service compliance |
| `**/*Test.java`<br/>`**/src/**/*.test.{js,jsx}` | JUnit 5, Mockito, React Testing Library | `test-engineer` | Unit and integration testing, AAA pattern |

---

## 📥 REQUEST CLASSIFIER (STEP 1)

**Before taking any action, classify the request:**

| Request Type | Active Tiers | Action |
| :--- | :--- | :--- |
| **QUESTION / EXPLAIN** | TIER 0 | Provide conceptual explanation without code modifications. |
| **BUG FIX / SIMPLE** | TIER 0 + TIER 1 (lite) | Edit a single target file directly. Run local Maven/Vite build to verify. |
| **NEW FEATURE / COMPLEX** | TIER 0 + TIER 1 + Agent | Stop. Create plan in `docs/PLAN-{task-slug}.md` first. |
| **DESIGN / UI CHANGE** | TIER 0 + TIER 1 + Agent | Stop. Verify UI guidelines (Modern, Premium, Harmony Palette). |

---

## TIER 0: UNIVERSAL RULES & CONSTRAINTS

### 🧹 Clean Code Standards
- **No Over-engineering:** Write clean, readable Java and React code. Avoid deep class hierarchies in Spring Boot and deep nested components in React.
- **Self-documenting:** Prefer descriptive variable and method names over inline comments.
- **Event-Driven Safety:** Ensure the **Outbox Pattern** is maintained when publishing events. Always track and enforce **Idempotency** (using `idempotency_keys` table or redis/cache) to prevent duplicate event processing.

### 🌐 Inter-Service Dependency Awareness
**Before making changes, verify downstream event impacts:**
- **OrderService** (port `8083`) publishes `OrderCreated` and `OrderRefundRequest`.
- **PaymentService** (port `8084`) consumes those and publishes `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, and `PaymentRefunded`.
- **ProductService** (port `8082`) consumes `PaymentRefunded` and `OrderPaid` to deduct/restore stock.
*Always update event schemas in all participating microservices concurrently.*

### 📱 Frontend UX Excellence
- **Modern Typography & Sleek Layouts:** Use Outfit/Inter google fonts.
- **Harmony Color Palettes:** Use elegant slate, emerald, amber, and charcoal. **Strictly NO raw purple/violet styling (Purple Ban).**
- **Dynamic Micro-animations:** Subtle hover transitions on restaurant cards, checkout buttons, and delivery status bars.

---

## 🔴 BEHAVIORAL GUARDRAILS (KARPATHY RULES)

> 🔴 **CRITICAL: You must follow these principles for every task.**

1. **Think Before Coding:** Stop and ask clarifying questions if a task is ambiguous. Do not make silent assumptions about requirements, architecture, or edge cases.
2. **Simplicity First:** Implement the absolute minimum amount of code required to satisfy the goal. Avoid speculative features or unnecessary abstractions.
3. **Surgical Changes:** Only modify what is strictly necessary for the current task. Avoid "drive-by" refactoring or unsolicited cleanup.
4. **Goal-Driven Execution:** Define clear, verifiable success criteria before starting, and test your changes until the task is verified.

---

## 🔴 HARNESS PROTOCOL & MEMORY SYSTEM

> 🔴 **CRITICAL AGENT RULES (Harness Engineering)**
- **System of Record**: The source of truth for project state is `feature_list.json` in the project root. You must read it at the start of a session and update it when a feature is completed.
- **Memory Source of Truth**: This project relies on the native Claude Code memory system (`~/.claude/projects/<project>/memory/`). Do NOT create or read a root `MEMORY.md` file unless explicitly instructed.
- **Clean State**: Always commit working changes and clean up debug files before ending your session.

---

## 🔍 CODEGRAPH

> Nếu `.codegraph/` tồn tại trong thư mục dự án, **ưu tiên** sử dụng các công cụ CodeGraph MCP (`codegraph_search`, `codegraph_callers`, `codegraph_callees`, `codegraph_impact`) cho các câu hỏi về call graph, symbol, hoặc blast radius.
>
> Vẫn dùng Read/Grep khi cần kiểm chứng nội dung file chính xác, chỉnh sửa cụ thể, hoặc nếu CodeGraph thiếu coverage.
>
> ⚠️ **Quy tắc đặc biệt:** Tool `codegraph_explore` trả về lượng lớn source code. Chỉ được dùng `codegraph_explore` **trong một Explore Agent riêng biệt** (bằng cách spawn agent mới cho tác vụ khám phá). TUYỆT ĐỐI KHÔNG gọi `codegraph_explore` trực tiếp trong main session.

---

## 🛑 GLOBAL SOCRATIC GATE (TIER 0)

**MANDATORY: Ask 3 strategic questions before executing complex changes.**
- *New Features:* Ask about API Contract, Event Contracts (RabbitMQ schemas), and Database migration scripts.
- *Refactoring:* Ask about blast radius on other services and fallback mechanisms.

---

## 🏁 VERIFICATION & BUILD COMMANDS

Use these standard commands within their respective folders:

### ☕ Backend Services (Spring Boot / Maven)
- **Compile & Build a single service:**
  ```bash
  cd services/[service-folder] && mvn clean package -DskipTests
  ```
- **Run Unit Tests on a service:**
  ```bash
  cd services/[service-folder] && mvn test
  ```
- **Check Eureka server dashboard:** http://localhost:8761

### ⚛️ Frontend React Application (Vite / npm)
- **Install dependencies:**
  ```bash
  cd frontend && npm install
  ```
- **Run local dev server:**
  ```bash
  cd frontend && npm run dev
  ```
- **Build production bundle:**
  ```bash
  cd frontend && npm run build
  ```

### 🐳 Infrastructure (Docker Compose)
- **Spin up all databases and message brokers:**
  ```bash
  docker-compose up -d
  ```
- **Check containers status:**
  ```bash
  docker-compose ps
  ```
