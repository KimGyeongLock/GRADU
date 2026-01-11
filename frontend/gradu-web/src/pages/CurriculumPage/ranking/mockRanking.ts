import type { RankingData } from "./rankingTypes";

export const mockRanking: RankingData = {
  major: [
    { rank: 1, courseName: "자료구조", takenCount: 2314, delta: 0 },
    { rank: 2, courseName: "운영체제", takenCount: 1988, delta: 1 },
    { rank: 3, courseName: "데이터베이스", takenCount: 1762, delta: -1 },
    { rank: 4, courseName: "컴퓨터네트워크", takenCount: 1623, delta: 2 },
    { rank: 5, courseName: "소프트웨어공학", takenCount: 1510, delta: 1 },
    { rank: 6, courseName: "알고리즘", takenCount: 1431, delta: -2 },
    { rank: 7, courseName: "컴퓨터구조", takenCount: 1378, delta: 0 },
    { rank: 8, courseName: "객체지향프로그래밍", takenCount: 1290, delta: 1 },
    { rank: 9, courseName: "웹프로그래밍", takenCount: 1217, delta: -1 },
    { rank: 10, courseName: "캡스톤디자인", takenCount: 1184, delta: 0 },
  ],
  liberal: [
    { rank: 1, courseName: "채플", takenCount: 2145, delta: 0 },
    { rank: 2, courseName: "팀빌딩과리더십", takenCount: 1876, delta: 2 },
    { rank: 3, courseName: "실용영어", takenCount: 1654, delta: -1 },
    { rank: 4, courseName: "기독교세계관", takenCount: 1523, delta: 1 },
    { rank: 5, courseName: "창의적사고", takenCount: 1456, delta: 3 },
    { rank: 6, courseName: "논리와비판적사고", takenCount: 1398, delta: -2 },
    { rank: 7, courseName: "글로벌시민교육", takenCount: 1287, delta: 0 },
    { rank: 8, courseName: "문화와예술", takenCount: 1234, delta: 1 },
    { rank: 9, courseName: "과학과기술", takenCount: 1196, delta: -1 },
    { rank: 10, courseName: "경제의이해", takenCount: 1148, delta: 0 },
  ],
};
