---
name: new-entity
description: meal-management 백엔드(Spring Boot)의 기존 코드 패턴을 그대로 따라 새 엔티티의 Entity/Repository/Service/Controller(+필요시 DTO)를 한 번에 생성합니다. "/new-entity 엔티티이름" 형태로 호출하세요 (예: /new-entity MealPlan).
---

# /new-entity — 신규 엔티티 자동 생성 스킬

이 스킬은 `$0`으로 전달받은 엔티티 이름을 기준으로, 이 저장소(`meal_management` 백엔드)에
이미 존재하는 `MealRecord` / `CompanyTeam` / `Company` 코드의 패턴을 그대로 따라서
Entity, Repository, Service, Controller(+ 필요시 요청/응답 DTO)를 생성한다.

**인자**: `$0` = 생성할 엔티티 이름 (PascalCase 권장, 예: `MealPlan`, `Notice`).
- `$0`이 비어 있으면, 생성하지 말고 사용자에게 엔티티 이름을 먼저 물어볼 것.
- `$0`이 camelCase/snake_case/한글 등으로 들어와도 PascalCase 클래스명으로 정규화해서 사용할 것
  (예: `meal_plan` → `MealPlan`, `mealPlan` → `MealPlan`).

## 0단계 — 먼저 확인할 것 (추측 금지)

새 엔티티를 만들기 전에, 아래 항목이 프롬프트나 대화에서 명확하지 않으면 **코드를 생성하기 전에
간단히 사용자에게 물어볼 것**. 억지로 추측해서 필드를 만들어내지 말 것 (특히 실제 컬럼/도메인 로직은
사용자마다 다르므로 틀리면 재작업 비용이 큼):

1. **필드 목록과 타입** — 엔티티가 어떤 컬럼을 가지는지 (예: 이름, 날짜, 금액, 참조 회사 등)
2. **다른 엔티티와의 관계** — `Company`/`CompanyTeam`/`User` 등을 참조하는지 (`@ManyToOne` 대상)
3. **소프트 딜리트 여부** — `CompanyTeam`/`Company`처럼 `isActive` 플래그로 논리 삭제할지,
   아니면 삭제 기능이 아예 없거나(MealRecord) 하드 삭제할지
4. **VIEWER 접근 범위 제한 필요 여부** — `MealRecordController`처럼 VIEWER 역할이면
   자기 회사 데이터만 보게 해야 하는지, 아니면 `/api/admin/**`이나 `/api/meal/input/**`처럼
   `SecurityConfig`의 역할 제한 규칙에 새로 걸어야 하는지 (해당 시 `SecurityConfig.java` 수정 필요함을 안내)
5. **URL 리소스 경로** — 기본은 엔티티명의 kebab-case 복수형이지만(`/api/{kebab-plural}`),
   `CompanyTeam`처럼 상위 리소스에 종속되는 구조(`/api/companies/{companyId}/teams`)가 필요한지

정보가 이미 충분히 주어졌다면 (예: 사용자가 필드까지 구체적으로 요청한 경우) 다시 묻지 말고 바로 생성할 것.

## 1단계 — 참고 파일 다시 읽기

생성 직전에 아래 3개 파일을 최신 상태로 다시 읽어서 패턴이 바뀌지 않았는지 확인할 것
(이 문서에 있는 템플릿은 스냅샷이며, 실제 코드가 더 정확한 소스임):

- `src/main/java/meal_management/entity/MealRecord.java` (연관관계 + 응답 DTO 패턴의 기준)
- `src/main/java/meal_management/entity/CompanyTeam.java` + 같은 이름의 Repository/Service/Controller (소프트 딜리트 + 단순 CRUD 기준)
- `src/main/java/meal_management/config/SecurityConfig.java` (새 엔드포인트에 역할 제한이 필요한 경우)

## 네이밍 규칙

`$0`을 `{Entity}`로 정규화했다고 할 때:

