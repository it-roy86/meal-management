# Instructions for Claude Code

## Language and Communication
- Always respond in Korean (모든 질문에 대한 설명과 답변은 반드시 한국어로 작성하세요).
- Keep code comments and commit messages in Korean where appropriate.

## 프로젝트 개요
- 어머니가 운영하시는 구내식당의 종이 명단(식수 인원 기록)을 디지털로 자동화하는 웹 앱.
- 대상 회사: 대박유통, (주)복수. 회사/팀별로 일자별 중식·석식 인원을 기록하고 정산에 활용.
- 이 저장소는 백엔드(Spring Boot API)만 포함. 관련 저장소:
  - 프론트엔드: `meal-management-front` (Vue.js 3 + Vite, 로컬 기본 포트 5173)
  - 개발노트: `dev-notes`
  - API 계약(요청/응답 필드)을 바꾸면 프론트엔드 저장소도 함께 확인/수정이 필요할 수 있음.

## 기술 스택
- Java 17, Spring Boot 3.5.13, Spring Security(JWT, jjwt 0.11.5), Spring Data JPA/Hibernate, PostgreSQL 16, Lombok, Apache POI 5.5.1(엑셀 생성).
- 인증은 세션 없이 완전 STATELESS. `JwtAuthenticationFilter`가 매 요청마다 토큰을 검사.
- JWT는 **httpOnly 쿠키**로 클라이언트에 전달함 (2026-09-13부터, `AuthController`). 응답 본문에는 담지 않음(XSS로 토큰 탈취 방지). `JwtAuthenticationFilter`는 `Authorization: Bearer` 헤더(Postman/test.http용)와 `token` 쿠키(프론트엔드용) 둘 다 지원. 로그아웃은 `POST /api/auth/logout`이 쿠키를 만료시켜서 처리(서버가 토큰 자체를 무효화하는 건 아님).
- **CSRF 토큰 활성화됨** (2026-09-13부터, `SecurityConfig` + `CsrfCookieFilter`). `XSRF-TOKEN` 쿠키(JS로 읽을 수 있음)와 `X-XSRF-TOKEN` 요청 헤더로 검증하는 쿠키 기반 방식 — 프론트 axios가 자동으로 처리해줌(`withXSRFToken: true`). **주의**: POST/PUT/DELETE/PATCH 요청은 `/api/auth/**` 같은 permitAll 엔드포인트도 CSRF 검사를 받음(인증 여부와 무관) — Postman/`test.http`로 수동 테스트할 때는 먼저 아무 GET 요청으로 `XSRF-TOKEN` 쿠키를 받아온 뒤, 그 값을 `X-XSRF-TOKEN` 헤더로 실어서 POST해야 함(같은 쿠키 세션 유지 필요).
- 역할(Role) 기반 접근 제어: `ADMIN`, `OPERATOR`, `VIEWER`.
  - `/api/admin/**` → ADMIN 전용
  - `/api/meal/input/**` → ADMIN, OPERATOR
  - VIEWER는 자기 회사 데이터만 조회 가능 (컨트롤러 레벨에서 `SecurityContextHolder`로 판별). 식사 기록은 이 분기가 `MealRecordController.findRecordsForCurrentUser()` 한 곳에 있고 조회 API와 엑셀 다운로드 API가 같이 씀 — **식사 기록을 조회하는 API를 새로 추가할 때도 권한 분기를 복사하지 말고 이 메서드를 재사용할 것** (한쪽만 고쳐져서 VIEWER가 다른 회사 데이터를 받아가는 일을 막기 위해서)
  - `/api/auth/**`, `/api/companies/public`은 인증 없이 허용(로그인 화면용)

## 빌드 / 실행 / 테스트
- 빌드: `./mvnw clean package`
- 로컬 실행: `./mvnw spring-boot:run` (기본은 `application.properties`, 로컬 프로파일 쓰려면 `-Dspring.profiles.active=local` 추가 → 로그 레벨 DEBUG로 상승)
- 테스트: `./mvnw test`
- API 수동 테스트는 `src/main/resources/test.http` 파일 참고/활용
- Docker로 전체 스택(프론트+백+DB) 띄우기: `docker-compose up -d` (단, `docker-compose.yml`이 `../meal-management-front`를 상대경로로 참조하므로 프론트 저장소가 형제 디렉토리에 있어야 함)

