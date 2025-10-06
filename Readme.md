# Task Tracker Application

A secure, microservices-based application for managing tasks with a robust, multi-layered security model. The project is built with Scala, Play Framework and gRPC, including JWT-based authentication, Role-Based Access Control (RBAC), and encrypted inter-service communication with mTLS.
---

## Table of Contents

- [Features](#features)
    - [Cron Job for Due Tasks](#cron-job-for-due-tasks)
- [Architecture](#architecture)
    - [Services](#services)
    - [Data Model](#data-model)
- [Security Model](#security-model)
    - [User Authentication (JWT)](#user-authentication-jwt)
    - [Authorization (RBAC & Ownership)](#authorization-rbac--ownership)
    - [Inter-Service Security (mTLS)](#inter-service-security-mtls)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
    - [1. Clone the Repository](#1-clone-the-repository)
    - [2. Configure Environment](#2-configure-environment)
    - [3. Generate TLS Certificates](#3-generate-tls-certificates-for-mtls)
    - [4. Build and Run](#4-build-and-run)

---

## Architecture

The application follows a microservice architecture, with distinct services for handling core business logic and notifications.

### Services

*   **`tasks-service`**
    *   **Technology**: Play Framework, Scala, gRPC.
    *   **Responsibility**: The core of the application. It provides REST API for creating, updating and searching tasks based on completion status on tasks. It is also responsible for scheduling and triggering notifications.

*   **`notification-service`**
    *   **Technology**: Scala, gRPC.
    *   **Responsibility**: A gRPC server that listens for notification requests from the `tasks-service`. When a task is due, this service receives the details, logs a confirmation to the console, and saves a record of the notification to its own database (`notificationdb`).

*   **`notification-proto`**
    *   **Purpose**: This directory contains the Protocol Buffers (`.proto`) definition file. It acts as the strict contract for communication between the `tasks-service` (gRPC client) and the `notification-service` (gRPC server), ensuring type safety and a clear API.

*   **`mysql`**
    *   **Purpose**: The primary database instance. It hosts two separate schemas: one for the `tasks-service` and another for the `notification-service`.

### Data Model

The application uses two separate database schemas, one for each service, to ensure loose coupling.

#### `tasks-service` Schema (`taskdb`)

This schema contains the core entities for users, roles, and tasks.

**User Table (`users`)**
Stores user account information.

| Field          | Type          | Description                                     |
|----------------|---------------|-------------------------------------------------|
| `id`           | Long          | The unique identifier for the user.             |
| `username`     | String        | The user's unique login name.                   |
| `email`        | String        | The user's unique email address.                |
| `passwordHash` | String        | The securely hashed password for the user.      |
| `createdAt`    | LocalDateTime | The timestamp when the user account was created. |
| `updatedAt`    | LocalDateTime | The timestamp when the account was last updated. |


**Role Table (`roles`)**
Defines the roles available in the system (e.g., `ADMIN`, `USER`).

| Field         | Type          | Description                                  |
|---------------|---------------|----------------------------------------------|
| `id`          | Long          | The unique identifier for the role.          |
| `name`        | String        | The name of the role (e.g., "ADMIN").        |
| `description` | String        | A brief description of the role's permissions. |
| `createdAt`   | LocalDateTime | The timestamp when the role was created.     |
| `updatedAt`   | LocalDateTime | The timestamp when the role was last updated.  |


**User-Role Join Table (`user_roles`)**
A many-to-many link table that assigns roles to users.

| Field       | Type          | Description                                  |
|-------------|---------------|----------------------------------------------|
| `userId`    | Long          | Foreign key referencing the `users` table.   |
| `roleId`    | Long          | Foreign key referencing the `roles` table.   |
| `createdAt` | LocalDateTime | The timestamp when the role was assigned.    |


**Task Table (`tasks`)**
Stores the tasks created by users.

| Field       | Type          | Description                                    |
|-------------|---------------|------------------------------------------------|
| `id`        | Long          | The unique identifier for the task.            |
| `title`     | String        | A short description of the task.               |
| `dueDate`   | LocalDateTime | The date and time the task is due.             |
| `status`    | String        | A flag indicating task is PENDING/COMPLETED    |
| `notified`  | Boolean       | A flag indicating due task is notified/not.    |
| `createdAt` | LocalDateTime | The date and time the task was created.        |
| `updatedAt` | LocalDateTime | The date and time the task was last updated.   |
| `ownerId`   | Long          | Foreign key referencing the `users` table to indicate the owner. |


#### `notification-service` Schema (`notificationdb`)

This schema stores a historical record of all notifications sent.

When the `notification-service` receives a gRPC request for a due task, it creates and saves a `Notification` object in its `notificationdb` database.

**Notification Table (`notifications`)**

| Field       | Type          | Description                                          |
|-------------|---------------|------------------------------------------------------|
| `id`        | Long          | The unique identifier for the notification.          |
| `taskId`    | Long          | The ID of the task this notification is for.         |
| `taskTitle` | String        | The title of the task at the time of notification.   |
| `dueDate`   | LocalDateTime | The due date of the task.                            |
| `createdAt` | LocalDateTime | The date and time the notification was created.      |


---

## Security Model

The application implements a multi-layered security strategy to protect user data and secure inter-service communication.

### User Authentication (JWT)

Access to the API is controlled via JSON Web Tokens (JWT). The authentication flow is as follows:

1.  **Registration & Login**: A new user can register via the `POST /auth/register` endpoint. An existing user can log in via `POST /auth/login`.
2.  **Token Generation**: Upon successful login, the server generates a JWT signed with the **HS256** algorithm (HMAC with SHA-256) using a secret key. This token contains claims such as `userId` and `roles`, which are used for authorization checks.
3.  **Authenticated Requests**: To access protected endpoints, the client must include this token in the `Authorization` header of every request, using the `Bearer` scheme:
    ```
    Authorization: Bearer <your_jwt_token>
    ```

### Authorization (RBAC & Ownership)

Once a user is authenticated, the system uses two layers of authorization to determine what they can access.

*   **Role-Based Access Control (RBAC)**:
    The system defines two roles with distinct permissions. When a new user registers, they are automatically assigned the `USER` role.
    *   **`USER`**: The standard role. Users can create, read, update, and delete **only their own tasks**.
    *   **`ADMIN`**: A privileged role. Admins can perform any action on **any user's tasks**, giving them full oversight of the system.

*   **Ownership-Based Authorization**:
    This is the core security principle for standard users. For any request that targets a specific task (e.g., `PUT /tasks/:id`), the API verifies that the `ownerId` of the task matches the `userId` of the authenticated user from the JWT. If they do not match, the request is denied with a `403 Forbidden` error.

### Inter-Service Security (mTLS)

Communication between the `tasks-service` and `notification-service` is secured using **mutual TLS (mTLS)**. This ensures that only trusted services can communicate with each other and that all data exchanged between them is encrypted.

1.  **Certificate Authority**: A local Root Certificate Authority (CA) is created using `OpenSSL`. This CA is the single source of trust for our internal services.
2.  **Service Certificates**: Each service (`tasks-service` and `notification-service`) is issued its own unique TLS certificate and private key, signed by our local CA.
3.  **Mutual Authentication**: When the `tasks-service` (client) initiates a gRPC call, a two-way handshake occurs:
    *   The `tasks-service` presents its certificate to the `notification-service`. The `notification-service` verifies it against the CA.
    *   Simultaneously, the `notification-service` presents its certificate to the `tasks-service`, which also verifies it against the same CA.
4.  **Encrypted Channel**: Only if both services successfully verify each other's identity is the connection established. All subsequent gRPC traffic is fully encrypted.

This mTLS setup prevents unauthorized services on the network from calling the `notification-service` or snooping on the traffic between services.

---

## Features

### Cron Job for Due Tasks

The `tasks-service` includes a built-in scheduler (cron job) that runs periodically to check for tasks that are due soon.

1.  The scheduler queries the database for tasks whose `dueDate` is approaching within the next 10 minutes.
2.  For each due task, it makes a gRPC call to the `notification-service`.
3.  The `notification-service` receives the request, logs a message to the console (e.g., `Saved notification for task 'Check project demo'`), and persists the notification record in its database table.

---

## Prerequisites

*   **Git**: To clone the repository.
*   **Docker and Docker Compose**: For running the application containers. [Install Docker Desktop](https://www.docker.com/products/docker-desktop/).
*   **Java Development Kit (JDK)**: Version 11 or higher.
*   **sbt**: For building the `notification-service` and `notification-proto`. Install sbt.

---

## Installation & Setup

Follow these steps to get the entire application stack running on your local machine.

### 1. Clone the Repository

Open your terminal and clone the project to your local machine.

```sh
git clone https://github.com/VishalKumar-S/task-tracker.git
cd task-tracker
```


### 2. Configure Environment

Create a file named `.env` in the root directory of the project and add all the environment variables required in docker-compose.yml file. Replace the placeholder values with your desired credentials.


### 3. Generate TLS Certificates for mTLS

Run the provided script to generate a local Certificate Authority (CA) and signed certificates for both services.

```sh
# Make the script executable (on Linux/macOS/Git Bash)
chmod +x generate-certs.sh

# Run the script
./generate-certs.sh
```

This will create the necessary `.crt` and `.key` files in the appropriate `certs` directories, which are required for mTLS.


### 4. Build and Run

Execute the following commands from the root directory of the project. These steps will build local Docker images for each service and then start the entire application stack.

```sh
# 1. Build the gRPC protocol definition (used by both services)
cd notification-proto
sbt publishLocal
cd ..

# 2. Build the notification-service Docker image
cd notification-service
sbt docker:publishLocal
cd ..

# 3. Build the tasks-service Docker image 
cd tasks-service
sbt docker:publishLocal
cd ..

# 4. Start all services using Docker Compose
docker-compose up --build
```

After these steps, the application will be running. The tasks-service API is available at http://localhost:9000, and the notification-service is listening for gRPC calls on port 50051.
