# 🌐 GeekForge Backend (Java)

<div align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-3.1-green?logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-17-blue?logo=openjdk" alt="Java 17">
  <img src="https://img.shields.io/badge/Security-JWT%2BreCAPTCHA-red" alt="Security">
  <img src="https://img.shields.io/badge/Architecture-Clean%20Layers-brightgreen" alt="Architecture">
</div>

## 📖 Table of Contents
- [✨ Project Advantages](#-project-advantages)
  - [🔒 Enhanced Security Features](#-enhanced-security-features)
  - [🏗️ Clean Architecture and Maintainability](#️-clean-architecture-and-maintainability)
- [✨ Key Features](#-key-features)
  - [🔒 Security](#-security)
  - [🏗️ Architecture](#️-architecture)
  - [⚙️ Operational](#️-operational)
  - [🚀 Highlights](#-highlights)


## ✨ Project Advantages

### 🔒 Enhanced Security Features
- **JWT-based Authentication**  
  Stateless auth with JSON Web Tokens
- **Account Locking**  
  Brute-force protection with failed attempt tracking
- **reCAPTCHA Integration**  
  Bot protection for registration/login
- **Password Hashing**  
  BCrypt for secure password storage
- **Role-Based Access Control**  
  Fine-grained permissions (ROLE_USER, ROLE_ADMIN)

### 🏗️ Clean Architecture and Maintainability
**Project Structure:**
- `config/` - Spring configurations
- `controller/` - API endpoints  
- `dto/` - Data Transfer Objects  
- `entity/` - JPA entities  
- `exception/` - Custom exceptions  
- `repository/` - Database access  
- `service/` - Business logic
- `utils/` - Utils logic  

## ✨ Key Features

### 🔒 Security
- **JWT Authentication** - Securely authenticates users using JSON Web Tokens, providing a stateless and scalable authentication mechanism.
- **Brute Force Protection** - Account locking after N failed attempts  
- **reCAPTCHA v2** - Bot prevention on auth endpoints  
- **BCrypt Password Hashing** - Secure credential storage  
- **RBAC** - Role-based access (USER/ADMIN) with Spring Security  
- **HttpOnly Cookies** - XSS protection for JWT storage  

### 🏗️ Architecture  
- **Clean Layers** - Controller → Service → Repository pattern  
- **Spring Boot 3.4.4** - Auto-config, DI, Actuator  
- **DTO Pattern** - Secure data transfer between layers  
- **Custom Exceptions** - Unified error handling with `@ControllerAdvice`  
- **JPA/Hibernate** - ORM with PostgreSQL support  
- **Query Optimization** - Pagination, caching, indexed queries  

### ⚙️ Operational
- **RESTful API** - HTTP standards compliant  
- **SLF4J Logging** - Structured logs with MDC support  
- **Stateless** - JWT-based session management  
- **Cloud Ready** - 12-factor app configuration  

### 🚀 Highlights
- JWT auth flow with refresh tokens  
- reCAPTCHA-protected registration  
- Role-based endpoint security  
- Optimized DB queries with Hibernate
