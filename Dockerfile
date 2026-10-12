# Stage 1: Build ứng dụng Java Spring Boot bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image gồm MariaDB (MySQL tương thích) + JRE 17
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV PORT=8080

WORKDIR /app

# Cài đặt MariaDB Server và OpenJDK 17 Runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    mariadb-server \
    openjdk-17-jre-headless \
    dos2unix \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Cấu hình MariaDB siêu nhẹ để chạy mượt trong 512MB RAM của Render
COPY custom-my.cnf /etc/mysql/conf.d/custom-my.cnf

# Copy file jar, database schema và entrypoint script
COPY --from=build /app/target/*.jar /app/app.jar
COPY src/main/resources/db/database.sql /app/database.sql
COPY entrypoint.sh /app/entrypoint.sh

# Phân quyền thực thi và chuyển đổi format CRLF sang LF
RUN dos2unix /app/entrypoint.sh && chmod +x /app/entrypoint.sh
RUN mkdir -p uploads/avatars uploads/products

EXPOSE ${PORT}

CMD ["/app/entrypoint.sh"]
