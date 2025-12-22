# EC2 배포 가이드 - Grafana Alloy

## 1. 필요한 파일

EC2에 업로드할 파일:
- `config.alloy` - Alloy 설정 파일
- `docker-compose.yml` - Alloy 컨테이너 실행 설정
- `.env` - Grafana Cloud 인증 정보 (중요!)

## 2. 파일 업로드

### 로컬에서 EC2로 파일 전송:

```bash
# alloy 폴더 전체를 EC2에 업로드
scp -r perf/alloy ec2-user@<EC2-IP>:~/

# 또는 개별 파일만
scp perf/alloy/config.alloy ec2-user@<EC2-IP>:~/alloy/
scp perf/alloy/docker-compose.yml ec2-user@<EC2-IP>:~/alloy/
scp perf/alloy/.env ec2-user@<EC2-IP>:~/alloy/
```

## 3. EC2에서 실행

### Step 1: EC2 접속
```bash
ssh ec2-user@<EC2-IP>
# 또는 SSM
aws ssm start-session --target <instance-id>
```

### Step 2: Docker 확인
```bash
# Docker가 설치되어 있는지 확인
docker --version
docker-compose --version

# 실행 중인 Spring Boot 컨테이너 확인
docker ps
```

### Step 3: Alloy 실행
```bash
cd ~/alloy
docker-compose up -d
```

### Step 4: 로그 확인
```bash
# Alloy 컨테이너 로그
docker logs -f alloy

# 정상 동작 확인 메시지:
# - "now listening for http traffic"
# - "starting process loop" (컨테이너 발견)
```

### Step 5: 상태 확인
```bash
# 메트릭 확인
curl http://localhost:12345/metrics | grep loki_source_docker_target_entries_total
curl http://localhost:12345/metrics | grep loki_write_sent_entries_total

# 0이 아니면 성공!
```

## 4. EC2 주의사항

### Spring Boot 컨테이너 확인

Alloy가 수집할 컨테이너를 확인:

```bash
# 컨테이너 이름 확인
docker ps --format "{{.Names}}"

# 예상 결과:
# - waitfair-backend (또는 비슷한 이름)
# - postgres
# - redis
```

### 컨테이너 필터 설정 (선택사항)

현재 `config.alloy`는 **모든 컨테이너**의 로그를 수집합니다.

특정 컨테이너만 수집하려면 `config.alloy` 수정:

```hcl
// 30-35번째 줄 주석 제거 및 수정
rule {
  source_labels = ["__meta_docker_container_name"]
  regex         = ".*(waitfair|backend).*"  # EC2 컨테이너 이름에 맞게 수정
  action        = "keep"
}
```

수정 후 재시작:
```bash
docker-compose restart alloy
```

### Spring Boot JSON 로그 확인

EC2의 Spring Boot가 JSON 로그를 출력하는지 확인:

```bash
docker logs <backend-container-name> | tail -10
```

JSON 형식이 아니면:
- `SPRING_PROFILES_ACTIVE=perf` 또는 `prod` 환경변수 설정 필요
- 최신 이미지로 재배포 필요

## 5. 트러블슈팅

### Alloy가 재시작 반복

```bash
# 로그에서 에러 확인
docker logs alloy

# 흔한 원因:
# 1. .env 파일 누락
# 2. Grafana Cloud 인증 정보 오류
# 3. config.alloy 문법 에러
```

### 로그가 수집되지 않음

```bash
# 메트릭 확인
curl http://localhost:12345/metrics | grep loki_source_docker_target_entries_total

# 0이면:
# 1. Docker socket 권한 확인
docker exec alloy ls -la /var/run/docker.sock

# 2. 컨테이너가 실행 중인지 확인
docker ps

# 3. Alloy 재시작
docker-compose restart alloy
```

### Grafana Cloud에 로그가 안 보임

```bash
# 전송 메트릭 확인
curl http://localhost:12345/metrics | grep loki_write_sent_entries_total

# 0이면:
# 1. .env 파일의 인증 정보 확인
# 2. 네트워크 연결 확인 (EC2 보안 그룹에서 443 아웃바운드 허용)
# 3. API 토큰 권한 확인 (Logs:Write 필요)
```

## 6. 자동 시작 설정

시스템 재부팅 시 자동 시작:

```bash
# docker-compose.yml의 restart 정책 확인 (이미 설정됨)
restart: unless-stopped

# Docker 서비스 자동 시작 활성화
sudo systemctl enable docker
```

## 7. 업데이트 방법

설정 변경 시:

```bash
# 1. config.alloy 수정
vi ~/alloy/config.alloy

# 2. Alloy 재시작
cd ~/alloy
docker-compose restart alloy

# 3. 로그 확인
docker logs -f alloy
```

## 8. 제거 방법

```bash
cd ~/alloy
docker-compose down
rm -rf ~/alloy
```

## 9. 확인 체크리스트

- [ ] EC2에 alloy 폴더 업로드 완료
- [ ] .env 파일에 Grafana Cloud 인증 정보 설정
- [ ] docker-compose up -d 실행
- [ ] docker logs alloy 에서 에러 없음
- [ ] curl localhost:12345/metrics 에서 entries_total > 0
- [ ] Grafana Cloud에서 {app="waitfair"} 쿼리로 로그 확인

모두 체크되면 배포 완료! ✅
