# 多阶段构建
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# 先复制并构建common模块
COPY common/pom.xml common/pom.xml
COPY common/src common/src
RUN cd common && mvn clean install -DskipTests

# 复制当前服务的pom.xml并下载依赖
COPY user-service/pom.xml user-service/pom.xml
RUN cd user-service && mvn dependency:go-offline -B

# 复制源代码并构建
COPY user-service/src user-service/src
RUN cd user-service && mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 添加非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 从构建阶段复制jar包
COPY --from=build /workspace/user-service/target/*.jar app.jar

# 暴露端口
EXPOSE 8081

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