## 아키텍처 & 코드 컨벤션
- 패키지 구조: `config`, `controller`, `dto`, `entity`, `repository`, `service`, `util`
- **요청/응답 DTO는 대부분 컨트롤러 안에 static inner class로 정의**하는 방식을 씀 (예: `MealRecordController.MealRecordRequest`). 최상위 `dto` 패키지는 `Auth` 관련(`LoginRequestDto`, `LoginResponseDto`)처럼 여러 곳에서 재사용되는 경우에만 사용. 새 기능 추가 시 기존 파일의 패턴을 따를 것.
- 클래스/메서드 주석은 한국어로, "~해요" 톤의 설명형 주석 스타일을 따름 (기존 코드 참고).
- CORS 허용 오리진은 `SecurityConfig`에 `localhost:5173~5175`로 고정되어 있음 — Vite 포트가 바뀌면 이 목록도 함께 갱신 필요.
- **엑셀 파일은 백엔드에서 생성** (2026-09-30, `MealRecordExcelService`). 추후 메일 발송 기능(기획설계서 기능 05)에서 청구서 첨부로 재사용할 수 있게, 이 서비스는 "식사 기록 목록 → 엑셀 바이트 배열" 변환만 담당하고 조회 조건/권한 처리는 호출하는 쪽이 맡음. 엑셀을 수정할 때 주의할 점: 열 너비는 `autoSizeColumn()` 대신 고정값을 씀(서버에 한글 폰트가 없으면 계산이 틀어짐), 합계 행은 `SUM` 수식 + 저장 전 `evaluateAll()`로 결과를 미리 계산해둠(메일 미리보기 등에서 0으로 보이는 문제 방지). 자세한 경위는 `구내식당_웹앱_기획설계서.md` 18장.

## 로깅 컨벤션 (2026-09-30 추가)

로그만 보고도 무슨 요청/처리였는지 파악할 수 있도록, **새로 작성하거나 수정하는 컨트롤러·서비스 메서드에는 로그를 남길 것** (기존 코드를 한 번에 소급 적용할 필요는 없음 — 손대는 부분부터 점진적으로).

- 클래스에 Lombok `@Slf4j` 사용 (`DataInitializer` 기존 패턴과 동일).

### 레벨 정책 및 로그 내용

- **컨트롤러 진입점**: `log.info`로 요청 파라미터를 남기고, 처리가 끝나는 지점(성공/실패 모두)에서 다시 `log.info`로 결과를 남김 — 요청/결과를 항상 한 쌍으로 남길 것.
  ```java
  log.info("식수 인원 등록 요청: companyId={}, date={}", companyId, date);
  // ... 서비스 호출 ...
  log.info("식수 인원 등록 완료: companyId={}, date={}, count={}", companyId, date, savedCount);
  ```
- **서비스의 분기/중간 계산 등 상세 흐름**: `log.debug`.
- **예외/실패 케이스**: `log.error("...", e)` 형태로 예외 객체와 함께 남김 (메시지만 찍고 스택트레이스를 버리지 말 것).
  - **스택트레이스는 한 번만 남길 것**: 컨트롤러에서 실패를 `log.error`로 남겼으면 예외를 다시 던지지 말고 에러 응답(예: `ResponseEntity.internalServerError().build()`)을 직접 반환할 것. 이 프로젝트에는 전역 예외 처리기(`@ControllerAdvice`)가 없어서, 다시 던지면 Tomcat이 같은 스택트레이스를 한 번 더 찍음.
  - 같은 이유로 서비스는 예외를 잡아서 감쌀 때 로그를 남기지 말고 원인(`e`)만 담아서 던질 것 — 로그는 요청 정보를 아는 호출 쪽(컨트롤러)이 한 번만 남김. (적용 예: `MealRecordController`의 조회/엑셀 API, `MealRecordExcelService`)

### 운영 환경에서도 log.debug가 보임 (이미 적용되어 있음)

Spring Boot 기본 설정(root=INFO)이면 운영에서 `log.debug`가 안 보이는 게 맞지만, 이 프로젝트는 `logback-spring.xml`에 **우리 패키지만 예외로 DEBUG를 걸어둠**:
```xml
<logger name="meal_management" level="DEBUG"/>
```
`!local` 프로파일(운영 등)도 `root`는 `INFO`지만, 이 로거 설정이 별도로 적용돼서 `meal_management` 패키지의 `log.debug`는 지금도 운영 로그에 남음. **새로 설정을 추가할 필요 없음** — 이 프로젝트는 YAML 설정 파일이 없고 `application.properties` + `logback-spring.xml` 조합만 쓰므로 `application-prod.yml` 같은 파일을 만들지 말 것.

### 로그 확인 경로

