# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
# gradle wrapper가 레포에 있다고 가정
RUN ./gradlew clean bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# bootJar 산출물 경로는 프로젝트에 따라 다를 수 있음 (build/libs/*.jar 확인)
COPY --from=build /app/build/libs/*SNAPSHOT*.jar app.jar
EXPOSE 8080
ENV TZ=Asia/Seoul
ENTRYPOINT ["java","-jar","/app/app.jar"]
