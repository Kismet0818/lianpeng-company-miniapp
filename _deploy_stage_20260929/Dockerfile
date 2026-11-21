# ============================================
# 联朋升学 (lpsx) 后端 — 微信云托管部署镜像
# 构建阶段：Maven 3.9 + JDK 21 打包
# 运行阶段：精简 JRE 21，仅运行 jar
# ============================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 先复制 pom.xml，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests

# 复制源码并打包（跳过测试）
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 时区设置为上海
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 默认激活云环境配置（读取 application-cloud.yml）
ENV SPRING_PROFILES_ACTIVE=cloud
# 云托管默认监听端口
ENV PORT=80

# 复制构建产物（Spring Boot 可执行 fat jar）
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 80

ENTRYPOINT ["java", "-jar", "app.jar"]
