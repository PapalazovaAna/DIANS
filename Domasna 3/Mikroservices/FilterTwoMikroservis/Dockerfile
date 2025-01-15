# Користиме Maven за билд и стартување
FROM maven:3.9.0-eclipse-temurin-17

# Работна директорија во контејнерот
WORKDIR /app

# Копирај го целиот код во контејнерот
COPY . .

# Команда за стартување на Spring Boot со Maven
CMD ["mvn", "spring-boot:run"]

# Експонирај го портот на микросервисот
EXPOSE 8082