| 대상 | 규칙 | 예시 (`$0=MealPlan`) |
|---|---|---|
| 클래스명 | PascalCase | `MealPlan` |
| 변수/필드명 | camelCase | `mealPlan` |
| 테이블명 (`@Table(name=...)`) | snake_case 단수형 | `meal_plan` |
| 컬럼명 (`@Column(name=...)`) | snake_case | `plan_name`, `created_at` |
| URL 리소스 경로 | kebab-case 복수형 | `/api/meal-plans` |
| Repository | `{Entity}Repository` | `MealPlanRepository` |
| Service | `{Entity}Service` | `MealPlanService` |
| Controller | `{Entity}Controller` | `MealPlanController` |

## 2단계 — Entity 생성

경로: `src/main/java/meal_management/entity/{Entity}.java`

```java
package meal_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
// 날짜만 있는 필드가 있다면 import java.time.LocalDate; 도 추가

@Entity
@Table(name = "{snake_case_table}")
@Getter
@Setter
public class {Entity} {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: 실제 필드를 여기에 추가 (0단계에서 확인한 필드 목록 기준)
    @Column(name = "{snake_case_column}", nullable = false)
    private String {fieldName};

    // 다른 엔티티 참조가 있다면 아래 패턴을 따를 것 (MealRecord.java 참고):
    // - 순환 참조 방지를 위해 반드시 @JsonIgnore를 붙인다.
    // - 응답에서 즉시 관련 필드(예: 이름)까지 함께 써야 하면 FetchType.EAGER,
    //   아니면 FetchType.LAZY (CompanyTeam.company, MealRecord.createdBy 처럼).
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "{fk_column}_id")
    private {RelatedEntity} {relatedField};

    // 소프트 딜리트가 필요하면 (CompanyTeam/Company 패턴):
    // @Column(name = "is_active")
    // private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

주의:
- `@Data` 대신 반드시 `@Getter` + `@Setter` 두 개를 따로 쓸 것 (이 프로젝트 전 엔티티가 이 조합을 씀 — `@Data`가 생성하는 `equals/hashCode/toString`이 JPA 연관관계에서 순환 참조를 일으킬 수 있기 때문).
- 연관관계 필드에 `@JsonIgnore`를 빼먹지 말 것. 응답에 관련 엔티티 이름 등을 노출해야 하면 Controller의 응답 DTO(4단계)에서 수동으로 매핑한다 — 엔티티에서 직접 직렬화하지 않는다.

## 3단계 — Repository 생성

경로: `src/main/java/meal_management/repository/{Entity}Repository.java`

```java
package meal_management.repository;

import meal_management.entity.{Entity};
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * {엔티티 한글명} Repository
 * JpaRepository를 상속받아 기본 CRUD가 자동으로 생성돼요.
 */
public interface {Entity}Repository extends JpaRepository<{Entity}, Long> {

