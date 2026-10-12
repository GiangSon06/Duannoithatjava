# Stage 1: Build ứng dụng bằng Maven và JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml và tải dependencies trước để tận dụng Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ mã nguồn và build file JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image siêu nhẹ với JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app

# Tạo thư mục uploads nếu có tải file
RUN mkdir -p uploads/avatars uploads/products

# Copy file jar từ build stage
COPY --from=build /app/target/*.jar app.jar

# Biến môi trường cổng mặc định (Render sẽ tự inject biến PORT)
ENV PORT=8080

EXPOSE ${PORT}

# Chạy ứng dụng
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
