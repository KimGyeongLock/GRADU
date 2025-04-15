# 🎓 GRADU (졸업심사 요건 충족기)
> 개인 프로젝트

졸업에 필요한 과목과 학점을 한눈에 보고 체계적으로 설계할 수 있는 서비스를 준비 중입니다.

<br/>

## 💡 IDEA
<img src="https://github.com/user-attachments/assets/91aad9fe-c6e8-463c-97f1-1c259675f7c6" width="50%" />

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

Frontend: <img src="https://img.shields.io/badge/react-61DAFB?style=for-the-badge&logo=react&logoColor=black">   
Backend: <img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white">    
Database: <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">   <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=Redis&logoColor=white"> 
- **React**: 재사용 가능한 컴포넌트 구조와 빠른 렌더링 성능, 풍부한 생태계를 기반으로 개발 생산성과 유지보수 효율이 높음
- **Spring**: 안정적인 서버 운영에 적합하며, REST API 설계와 확장성 확보에 용이
- **MySQL**: 주요 데이터를 안정적으로 저장할 RDBMS, Spring Data JPA를 통한 ORM 지원으로 생산성 및 유지보수성 향상
- **Redis**: 세션 관리 또는 캐싱 용도, 자주 변경되지 않는 데이터나 사용자 세션을 메모리에 저장해 빠른 응답 속도 제공

> 본 서비스는 컴퓨터와 모바일 환경에서 동시에 접근이 가능한 **웹 플랫폼**으로 우선 출시되며, 이후 모바일 환경에 특화된 **앱 출시**와 서버 비용 절감을 고려한 **데스크탑 앱(Electron)** 전환도 계획하고 있습니다.


<br/>

## 🚧 Pain Points

- **제한적인 접근**: 기존 학교 시스템은 졸업심사 중에는 접근 자체가 불가한 경우도 있어 유동적으로 계획하기 어려움  
- **수정 불가능**: 미리 다음 학기를 시뮬레이션하기 어려워, 실제 수강 계획과 괴리가 발생  
- **낮은 편의성**: 여러 단계의 페이지를 거쳐야 하므로 사용자가 원하는 정보에 빠르게 도달하기 어렵다 판단

<br/>

## 🧩 Prototype

<img src="https://github.com/user-attachments/assets/30d7c34d-3b1a-4ce7-9fe7-396a1e8ca7a1" width="32%" />
<img src="https://github.com/user-attachments/assets/94667aff-622d-4b66-a8bf-f6246e4b9f7b" width="32%" />
<img src="https://github.com/user-attachments/assets/60ac3f5e-a3c6-4cc1-b0f3-151f9a8b2139" width="32%" />


### 사용 흐름
1. **분류 선택**  
   - (전공필수, 전공선택, 교양, 자유선택 등)
2. **과목 추가**  
   - 직접 입력하거나 DB에 등록된 과목에서 선택
3. **수정 및 삭제**  
   - 계획 변동 시 언제든 손쉽게 조정
4. **학기별 탭 기능** (옵션)  
   - 1안, 2안 등 여러 플랜을 동시에 비교·저장
5. **자동 학점 계산**  
   - 누적 학점 합산 및 졸업 요건 충족 여부 실시간 체크
6. **결과 저장**  
   - 서버에 백업하여 안전하게 보관하고 필요 시 다시 불러오기

---