- **현재(로컬, `mvnw`/IntelliJ 직접 실행)**: IntelliJ 실행 콘솔 또는 `logs/meal-management.log`에서 바로 확인. `-Dspring.profiles.active=local` 없이도 `meal_management` 패키지는 위 로거 설정 덕분에 이미 DEBUG까지 보임.
- **추후(홈서버에 Docker로 배포 시)**: `docker logs -f <컨테이너명>` (예: `meal-backend`)이 기본 확인 방법.
- 파일 로그(`logs/meal-management.log` 전체, `logs/meal-management-error.log`는 ERROR만)는 컨테이너 안 파일이라 **컨테이너가 재생성되면 사라짐**. 지금 `docker-compose.yml`의 `backend` 서비스에는 `logs/` 볼륨 마운트가 없음(확인함) — 재시작 이력이 남는 파일 로그가 필요해지면 홈서버 구성 확정 후 바인드 마운트 추가를 검토할 것 (아직 미착수, TODO).

### 공통

- 메시지는 한국어로, 어떤 요청인지 구체적으로 (엔티티/식별자/파라미터 포함).
- **비밀번호, JWT 토큰, 세션값 등 민감정보는 절대 로그에 남기지 말 것.**

## 환경 변수 / 설정
- DB 접속 정보는 환경 변수로 주입: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (미설정 시 `application.properties`의 기본값 사용 — 이 기본값은 로컬 개발용이며 운영 배포 시 반드시 환경 변수로 덮어써야 함).
- `ADMIN_PASSWORD`, `OPERATOR_PASSWORD` 등 초기 계정 비밀번호도 환경 변수로 주입 (`docker-compose.yml` 참고).
- `JWT_SECRET`: JWT 서명 키. `JwtUtil`에서 `@Value("${JWT_SECRET:...}")`로 주입받음. 값이 없으면 로컬 개발용 기본값을 쓰지만, **운영 배포 시에는 반드시 환경 변수로 별도 값을 지정**해야 함. 이 값을 바꾸면 기존에 발급된 모든 JWT 토큰이 즉시 무효화(전체 강제 로그아웃)되므로 배포 타이밍에 주의.
- `COOKIE_SECURE`: JWT 쿠키의 `Secure` 속성 여부. `AuthController`에서 `@Value("${COOKIE_SECURE:false}")`로 주입받음. 기본값 `false`는 아직 HTTPS를 안 쓰는 현재 환경 기준이고, **HTTPS 적용 후에는 반드시 `true`로 설정**해야 함(안 그러면 브라우저가 쿠키를 거부하거나, HTTPS 미적용 시 평문으로 토큰이 오갈 수 있음).
- `.env`, `logs/`, `backups/`는 `.gitignore`에 포함되어 있음 — 실제 비밀번호·시크릿 값·DB 백업 파일은 절대 커밋하지 말 것.
- `BACKUP_RETENTION_DAYS`(기본 56일), `BACKUP_INTERVAL_SECONDS`(기본 604800초=7일): DB 자동 백업(`backup.sh`, `docker-compose.yml`의 `backup` 서비스) 주기/보관기간. 미설정 시 기본값 사용.

## DB 백업 (2026-09-13 추가)
- `docker compose up` 시 `backup` 서비스가 자동으로 `pg_dump`를 주 1회 실행해서 `./backups/`(호스트 폴더, Docker 볼륨 아님)에 `.sql.gz`로 저장함. 오래된 백업은 자동 삭제됨 (`backup.sh` 참고).
- 볼륨 문제(구내식당_웹앱_기획설계서.md 13장)와 무관하게 안전하도록 일부러 볼륨이 아니라 호스트 폴더에 저장하는 설계임 — 이 방식을 바꿀 땐 이 이유를 염두에 둘 것.
- **할 일(TODO)**: 지금은 백업이 로컬(또는 홈서버) 디스크에만 저장돼서 진짜 재해(디스크 고장 등)에는 취약함. **홈서버 이전이 완료되면, 백업 파일을 구글 드라이브 등 클라우드에 주기적으로 업로드하는 기능을 추가로 개발할 것** (사용자 요청, 2026-09-13). 아직 미착수.

