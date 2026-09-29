FROM amazoncorretto:21

WORKDIR /app

COPY target/networking-lab2.jar app.jar
COPY webroot ./webroot

ENV PORT=8080
ENV STATIC_FILES_PATH=webroot
ENV APP_ENV=production

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]