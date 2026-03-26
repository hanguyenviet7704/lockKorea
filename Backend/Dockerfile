FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mkdir -p uploads
COPY uploads* ./uploads/
RUN mvn package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/Sneakers-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/uploads uploads
EXPOSE 8089
CMD ["java", "-jar", "app.jar"]
