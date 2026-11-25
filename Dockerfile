# ===== MoneyOS · imagen para Render (o cualquier servidor con Docker) =====

# 1) Compilar con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# 2) Imagen final, solo con Java para ejecutar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilar /app/target/moneyos.jar moneyos.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar moneyos.jar"]
