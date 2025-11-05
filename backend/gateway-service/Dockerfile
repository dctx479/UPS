# 多阶段构建
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# 复制gateway-service的pom.xml并下载依赖
COPY gateway-service/pom.xml gateway-service/pom.xml
RUN cd gateway-service && mvn dependency:go-offline -B

# 复制源代码并构建
COPY gateway-service/src gateway-service/src
RUN cd gateway-service && mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 添加非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 从构建阶段复制jar包
COPY --from=build /workspace/gateway-service/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
