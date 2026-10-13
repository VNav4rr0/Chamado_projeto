FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY domain/pom.xml domain/
COPY spring/pom.xml spring/
COPY domain/src domain/src
COPY spring/src spring/src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/spring/target/*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
