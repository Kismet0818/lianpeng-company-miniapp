FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=cloud
ENV PORT=80

COPY app.jar /app/app.jar

EXPOSE 80
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
