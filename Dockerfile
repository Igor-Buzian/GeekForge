# Используем официальный образ OpenJDK для сборки (с Maven)
FROM maven:3.8.7-openjdk-17 AS build

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем pom.xml и скачиваем зависимости (чтобы не перескачивать при каждом изменении кода)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем весь остальной код
COPY src ./src

# Собираем Spring Boot приложение
RUN mvn clean install -DskipTests

# Используем более легковесный образ для запуска приложения
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR-файл из стадии 'build'
COPY --from=build /app/target/GeekForge-0.0.1-SNAPSHOT.jar spring-app.jar

# Открываем порт, который ваше Spring Boot приложение слушает (обычно 8080)
EXPOSE 8080

# Команда для запуска вашего Spring Boot приложения
CMD ["java", "-jar", "spring-app.jar"]