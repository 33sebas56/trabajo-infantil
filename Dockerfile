# Primera etapa: compilar el proyecto con Java 21 y Maven
FROM maven:3.9-eclipse-temurin-21 AS compilacion

WORKDIR /app

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B -DskipTests package


# Segunda etapa: ejecutar la aplicacion
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=compilacion /app/target/trabajo-infantil-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]