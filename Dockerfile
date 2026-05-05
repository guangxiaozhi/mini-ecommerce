# 多阶段：1) 构建 Vite  2) Maven 打包  3) 只带 JRE 运行
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:17-jdk-alpine AS backend-build
WORKDIR /app
COPY backend/ ./backend/
# 让 Spring Boot 在运行时从 classpath:/static 提供 React 构建产物
COPY --from=frontend-build /app/frontend/dist/ ./backend/src/main/resources/static/
RUN chmod +x ./backend/mvnw
WORKDIR /app/backend
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
COPY --from=backend-build /app/backend/target/mini-ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar
# Render 会注入 PORT；application-prod 里用 server.port: ${PORT:8080} 即可
CMD ["java", "-jar", "/app/app.jar"]