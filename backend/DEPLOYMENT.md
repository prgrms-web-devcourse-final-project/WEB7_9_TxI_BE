# 운영 환경 배포 가이드

## 필수 환경변수

prod 프로필로 실행하기 위해 다음 환경변수들을 설정해야 합니다:

### 데이터베이스
```bash
DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

### Redis
```bash
REDIS_HOST=<redis-host>
REDIS_PORT=<redis-port>
REDIS_PASSWORD=<redis-password>  # 필요시
```

### JWT
```bash
JWT_SECRET=<jwt-secret-key>
JWT_ACCESS_TOKEN_DURATION=3600
JWT_REFRESH_TOKEN_DURATION=1209600
```

### 도메인 설정
```bash
API_BASE_URL=<your-domain.com>  # 예: example.com
FRONTEND_URL=<frontend-url>     # 예: https://www.example.com
```

## 프로필별 특징

### dev (개발)
- DB: 로컬 PostgreSQL
- DDL: update (자동 스키마 업데이트)
- 로그: 상세 (SQL, 트랜잭션 TRACE)
- MockPaymentClient 사용

### perf (성능 테스트)
- DB: waitfair_perf 데이터베이스
- DDL: create (매번 스키마 재생성)
- 로그: 최소화 (INFO)
- Actuator: 모든 엔드포인트 노출
- MockPaymentClient 사용

### prod (운영)
- DB: 환경변수로 지정
- DDL: validate (스키마 검증만)
- 로그: 최소화 (INFO/WARN)
- Actuator: health, prometheus만 노출
- **실제 PaymentClient 구현 필요** ⚠️

## 주의사항

### 1. PaymentClient 구현 필요
운영 환경에서는 MockPaymentClient가 비활성화됩니다.
실제 결제 API(토스페이먼트 등) 연동을 위한 PaymentClient 구현체를 추가해야 합니다.

예시:
```java
@Component
@Profile("prod")
public class TossPaymentClient implements PaymentClient {
	// 실제 토스페이먼트 API 연동 구현
}
```

### 2. 데이터베이스 마이그레이션
운영 환경에서는 `ddl-auto: validate`로 설정되어 있어 스키마를 자동으로 변경하지 않습니다.
Flyway나 Liquibase 같은 마이그레이션 도구 사용을 권장합니다.

### 3. CORS 설정
운영 환경에서는 `FRONTEND_URL` 환경변수로 허용할 프론트엔드 도메인을 지정하세요.

### 4. Redis 설정
운영 환경 Redis가 비밀번호를 사용하는 경우 `REDIS_PASSWORD` 환경변수를 설정하세요.

## 실행 방법

```bash
# 프로필 지정하여 실행
java -jar -Dspring.profiles.active=prod backend.jar

# 또는 환경변수로 지정
export SPRING_PROFILES_ACTIVE=prod
java -jar backend.jar
```

## Health Check

```bash
# Health 체크
curl http://localhost:8080/actuator/health

# Prometheus 메트릭
curl http://localhost:8080/actuator/prometheus
```