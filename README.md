# Humanitarian Logistics AI System 🌍🚁

[![Java CI](https://github.com/Cuong1216/Humanitarian-Logistics-AI-System/actions/workflows/main.yml/badge.svg)](https://github.com/Cuong1216/Humanitarian-Logistics-AI-System/actions/workflows/main.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.100+-blue.svg)](https://fastapi.tiangolo.com/)

> **Origin & Development Note (v2.0)** 🚀
> 
> This project originally started as an academic coursework assignment in collaboration with the initial group of authors. You can find the original source code here: [Tom984-vn/KeyEmotion_through_SocialMedia](https://github.com/Tom984-vn/KeyEmotion_through_SocialMedia).
> 
> In this current version (v2.0), the project has been independently developed and heavily refactored into a full-fledged Client-Server architecture. Key enhancements include a modern GUI (JavaFX), integration of advanced AI NLP & KNN models, and a real-time Logistics Routing system.

---

## 🎯 Our Goal
In today's age, social media has become one of the most important ways people express their feelings, opinions, and interests. Driven by this motive, we built an AI system for fast and accurate crowd emotion detection through posts, reactions, and comments on social networks like Facebook and Twitter.

Our goal is to leverage emotion detection on specific disaster-related keywords to improve the efficiency of Humanitarian Logistics. For example: *"Làng XYZ đang bị tàn phá nặng nề bởi bão Yagi và người dân đang rất cần thức ăn, nước uống ngay lập tức"* *(Village XYZ is being badly damaged by the Yagi storm and the residents desperately need food and water right away)*. Based on these signals, our system can immediately coordinate and dispatch trucks loaded with food, water, and medical supplies directly to Village XYZ.

---

## 🏗 System Architecture (C4 Model)

The system utilizes a hybrid Microservices & Modular Monolith architecture, comprising four main components communicating seamlessly.

### 1. 🖥️ Core Desktop Client (JavaFX + Spring Boot)
- **Role:** The main interface for relief coordinators.
- **Tech Stack:** Java 21, Spring Boot 3.2, JavaFX, Resilience4j, Selenium.
- **Responsibilities:** 
  - Scrapes raw social media data (crowdsourcing) via Headless Browsers.
  - Sends text data to the AI Engine for analysis via HTTP REST.
  - Fetches and renders geographical routes using the OSRM API.
  - Ensures a non-blocking UI using Spring's Event-Driven model and `CompletableFuture`.

### 2. 🧠 AI Analysis Engine (Python FastAPI)
- **Role:** The intelligent backend that evaluates distress signals.
- **Tech Stack:** Python 3.10, FastAPI, Scikit-learn (KNN), Uvicorn.
- **Responsibilities:**
  - Receives unstructured social media text.
  - Uses a Google Gemini LLM API wrapper for deep semantic understanding and emotion detection.
  - Applies a K-Nearest Neighbors (KNN) model to classify damage severity and calculate urgency scores.

### 3. 📬 Message Broker (RabbitMQ)
- **Role:** Handles high-volume, asynchronous data streams.
- **Responsibilities:** Buffers the scraped social media posts before they are processed by the AI Engine, preventing system overload during a major disaster (e.g., peak storm hours).

### 4. 🗄️ Cache Server (Redis)
- **Role:** Caches configurations and API results.
- **Responsibilities:** Reduces redundant API calls to external services (LLM, OSRM) and serves as a critical layer for the Circuit Breaker fallback mechanism when the AI Engine experiences high latency.

> **Note on Localization:** 🇻🇳 *The current codebase—including the NLP models, UI elements, and geographical defaults—is specifically tailored for the **Vietnamese language and geographical context**, aiming to assist local relief efforts within Vietnam.*

---

## 🚀 Technical Highlights

### 1. Spring Boot & JavaFX Integration
An Event-Driven architecture combines the power of Spring Boot's Dependency Injection (IoC) with the JavaFX Desktop platform. The UI remains fully non-blocking by offloading all HTTP/REST logic to background threads and updating the interface via `Platform.runLater()`.

### 2. OSRM / A* Routing (Logistics Algorithm)
Integrates the **OSRM** open-source map API (based on OpenStreetMap data) to calculate real-world routes instead of straight-line (Haversine) distances. The `RouteFinder` module is designed using the Strategy/Service pattern, making it highly extensible to other map providers like OpenRouteService (which supports `avoid_polygons` for storm avoidance).

### 3. Resilience4j Circuit Breaker
HTTP connections from the Java Client to the Python FastAPI are protected by a **Circuit Breaker**.
If the AI Server crashes or experiences high load (Error rate > 50%), the system automatically opens the circuit (OPEN state) and returns default *Fallback* results (e.g., Urgency = LOW). This guarantees that the coordinator's GUI never crashes.

### 4. Multithreading & Data Scraping (Selenium)
Utilizes Selenium WebDriverManager with Headless Browsers to continuously listen for and scrape disaster relief posts via keywords on social media in the background.

### 5. Enterprise Code Quality (MapStruct)
Utilizes **MapStruct** for automatic and type-safe Object Mapping between Entities and DTOs. This reduces boilerplate code in the service layer, resulting in cleaner and more maintainable business logic.

### 6. Automated CI/CD Pipeline (GitHub Actions)
Fully automated CI/CD pipeline executing Unit Tests for both Java (JUnit) and Python (Pytest). Upon successful validation, Docker images for both `java-backend` and `ai-engine` are automatically built and pushed to **Docker Hub**.

---

## 📂 Project Structure

```text
Humanitarian-Logistics-AI-System/
├── java_core/                      # Java Client (Spring Boot + JavaFX)
│   ├── src/main/java/.../gui       # UI (Controllers, FXML, CSS)
│   ├── src/main/java/.../ai_client # Resilience4j REST Client connecting to FastAPI
│   ├── src/main/java/.../logistics # OSRM Routing and Resource Allocation Logic
│   └── src/test/java/...           # JUnit 5 & Mockito Unit Tests
├── python_ai_engine/               # AI Backend (FastAPI + LLM)
│   ├── api/                        # REST Controllers
│   ├── models/                     # KNN & Sentiment Logic
│   └── data_collector_and_analyzer.py 
├── docker-compose.yml              # Infrastructure setup (Redis, RabbitMQ)
└── README.md                       
```

---

## 🛠 Installation & Setup Guide

### 1. Initialize Infrastructure
The project requires **RabbitMQ** and **Redis**. Run the following command in the root directory:
```bash
docker-compose up -d
```

### 2. Run the Python AI Engine
Setup the virtual environment and start the FastAPI Server:
```bash
cd python_ai_engine
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### 3. Run the Java Client (UI)
Use Maven to build and run the Spring Boot + JavaFX application:
```bash
cd java_core
mvn clean install
mvn spring-boot:run
```

---

## 🧪 Unit Testing & CI/CD
The project includes a comprehensive GitHub Actions configuration (`.github/workflows/ci.yml`) to automatically run validation on every `push` or `pull_request`.
- **Java Tests:** Executed using `JUnit 5` and `Mockito`.
- **Python Tests:** Executed using `Pytest`.
- **Docker Push:** Automatically builds and pushes updated images to Docker Hub upon passing all tests.

> **Note**: The Mockito tests simulating the Circuit Breaker short-circuiting (`FastApiRestClientTest.java`) serve as a strong proof of the software architecture's stability and resilience.
