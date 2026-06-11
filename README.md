# Firstclub

Spring Boot membership application.

## Prerequisites

- **Java 21**
- **Docker**

## Steps to Run

1. **Set the compile-time and runtime environment**

   Configure your IDE or shell to use Java 21 for both compilation and execution.

   ```bash
   java -version
   ```

   Ensure the output shows Java 21.

2. **Build the project**

   From the project root:

   ```bash
   mvn clean install
   ```

3. **Start MySQL and phpMyAdmin**

   Navigate to the `docker` directory and start the containers:

   ```bash
   cd docker
   docker compose up
   ```

4. **Start the application**

   Run `FirstclubApplication.java`:

   `src/main/java/com/firstclub/firstclub/FirstclubApplication.java`

   You can start it from your IDE or from the project root:

   ```bash
   ./mvnw spring-boot:run
   ```

5. **Verify the health endpoint**

   Open the following URL in your browser:

   http://localhost:8080/actuator/health

   If the application is running correctly, the health endpoint should respond successfully.
