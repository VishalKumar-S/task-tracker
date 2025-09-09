# Task Tracker Application

A microservices-based application for managing tasks and sending notifications for upcoming deadlines. The project is built with Scala, Play Framework, Akka, and gRPC, and is fully containerized with Docker.

---

## Table of Contents

- [Architecture](#architecture)
    - [Services](#services)
    - [Data Model](#data-model)
- [Features](#features)
    - [Cron Job for Due Tasks](#cron-job-for-due-tasks)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
    - [1. Clone the Repository](#1-clone-the-repository)
    - [2. Configure Environment](#2-configure-environment)
    - [3. Build and Run](#3-build-and-run)

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

A `Task` object contains the following fields:

| Field       | Type          | Description                                    |
|-------------|---------------|------------------------------------------------|
| `id`        | Long          | The unique identifier for the task.            |
| `title`     | String        | A short description of the task.               |
| `dueDate`   | LocalDateTime | The date and time the task is due.             |
| `status`    | String        | A flag indicating task is PENDING/COMPLETED    |
| `notified`  | Boolean       | A flag indicating due task is notified/not.    |
| `createdAt` | LocalDateTime | The date and time the task was created.        |
| `updatedAt` | LocalDateTime | The date and time the task was last updated.   |


When the `notification-service` receives a gRPC request for a due task, it creates and saves a `Notification` object in its `notificationdb` database.

A `Notification` object contains the following fields:

| Field       | Type          | Description                                          |
|-------------|---------------|------------------------------------------------------|
| `id`        | Long          | The unique identifier for the notification.          |
| `taskId`    | Long          | The ID of the task this notification is for.         |
| `taskTitle` | String        | The title of the task at the time of notification.   |
| `dueDate`   | LocalDateTime | The due date of the task.                            |
| `createdAt` | LocalDateTime | The date and time the notification was created.      |


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


### 3. Build and Run

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