## 인프라 변경 예정 (2026-09 기준)
- README.md에는 "AWS Lightsail (Seoul)"로 되어 있지만, 현재 Lightsail 사용은 중지된 상태.
- 사용자가 이사 예정이며, 이사 후에는 자택에 홈서버를 구축해서 직접 배포/운영할 계획 (아직 미착수).
- 배포/인프라 관련 작업 시 Lightsail 전제로 조언하지 말 것. 홈서버 환경(포트포워딩 최소화, 리버스 프록시+HTTPS, DDNS 또는 Cloudflare Tunnel/Tailscale, DB 포트 외부 노출 금지, 백업)을 고려할 것.
- 홈서버 구축이 실제로 시작되면 README.md 인프라 섹션과 이 항목을 갱신할 것.
- 홈서버 구축이 **완료**되면 위 "DB 백업" 항목의 구글 드라이브 업로드 TODO를 진행할 것.

## 히스토리 기록 규칙 (중요)
바이브코딩 특성상 사용자가 변경 내역을 놓치고 넘어갈 수 있으므로, **의미 있는 변경을 했을 때는 요청이 없어도 `구내식당_웹앱_기획설계서.md`에 히스토리를 남길 것.**

- **기록 대상** (해당하면 남김):
  - 보안 이슈 발견/수정 (예: 시크릿 하드코딩, 인증/권한 문제)
  - 원인을 바로 알기 어려웠던 버그의 원인과 해결 방법
  - 아키텍처·설계 결정 변경 (DB 스키마, 인증 방식, 주요 로직 변경 등)
  - 인프라·배포 환경 변경 (예: 배포 대상, 환경변수 구성)
  - 그 외 "나중에 왜 이렇게 했는지 몰라서 헤맬 만한" 결정
- **기록하지 않아도 되는 것**: 오타 수정, 스타일/포맷팅, 단순 리팩터링 등 git 커밋 메시지만으로 충분한 사소한 변경
- **작성 위치/형식**: 문서의 트러블슈팅/이력 섹션(현재 "10. 트러블슈팅 & 운영 노트", "11. 보안 점검 & 인프라 변경 이력" 등 날짜가 붙은 섹션들)에 이어서, 같은 형식(제목에 날짜, 하위 항목에 배경/원인/조치/참고 등)으로 추가. 새로운 성격의 변경이면 새 번호의 섹션(`## 12. ...`)을 만들고, 기존 주제의 연장이면 하위 항목만 추가.
- 이 문서에 히스토리를 남긴 뒤에는, 관련 코드 변경과 함께 커밋할지 사용자에게 확인할 것 (커밋 자체는 여전히 사용자 요청이 있을 때만 수행).

## 주의사항
- JWT 시크릿, DB 비밀번호 등 민감 정보를 코드나 커밋 메시지에 하드코딩하지 말 것.
- 운영 DB에 직접 마이그레이션/스키마 변경을 실행하지 말 것 (`ddl-auto=update`이므로 운영 반영 전 로컬에서 충분히 검증).
- 프론트엔드 API 스펙 변경 시 `meal-management-front` 저장소와의 호환성 확인.

## Docker 작업 시 주의사항 (2026-09-13 사고 이후 추가)
로컬에서 `docker compose up` 중 컨테이너 이름 충돌을 `docker rm`으로 해결하다가, 실제 데이터가 든 볼륨이 새로 생성된 빈 볼륨과 분리되는 사고가 있었음 (데이터 손실은 없었고 복구함 — 자세한 경위는 `구내식당_웹앱_기획설계서.md` 13장 참고). 재발 방지를 위한 규칙:
- 컨테이너 이름 충돌 등 정리가 필요할 땐 `docker rm <container>`로 개별 컨테이너만 지우지 말고 **`docker compose down`**을 쓸 것 (프로젝트가 관리하는 컨테이너/네트워크를 일관되게 정리함).
- 컨테이너를 지우기 전에는 `docker inspect <container> --format '{{json .Mounts}}'` 등으로 **어떤 볼륨을 쓰고 있는지 먼저 확인**할 것. 특히 이름 없는(익명) 볼륨을 쓰고 있다면, 지운 뒤 재생성 시 다른(새) 볼륨에 연결될 수 있음.
- `docker compose down -v`, `docker volume rm`, `docker volume prune` 등 **볼륨을 지우는 명령은 절대 먼저 실행하지 말 것.** 실행 전 반드시 그 볼륨에 실제 데이터가 있는지 확인하고(`docker volume ls`, 필요시 임시 컨테이너로 마운트해서 데이터 확인), 사용자에게 먼저 확인받을 것.
- DB 컨테이너가 죽었다 새로 떠서 테이블이 새로 생성되는 로그(`Hibernate: create table ...`)가 보이면, 원래 있어야 할 데이터가 없는 빈 DB로 기동된 것일 수 있으니 즉시 의심하고 확인할 것.