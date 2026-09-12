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
- Java 17, Spring Boot 3.5.13, Spring Security(JWT, jjwt 0.11.5), Spring Data JPA/Hibernate, PostgreSQL 16, Lombok.
- 인증은 세션 없이 완전 STATELESS. `JwtAuthenticationFilter`가 매 요청마다 토큰을 검사.
- JWT는 **httpOnly 쿠키**로 클라이언트에 전달함 (2026-09-13부터, `AuthController`). 응답 본문에는 담지 않음(XSS로 토큰 탈취 방지). `JwtAuthenticationFilter`는 `Authorization: Bearer` 헤더(Postman/test.http용)와 `token` 쿠키(프론트엔드용) 둘 다 지원. 로그아웃은 `POST /api/auth/logout`이 쿠키를 만료시켜서 처리(서버가 토큰 자체를 무효화하는 건 아님).
- 역할(Role) 기반 접근 제어: `ADMIN`, `OPERATOR`, `VIEWER`.
  - `/api/admin/**` → ADMIN 전용
  - `/api/meal/input/**` → ADMIN, OPERATOR
  - VIEWER는 자기 회사 데이터만 조회 가능 (컨트롤러 레벨에서 `SecurityContextHolder`로 판별)
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

## 환경 변수 / 설정
- DB 접속 정보는 환경 변수로 주입: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (미설정 시 `application.properties`의 기본값 사용 — 이 기본값은 로컬 개발용이며 운영 배포 시 반드시 환경 변수로 덮어써야 함).
- `ADMIN_PASSWORD`, `OPERATOR_PASSWORD` 등 초기 계정 비밀번호도 환경 변수로 주입 (`docker-compose.yml` 참고).
- `JWT_SECRET`: JWT 서명 키. `JwtUtil`에서 `@Value("${JWT_SECRET:...}")`로 주입받음. 값이 없으면 로컬 개발용 기본값을 쓰지만, **운영 배포 시에는 반드시 환경 변수로 별도 값을 지정**해야 함. 이 값을 바꾸면 기존에 발급된 모든 JWT 토큰이 즉시 무효화(전체 강제 로그아웃)되므로 배포 타이밍에 주의.
- `COOKIE_SECURE`: JWT 쿠키의 `Secure` 속성 여부. `AuthController`에서 `@Value("${COOKIE_SECURE:false}")`로 주입받음. 기본값 `false`는 아직 HTTPS를 안 쓰는 현재 환경 기준이고, **HTTPS 적용 후에는 반드시 `true`로 설정**해야 함(안 그러면 브라우저가 쿠키를 거부하거나, HTTPS 미적용 시 평문으로 토큰이 오갈 수 있음).
- `.env`, `logs/`는 `.gitignore`에 포함되어 있음 — 실제 비밀번호·시크릿 값은 절대 커밋하지 말 것.

## 인프라 변경 예정 (2026-09 기준)
- README.md에는 "AWS Lightsail (Seoul)"로 되어 있지만, 현재 Lightsail 사용은 중지된 상태.
- 사용자가 이사 예정이며, 이사 후에는 자택에 홈서버를 구축해서 직접 배포/운영할 계획 (아직 미착수).
- 배포/인프라 관련 작업 시 Lightsail 전제로 조언하지 말 것. 홈서버 환경(포트포워딩 최소화, 리버스 프록시+HTTPS, DDNS 또는 Cloudflare Tunnel/Tailscale, DB 포트 외부 노출 금지, 백업)을 고려할 것.
- 홈서버 구축이 실제로 시작되면 README.md 인프라 섹션과 이 항목을 갱신할 것.

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