    // 필요한 조회 조건에 맞춰 Spring Data 메서드 이름 규칙으로 추가.
    // 메서드 위에는 "어떤 SQL이 자동 생성되는지" 한국어 주석을 달 것 (기존 파일 스타일).
    // 예) 소프트 딜리트 엔티티라면:
    // List<{Entity}> findByIsActiveTrue();
}
```

## 4단계 — Service 생성

경로: `src/main/java/meal_management/service/{Entity}Service.java`

```java
package meal_management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import meal_management.entity.{Entity};
import meal_management.repository.{Entity}Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * {엔티티 한글명} 관련 비즈니스 로직을 담당하는 서비스예요.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class {Entity}Service {

    private final {Entity}Repository {entityVar}Repository;

    /**
     * {엔티티 한글명} 목록 조회
     */
    public List<{Entity}> getAll{Entity}s() {
        return {entityVar}Repository.findAll();
        // 소프트 딜리트 대상이면 findByIsActiveTrue() 등으로 교체
    }

    /**
     * {엔티티 한글명} 단건 조회
     * 없으면 예외를 발생시켜요.
     */
    public {Entity} get{Entity}(Long id) {
        return {entityVar}Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("{엔티티 한글명}을(를) 찾을 수 없습니다."));
    }

    /**
     * {엔티티 한글명} 등록
     */
    @Transactional
    public {Entity} create{Entity}({Entity} {entityVar}) {
        log.debug("{엔티티 한글명} 등록 처리: {}", {entityVar});
        {Entity} saved = {entityVar}Repository.save({entityVar});
        log.info("{엔티티 한글명} 등록 완료: id={}", saved.getId());
        return saved;
    }

    /**
     * {엔티티 한글명} 수정
     * 변경 가능한 필드만 골라서 업데이트해요.
     */
    @Transactional
    public {Entity} update{Entity}(Long id, {Entity} updated) {
        {Entity} existing = get{Entity}(id);
        // TODO: existing.setXxx(updated.getXxx()) 형태로 변경 가능한 필드만 갱신
        {Entity} saved = {entityVar}Repository.save(existing);
        log.info("{엔티티 한글명} 수정 완료: id={}", id);
        return saved;
    }

    /**
     * {엔티티 한글명} 삭제
     * 소프트 딜리트 대상이면 CompanyTeamService.deleteTeam()처럼
     * isActive를 false로 바꿔서 save() — 하드 삭제가 맞다면 repository.deleteById(id) 사용.
     */
    @Transactional
    public void delete{Entity}(Long id) {
        {entityVar}Repository.deleteById(id);
        log.info("{엔티티 한글명} 삭제 완료: id={}", id);
    }
}
```

- 조회(읽기) 메서드에는 `@Transactional`을 붙이지 않는다(기존 코드 기준). 쓰기(등록/수정/삭제) 메서드에만 붙인다.
- 실패 시 예외는 커스텀 예외 클래스 없이 `new RuntimeException("한국어 메시지")` 형태를 그대로 따른다(기존 코드 전체가 이 패턴).
- 다른 엔티티를 참조해야 하면(예: `companyId`를 받아 `Company`를 조회) `CompanyTeamService`처럼 해당 `{Related}Service`를 생성자 주입으로 받아 `get{Related}(id)`를 호출해 존재를 검증한 뒤 세팅한다.
- 로깅 컨벤션(CLAUDE.md "로깅 컨벤션" 참고): 쓰기 메서드는 `log.info`로 결과(id 등)를 남기고, 상세 흐름은 `log.debug`. 비밀번호/토큰 등 민감정보는 절대 로그에 남기지 않는다.

## 5단계 — Controller 생성

경로: `src/main/java/meal_management/controller/{Entity}Controller.java`

DTO 방침(중요, 반드시 판단할 것):
- **엔티티가 단순하고 연관관계(`@JsonIgnore` 대상)가 없거나 그대로 노출해도 무방하면** (`Company`처럼) 요청/응답 모두 엔티티 클래스를 그대로 `@RequestBody`/반환 타입으로 써도 된다. 별도 DTO 불필요.
- **엔티티에 `@JsonIgnore` 연관관계가 있어 관련 필드(예: 이름)를 평탄화해서 내려줘야 하거나, 요청 필드 타입 변환(예: 문자열 날짜 → `LocalDate`)이 필요하면** (`MealRecord`처럼) 아래처럼 컨트롤러 안에 `static inner class`로 Request/Response DTO를 만든다.
- 여러 컨트롤러에서 재사용되는 DTO가 아니라면 최상위 `dto` 패키지에 만들지 말 것. `dto` 패키지는 `LoginRequestDto`/`LoginResponseDto`처럼 인증처럼 여러 곳에서 공유되는 경우 전용이다.

```java
package meal_management.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import meal_management.entity.{Entity};
import meal_management.service.{Entity}Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {엔티티 한글명} 관련 API를 처리하는 컨트롤러예요.
 */
@Slf4j
@RestController
@RequestMapping("/api/{kebab-plural}")
@RequiredArgsConstructor
public class {Entity}Controller {

    private final {Entity}Service {entityVar}Service;

    // ========================
    // 요청/응답 DTO (연관관계 평탄화나 타입 변환이 필요할 때만)
    // ========================

    @Getter
    @Setter
    public static class {Entity}Request {
        // TODO: 요청 필드
    }

    @Getter
    public static class {Entity}Response {
        // TODO: 응답 필드 (연관관계는 여기서 이름 등으로 평탄화)

        public {Entity}Response({Entity} {entityVar}) {
            // TODO: this.xxx = {entityVar}.getXxx();
        }
    }

    // ========================
    // API
    // ========================

