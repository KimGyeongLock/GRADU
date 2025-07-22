# GRADU Java/Spring Style Guide

## 0. Language Policy
- **모든 코드 리뷰 코멘트는 반드시 한국어(Korean)로 작성한다.**
- 실수로 영어로 작성했다면 한국어로 다시 작성한다.
- 예시
    - ❌ _Consider renaming this variable..._
    - ✅ 변수명을 더 명확하게 하기 위해 `totalCredit`으로 변경하는 것이 좋습니다.

---

## 1. Introduction
이 문서는 GRADU 프로젝트(Java 21 + Spring Boot)의 코딩 컨벤션을 정의한다.  
Google Java Style, Spring 공식 가이드라인을 기본으로 하되 프로젝트 특성에 맞춰 조정한다.

**핵심 원칙:** Readability / Maintainability / Consistency / Domain-Centric Design / Reasonable Performance

---

## 2. Project Architecture & Package Structure
- **도메인 기반 패키징(Domain-Driven Packaging)** 을 따른다.

```
com.gradu/
  common/          # 공통 유틸, 응답 래퍼, 공통 예외/에러코드
  config/          # Spring/JPA/Security/CORS/Swagger 설정
  domain/
    student/
      controller/
      service/
      repository/
      dto/
      entity/
    curriculum/
    record/
    evaluation/
```
- 레이어를 섞지 않는다. 각 도메인 별로 `controller/service/repository/dto/entity` 구성.
- Controller는 얇게(요청/응답 변환, HTTP 처리). Service에 비즈니스 로직 집중. Repository는 영속화만.

---

## 3. Controller Naming & Responsibilities
- **역할 기반 이름** 사용: `AuthController`, `GraduationController`, `RecordController`, `StudentProfileController` 등.
- RESTful 리소스 네이밍:
    - `/api/students/{id}/records`, `/api/graduation/status`
- 한 컨트롤러는 한 책임만 갖는다. (SRP)

---

## 4. Naming Conventions
- **Class / Enum 타입명**: `PascalCase` (예: `StudentService`, `GraduationResult`)
- **메서드 / 변수명**: `camelCase` (예: `calculateStatus`, `totalCredit`)
- **상수**: `UPPER_SNAKE_CASE`
- **DTO**: 접미사 `Dto` (`StudentRequestDto`, `StudentResponseDto`)
- **Enum 값**: `UPPER_SNAKE_CASE`
- 불필요한 축약어 금지. (`id`, `url`, `dto` 등 일반적인 약어는 허용)

---

## 5. Java & Spring Conventions
- 대상 JDK는 **Java 21**. 가독성 향상에 도움이 되면 `record`, pattern matching 사용(Preview 기능은 운영 코드에서 사용 금지).
- **생성자 주입** 기본. 필드 주입 금지.
- 엔티티를 API 결과로 직접 노출하지 않는다. 항상 DTO로 변환.
- 트랜잭션은 Service 레이어에서 관리. (`@Transactional`)
    - 읽기 전용 쿼리는 `@Transactional(readOnly = true)` 명시.
- Validation은 `jakarta.validation` 애노테이션 사용.

---

## 6. Persistence & Performance (JPA)
- **N+1 쿼리 방지**: Fetch Join, Batch Size, EntityGraph 등을 적절히 사용.
- 대량 처리 시 벌크 연산(QueryDSL/Native Query 등) 고려.
- 연관관계 편의 메서드 정의 시 양방향 일관성 유지.
- LAZY 로딩 기본, 필요한 곳에서만 EAGER/Fetch join.

---

## 7. Exception & Error Handling
- 공통 예외 계층: `BaseException` 및 `ErrorCode` enum(`code`, `httpStatus`, `message` 포함).
- 광범위한 `Exception` 캐치는 지양. 필요한 경우 로깅 후 래핑하여 던진다.
- 전역 예외 처리: `@ControllerAdvice` + `@ExceptionHandler` 이용.
- 에러 응답은 통일된 형태 `{ code, message, detail }` 로 반환.

