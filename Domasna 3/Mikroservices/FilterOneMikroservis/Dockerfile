FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/FilterOneMikroservis.jar /app/FilterOneMikroservis.jar
ENTRYPOINT ["java", "-jar", "FilterOneMikroservis.jar"]
EXPOSE 8081
