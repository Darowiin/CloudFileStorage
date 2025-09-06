# ☁️ Cloud File Storage

Многопользовательское файловое хранилище с веб-интерфейсом.  
Аналог **Google Drive**, позволяющий безопасно хранить, искать и управлять файлами и папками.

---

## 🚀 Возможности

- 🔐 Регистрация и аутентификация пользователей
- 📤 Загрузка и скачивание файлов
- 🗑 Удаление файлов и папок
- 📂 Создание папок и вложенных директорий
- 🔎 Поиск файлов по названию
- ✏️ Перемещение и переименование файлов/папок
- 👀 Просмотр содержимого папок

---

## 🛠️ Технологии

### Backend
![Java](https://img.shields.io/badge/Java-21-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.2.4-brightgreen?logo=spring)
![Spring Data JPA](https://img.shields.io/badge/JPA-3.2.4-blue?logo=hibernate)  
![Liquibase](https://img.shields.io/badge/Liquibase-4.31.1-blue?logo=liquibase)
![MapStruct](https://img.shields.io/badge/MapStruct-1.6.3-blue?logo=mapstruct)
![Swagger](https://img.shields.io/badge/Swagger-UI-green?logo=swagger)

- Java 21, Spring Boot, Spring Security, Spring Data JPA
- Liquibase (миграции БД)
- MapStruct (маппинг DTO ↔ Entity)
- Swagger / OpenAPI (документация API)

### Хранилища
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-42-blue?logo=postgresql)
![MinIO](https://img.shields.io/badge/MinIO-S3-red?logo=minio)
![Redis](https://img.shields.io/badge/Redis-6.6.0-red?logo=redis)

- **PostgreSQL** – реляционная база данных
- **MinIO** – объектное S3-совместимое хранилище
- **Redis** – кеш и хранение сессий

### Тестирование
![JUnit](https://img.shields.io/badge/JUnit-5-green?logo=junit5)
![Testcontainers](https://img.shields.io/badge/Testcontainers-1.21.3-blue?logo=testcontainers)

- JUnit 5, Testcontainers (интеграционные и модульные тесты)

### Контейнеризация
![Docker](https://img.shields.io/badge/Docker-blue?logo=docker)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-blue?logo=docker)

- Docker + Docker Compose для локального запуска и деплоя

---

## 🏗 Архитектура

- **Backend** на Spring Boot взаимодействует с PostgreSQL, MinIO и Redis.
- **Frontend** (React SPA) общается с API через REST.
- Все сервисы поднимаются в Docker-контейнерах.

---

## 📂 Структура Backend

- `ru.controller` - REST контроллеры
- `ru.service` - бизнес-логика
- `ru.entity` - модели данных
- `ru.repository` - доступ к базе данных
- `ru.config` - настройки Spring компонентов
- `ru.exception` - обработка исключений
- `ru.dto` - объекты для передачи данных
- `ru.docs` - документация API (Swagger)
- `ru.util` - утилитарные классы
- `ru.security` - классы безопасности


## 📡 API

### Аутентификация

- `POST /api/auth/sign-up` - регистрация нового пользователя
- `POST /api/auth/sign-in` - вход пользователя в систему
- `POST /api/auth/sign-out` - выход из системы
- `GET /api/user/me` - информация о текущем пользователе

### 📂 Папки

- `GET /api/directory?path={path}` - получение содержимого папки
- `POST /api/directory?path={path}` - создание пустой папки

### 📄 Файлы

- `GET /api/resource?path={path}` - получение информации о файле/папке
- `POST /api/resource?path={path}` - загрузка файла/папки
- `DELETE /api/resource?path={path}` - удаление файла/папки
- `GET /api/resource/download?path={path}` - скачивание файла/папки(zip)
- `GET /api/resource/move?from={from}&to={to}` - перемещение файла/папки
- `GET /api/resource/search?query={query}` - поиск файлов и папок по запросу

## ▶️ Запуск проекта

### Предварительные требования

- Java 21
- Docker (28.3.3) и Docker Compose (2.39.2)
- Maven 4.0.0

### Запуск через Docker Compose

1. Соберите проект:
   ```
   ./mvnw clean package -DskipTests
   ```

2. Запустите приложение с помощью Docker Compose:
   ```
   docker-compose up -d
   ```

3. Приложение будет доступно по адресу: http://localhost:8080
   Frontend интерфейс: http://localhost

## 🚀Деплой доступен по адресу http://51.250.20.172

### Запуск для разработки

1. Запустите необходимые сервисы через Docker Compose:
   ```
   docker-compose up -d postgres redis minio
   ```

2. Запустите Spring Boot приложение:
   ```
   ./mvnw spring-boot:run
   ```

## 🔧Конфигурация

Основные настройки находятся в файлах:
- `application.properties` - настройки для локальной разработки
- `application-docker.properties` - настройки для запуска в Docker

## 📋Тестирование

Проект содержит тесты для основной функциональности:
- `UserTest` - тестирование функций авторизации
- `StorageTest` - тестирование операций с файлами

Для запуска тестов используйте:
```
./mvnw test
```

## 📝Лицензия

[MIT](https://choosealicense.com/licenses/mit/)
