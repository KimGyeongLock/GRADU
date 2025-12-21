# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# non-root 유저 생성
RUN useradd -r -u 10001 -g root appuser

# jar 복사 + 소유권 부여
COPY --from=build --chown=appuser:root /app/build/libs/*SNAPSHOT*.jar app.jar

ENV TZ=Asia/Seoul
EXPOSE 8080

# non-root로 실행
USER appuser
ENTRYPOINT ["java","-jar","/app/app.jar"]
