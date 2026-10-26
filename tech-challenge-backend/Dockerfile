FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -DskipTests -q

# Agente Java do New Relic (APM, distributed tracing e logs), mesma versão da
# dependência newrelic-api do pom.xml. O hash é conferido antes de seguir.
FROM eclipse-temurin:21-jdk-alpine AS newrelic
ARG NEWRELIC_VERSION=9.5.0
ARG NEWRELIC_SHA1=e0020fe1f622fd68bcaa93675efcf2230d5963e3
WORKDIR /newrelic
RUN wget -q -O newrelic.jar \
      "https://repo1.maven.org/maven2/com/newrelic/agent/java/newrelic-agent/${NEWRELIC_VERSION}/newrelic-agent-${NEWRELIC_VERSION}.jar" \
 && echo "${NEWRELIC_SHA1}  newrelic.jar" | sha1sum -c -

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=newrelic /newrelic/newrelic.jar /app/newrelic/newrelic.jar
COPY newrelic/newrelic.yml /app/newrelic/newrelic.yml
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Com NEW_RELIC_LICENSE_KEY (Secret do Kubernetes), sobe com o agente; sem ela
# (docker compose local), sobe sem agente e a API do New Relic vira no-op.
ENTRYPOINT ["sh", "-c", "if [ -n \"$NEW_RELIC_LICENSE_KEY\" ]; then exec java -javaagent:/app/newrelic/newrelic.jar -jar app.jar; else exec java -jar app.jar; fi"]