    /**
     * {엔티티 한글명} 목록 조회 API
     * GET /api/{kebab-plural}
     */
    @GetMapping
    public ResponseEntity<List<{Entity}>> get{Entity}s() {
        return ResponseEntity.ok({entityVar}Service.getAll{Entity}s());
    }

    /**
     * {엔티티 한글명} 단건 조회 API
     * GET /api/{kebab-plural}/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<{Entity}> get{Entity}(@PathVariable Long id) {
        return ResponseEntity.ok({entityVar}Service.get{Entity}(id));
    }

    /**
     * {엔티티 한글명} 등록 API
     * POST /api/{kebab-plural}
     */
    @PostMapping
    public ResponseEntity<{Entity}> create{Entity}(@RequestBody {Entity} {entityVar}) {
        log.info("{엔티티 한글명} 등록 요청");
        return ResponseEntity.ok({entityVar}Service.create{Entity}({entityVar}));
    }

    /**
     * {엔티티 한글명} 수정 API
     * PUT /api/{kebab-plural}/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<{Entity}> update{Entity}(
            @PathVariable Long id,
            @RequestBody {Entity} {entityVar}) {
        log.info("{엔티티 한글명} 수정 요청: id={}", id);
        return ResponseEntity.ok({entityVar}Service.update{Entity}(id, {entityVar}));
    }

    /**
     * {엔티티 한글명} 삭제 API
     * DELETE /api/{kebab-plural}/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete{Entity}(@PathVariable Long id) {
        log.info("{엔티티 한글명} 삭제 요청: id={}", id);
        {entityVar}Service.delete{Entity}(id);
        return ResponseEntity.ok().build();
    }
}
```

- 각 API 메서드 위 Javadoc에는 기존 컨트롤러들처럼 **요청/응답 JSON 예시**를 반드시 포함시킬 것.
- VIEWER를 자기 회사 데이터로 제한해야 한다면, `MealRecordController`의 `getCurrentRole()` / `getCurrentCompanyId()` 헬퍼 메서드와 조회 API 내 분기 패턴을 그대로 복사해서 적용한다.
- 상위 리소스에 종속되는 구조가 필요하면(`CompanyTeamController`처럼) `@RequestMapping`을 `/api/{parent-kebab-plural}/{parentId}/{kebab-plural}` 형태로 바꾸고, 각 메서드에 `@PathVariable Long {parentId}`를 추가한다.

## 6단계 — 역할 기반 접근 제어가 필요하면 SecurityConfig 갱신

새 엔드포인트가 `ADMIN` 전용이거나 `ADMIN`+`OPERATOR` 전용이어야 한다면(0단계에서 확인),
`SecurityConfig.java`의 `authorizeHttpRequests` 블록에 URL 패턴을 추가해야 한다. 단순히
"로그인한 사람이면 누구나"면 별도 수정 없이 기존 `.anyRequest().authenticated()` 규칙에 걸린다.
이 파일을 고칠 경우 **CLAUDE.md의 히스토리 기록 규칙**(구내식당_웹앱_기획설계서.md에 아키텍처/보안
변경 이력 남기기)에 해당하므로, 생성 작업이 끝난 뒤 사용자에게 히스토리 기록 여부를 확인할 것.

## 7단계 — 마무리 체크리스트

생성 후 아래를 확인하고 사용자에게 보고할 것:
0. 생성한 코드에 `// TODO`로 남은 부분이 없는지 확인 —
   0단계에서 확인한 필드/관계로 전부 채워야 하며,
   빈 TODO가 남아있으면 안 됨 (특히 update 메서드, Request/Response DTO)
1. 4개(또는 DTO 포함 5개) 파일이 기존 패턴과 어노테이션/네이밍이 일치하는지
2. `ddl-auto=update`이므로 로컬 실행(`./mvnw spring-boot:run`) 시 새 테이블이 자동 생성됨 — 운영 DB에는
   직접 마이그레이션하지 말 것(CLAUDE.md 주의사항)
3. API 계약(요청/응답 필드)이 프론트엔드(`meal-management-front`)에서 쓰일 예정이면, 그쪽 저장소와의
   호환성 확인이 필요함을 사용자에게 안내
4. **파일 생성만 하고 git commit은 사용자가 명시적으로 요청하기 전까지 하지 말 것**
