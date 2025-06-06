# Simple Library Service

## How to set up in local
1. Setup Java 17 in IDE
2. Run SimpleLibraryApplication with default (h2) spring profile
3. Verify APIs using swagger-ui: http://localhost:8080/swagger-ui/index.html
4. If H2 database is using, refer database console: http://localhost:8080/h2-console

## How to set up in upper environment
1. Setup Postgres database
2. Create user and database
   3. CREATE DATABASE simple-library; 
   4. CREATE USER library-admin WITH PASSWORD 'your_db_password'; 
   5. GRANT ALL PRIVILEGES ON DATABASE simple-library TO library-admin;
3. Configure postgres connections details via environment variables 
   4. SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/simple-library 
   5. SPRING_DATASOURCE_USERNAME: library-admin
   6. SPRING_DATASOURCE_PASSWORD: your_db_password
4. Override active spring profile when run the application
   5. ex: java -jar -Dspring.profiles.active=prod simple-library.jar
