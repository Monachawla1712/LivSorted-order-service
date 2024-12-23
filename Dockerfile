FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/*.jar app.jar
RUN mkdir -p /usr/local/newrelic
ADD ./newrelic/newrelic.jar /usr/local/newrelic/newrelic.jar
ADD ./newrelic/newrelic.yml /usr/local/newrelic/newrelic.yml
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=${ENV}","-javaagent:/usr/local/newrelic/newrelic.jar","-jar","/app.jar"]