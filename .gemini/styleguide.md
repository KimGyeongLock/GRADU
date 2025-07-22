# 스타일 가이드 (한글)

## 언어
- 모든 코드 리뷰 코멘트는 **한국어**로 작성합니다.

## 네이밍 규칙
- 클래스, 변수, 메소드명은 **카멜케이스(camelCase)** 사용
- Request/Response DTO는 **~Dto** 접미사 붙이기

## 코드 구조 및 배치
- Controller, Service, Repository 계층 분리
- 패키지 구조는 도메인 중심으로 구성 (예: domain.student)

## 에러 처리
- 예외는 `BaseException`을 상속해서 처리
- 공통 에러 코드는 `ErrorCode` enum에서 관리

## 주석 및 문서화
- 메소드 설명은 **Javadoc 형식**으로 작성
- TODO 주석 사용 시 `TODO: 작성 이유`를 명시
