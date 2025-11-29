// src/pages/CurriculumPage/curriculumTypes.ts
export type Term = "1" | "2" | "sum" | "win";

export type SummaryRow = {
  key: string;
  name: string;
  grad: string;
  earned: number;
  designedEarned?: number;
  status: "PASS" | "FAIL" | string;
};

export type SummaryDto = {
  rows: SummaryRow[];
  pfCredits: number;
  pfLimit: number;
  pfPass: boolean;
  totalCredits: number;
  totalPass: boolean;
  gpa: number;
  engMajorCredits: number;
  engLiberalCredits: number;
  englishPass: boolean;
  gradEnglishPassed: boolean;
  deptExtraPassed: boolean;
  finalPass: boolean;
};

export type CourseLite = {
  id: number;
  name: string;
  category: string;
  credit: number;
  grade: string | null;
  isEnglish: boolean;
  academicYear: number;
  term: Term;
};

export const TERM_ORDER: Record<Term, number> = {
  "1": 0,
  sum: 1,
  "2": 2,
  win: 3,
};

export const CATEGORY_LABELS: Record<string, string> = {
  FAITH_WORLDVIEW: "신앙및세계관",
  PERSONALITY_LEADERSHIP: "인성및리더십",
  PRACTICAL_ENGLISH: "실무영어",
  GENERAL_EDU: "전문교양",
  BSM: "BSM",
  ICT_INTRO: "ICT융합기초",
  FREE_ELECTIVE_BASIC: "자유선택(교양)",
  FREE_ELECTIVE_MJR: "자유선택(교양또는비교양)",
  MAJOR: "전공",
};

export function getCategoryLabel(code: string) {
  return CATEGORY_LABELS[code] ?? code; // 혹시 모르는 값은 그냥 원문 노출
}

export const fmtCred = (n?: number | null) => {
  if (n == null || Number.isNaN(n)) return "-";
  const v = Math.round(n * 10) / 10;
  return Number.isInteger(v) ? String(v) : v.toFixed(1);
};

export const statusText = (sTxt: string) =>
  sTxt === "PASS" ? "합격" : sTxt === "FAIL" ? "불합격" : sTxt || "-";

export function formatSemester(year?: number, term?: Term): string {
  if (!year || !term) return "-";
  const yy = String(year).slice(-2);
  const t =
    term === "1" || term === "2"
      ? term
      : term === "sum"
      ? "summer"
      : "winter";
  return `${yy}-${t}`;
}