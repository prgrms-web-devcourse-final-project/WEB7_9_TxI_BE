# 1단계: Gradle로 Spring Boot JAR 빌드
FROM gradle:8.10.0-jdk21 AS builder
WORKDIR /app

# Gradle 설정 파일 먼저 복사 (캐시 최적화)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew

# 의존성 캐시
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# Spring Boot 실행용 JAR 생성
RUN ./gradlew bootJar --no-daemon -x test

# 두 번째 스테이지: 실행만 하는 가벼운 JRE 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app

# 첫 번째 스테이지에서 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
# 실행할 JAR 파일 지정
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]





## 1단계: Gradle로 Spring Boot JAR 빌드
#FROM gradle:8.10.0-jdk21 AS builder
#WORKDIR /app
#
## 프로젝트 전체 복사 (컨텍스트: backend/)
#COPY . .
#
## Gradle Wrapper 실행 권한 부여 후 빌드
#RUN chmod +x gradlew \
#    && ./gradlew clean bootJar --no-daemon
#
## 2단계: 실행만 하는 가벼운 JRE 이미지
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#
## 빌드 결과 JAR 복사 (libs 안에 하나만 있다고 가정)
#COPY --from=builder /app/build/libs/*.jar app.jar
#
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]