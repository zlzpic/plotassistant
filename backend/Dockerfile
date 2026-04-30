# 基于 Eclipse Temurin Java 8 JRE，轻量且长期维护
FROM eclipse-temurin:8-jre

# 设置工作目录
WORKDIR /app

# 复制本地打好的 jar（注意：执行 build 前请确认 target 里已经有 jar）
COPY target/plotassistant-0.0.1-SNAPSHOT.jar app.jar

# 暴露 8080 端口
EXPOSE 8080

# 容器启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