---

## 8. Logging
- `org.slf4j.Logger` 사용 (Lombok `@Slf4j` 허용).
- 로그 레벨 가이드
    - DEBUG: 상세 내부 상태
    - INFO: 정상 흐름의 중요한 이벤트
    - WARN: 복구 가능한 예외/비정상 입력
    - ERROR: 시스템 오류/치명적 상황
- 민감 정보(비밀번호, 토큰, 주민번호 등)는 마스킹하거나 로그에 남기지 않는다.
- 중복 로그, 과도한 로깅 금지.

---

## 9. Validation
- 요청 DTO에 `@NotNull`, `@Size`, `@Pattern` 등 선언적 검증 사용.
- 커스텀 Validator는 `common.validation` 또는 도메인 내부에 배치.
- Service 단에서도 비즈니스 검증 로직을 명확히 작성.

---

## 10. Tests
- **JUnit 5** + AssertJ/Mockito 기본.
- 단위 테스트(Unit): 외부 의존성(Mock) 처리.
- 통합 테스트(Integration): `@SpringBootTest`, Testcontainers 또는 H2 사용.
- 테스트 메서드 네이밍: `methodUnderTest_condition_expectedResult` 형식 권장.
- 커버리지 목표(예: 70% 이상)가 있다면 CI에서 체크.

---

## 11. Build & Tooling
- **Gradle** 사용, Java toolchain 21 지정.
- **Formatter**: Google Java Format 또는 Spotless로 자동 정렬. (CI에서 포맷 불일치 시 실패)
- **Static Analysis**: Checkstyle/PMD/SpotBugs(선택) 실행.
- CI 파이프라인에서 빌드/테스트/포맷/린트 자동화.

---

## 12. Documentation & Comments
- 공개 API(Service, Util 등)는 **Javadoc** 작성.
- 주석은 “무엇”이 아닌 “왜”를 설명. (자명한 코드에 주석 금지)
- `TODO:` 주석은 반드시 사유/이슈 번호 포함: `TODO(GR-123): 하드코딩 제거`

---

## 13. Commit & PR Convention
- **Conventional Commits** 형식:
    - `feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`, `chore:`, `build:`, `ci:`
- 하나의 PR은 하나의 목적만 수행(사이드 리팩토링 분리).
- PR 본문 필수 항목:
    1. 변경 내용(What) & 이유(Why)
    2. 관련 이슈/티켓 링크
    3. 테스트 방법(How to test) 및 결과 증빙
- Gemini는 이 가이드를 기준으로 **한국어로** 리뷰한다.

---

## 14. 리뷰 체크리스트 (Gemini, 아래 항목을 중점으로 리뷰해라)
1. **언어**: 모든 리뷰 코멘트가 한국어인가?
2. **레이어 책임 분리**: Controller에 비즈니스 로직이 들어가 있지 않은가?
3. **엔티티 노출 금지**: API 응답에 JPA 엔티티를 그대로 반환하지 않았는가?
4. **트랜잭션 관리**: Service 레이어에서만 @Transactional을 사용했는가? 읽기 전용 설정은 적절한가?
5. **예외 처리 일관성**: BaseException + ErrorCode 규칙을 지켰는가?
6. **N+1 쿼리**: JPA 사용 시 N+1 문제가 발생하지 않도록 쿼리 전략을 적용했는가?
7. **DTO 네이밍/구조**: ~RequestDto, ~ResponseDto 규칙을 지켰는가? DTO가 적절한 필드만 노출하는가?
8. **로깅 민감정보**: 민감 정보를 로그에 출력하지 않았는가?
9. **Validation**: 입력값 검증이 충분한가(애노테이션/비즈니스 검증)?
10. **테스트 품질**: 단위/통합 테스트가 적절히 분리되고 커버리지가 충분한가?
11. **커밋/PR 컨벤션**: Conventional Commits, PR 설명 규칙이 지켜졌는가?
12. **코드 일관성/가독성**: 불필요한 축약어, 매직넘버, 중복 코드가 없는가?

---
