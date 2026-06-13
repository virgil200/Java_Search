# Multi-stage Docker image for the Java public-records due-diligence website.
# Build: docker build -t public-records-search .
# Run:   docker run --rm -p 8080:8080 public-records-search

FROM eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY src ./src
RUN javac src/PublicRecordsSearchServer.java

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/src/*.class ./src/
COPY public ./public
ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "java -cp src PublicRecordsSearchServer ${PORT}"]
