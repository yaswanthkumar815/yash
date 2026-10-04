# Stage 1: Build the Java 21 application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY src ./src
COPY static ./static

# Compile Java source code into bytecode
RUN mkdir bin && javac -encoding UTF-8 -d bin \
    src/com/taskflow/model/*.java \
    src/com/taskflow/util/*.java \
    src/com/taskflow/repository/*.java \
    src/com/taskflow/service/*.java \
    src/com/taskflow/server/*.java \
    src/com/taskflow/cli/*.java \
    src/com/taskflow/test/*.java \
    src/com/taskflow/Main.java

# Stage 2: Minimal Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/static ./static
RUN mkdir data

# Expose default HTTP port
EXPOSE 8080
ENV PORT=8080

# Run in headless server mode
CMD ["java", "-cp", "bin", "com.taskflow.Main", "--server-only"]
