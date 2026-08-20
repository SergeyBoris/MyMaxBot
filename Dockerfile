# Этап сборки - используем полный JDK образ
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Копируем pom.xml и загружаем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходники и собираем
COPY src ./src
RUN mvn clean package -DskipTests

# Финальный этап - только JRE для запуска
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache bash curl

WORKDIR /app

# Копируем JAR из этапа сборки
COPY --from=builder /app/target/MyMaxBot-2.1U-jar-with-dependencies.jar app.jar

# Создаём папки
RUN mkdir -p /app/logs

# Копируем конфиг (если есть)
COPY config.json /app/
COPY BD.json /app/
COPY pbfConfig.json /app/
COPY pfiConfig.json /app/

ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV HEADLESS=true

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]