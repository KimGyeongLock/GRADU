# 🎓 GRADU

졸업 요건에 맞춰 과목과 학점을 쉽고 직관적으로 관리할 수 있는 학기별 수업 설계 도구를 만들고자 합니다.

<br/>

## 💡 IDEA

### 선정한 이유
- 졸업 직전에 필수 과목이나 전공 이수를 놓쳐서 곤란해지는 상황이 잦음  
- 학교의 공식 시스템은 조회 위주이거나, 졸업심사 중 접근이 어려워 유연한 설계가 힘듦  
- 학생들이 자유도 높게 학점을 관리하고, 놓칠 뻔한 요건을 사전에 파악할 수 있는 툴이 필요

### 프로젝트 개요
- **목표**: 각 학기에 들을 과목을 자유롭게 추가·편집하고, 누적 학점과 졸업 요건 충족 여부를 실시간으로 확인할 수 있는 툴 제공  
- **핵심**: 한눈에 학점 현황을 파악하여, 예기치 못한 졸업 연기를 방지  
- **확장성**: 모바일과 웹에서 모두 접근 가능하도록 개발할 예정

### 기대 효과
- **학점 계획 간소화**: 학기별·분야별 과목 구성 시 자동으로 학점이 계산되어, 학습 효율 극대화  
- **졸업 요건 사전 대비**: 필수 과목·학점을 놓치는 일이 줄어들어, 졸업 연기 가능성 감소  
- **추가 기능**(선택 사항): 별점·후기·추천 과목 등 커뮤니티 기능 확장으로 선택에 도움

<br/>

## 🔧 Stacks

Frontend: <img src="https://img.shields.io/badge/flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white">  
Backend: <img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
Database: <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> 
- **Flutter**: 빠른 프로토타이핑, 크로스플랫폼 지원(안드로이드/iOS/웹)  
- **Spring**: 안정적인 서버 운영에 적합하며, REST API 설계와 확장성 확보에 용이

<br/>

## 🚧 Pain Points

- **제한적인 접근**: 기존 학교 시스템은 졸업심사 중에는 접근 자체가 불가한 경우도 있어 유동적으로 계획하기 어려움  
- **수정 불가능**: 미리 다음 학기를 시뮬레이션하기 어려워, 실제 수강 계획과 괴리가 발생  
- **낮은 편의성**: 모바일 등 다양한 기기를 통한 접근성이 떨어져, 수시 확인이 쉽지 않음

<br/>

## 🧩 Prototype

### 사용 흐름
1. **분류 선택**  
   - (전공필수, 전공선택, 교양, 자유선택 등)
2. **과목 추가**  
   - 직접 입력하거나 DB에 등록된 과목에서 선택
3. **수정 및 삭제**  
   - 계획 변동 시 언제든 손쉽게 조정
4. **학기별 탭 기능**  
   - 1안, 2안 등 여러 플랜을 동시에 비교·저장
5. **자동 학점 계산**  
   - 누적 학점 합산 및 졸업 요건 충족 여부 실시간 체크
6. **결과 저장**  
   - 서버에 백업하여 안전하게 보관하고 필요 시 다시 불러오기

---

