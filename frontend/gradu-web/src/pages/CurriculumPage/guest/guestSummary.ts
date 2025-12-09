// src/pages/CurriculumPage/guestSummary.ts
import type { CourseDto, SummaryDto } from "../curriculumTypes";

// ==================== Summary 정책 (백엔드 포팅) ====================

export type SummaryPolicy = {
  pfRatioMax: number;
  pfMinTotalForLimit: number;
  totalCreditsMin: number;
  gpaMin: number;
  engMajorMinA: number;
  engLiberalMinA: number;
  engMajorMinB: number;
  engLiberalMinB: number;
  majorDesignedRequired: number;
  required: Record<string, number>;
};

export const SUMMARY_POLICY: SummaryPolicy = {
  pfRatioMax: 0.3,
  pfMinTotalForLimit: 130,
  totalCreditsMin: 130,
  gpaMin: 0.0,
  engMajorMinA: 21,
  engLiberalMinA: 9,
  engMajorMinB: 24,
  engLiberalMinB: 6,
  majorDesignedRequired: 12,
  required: {
    FAITH_WORLDVIEW: 9,
    PERSONALITY_LEADERSHIP: 6,
    PRACTICAL_ENGLISH: 9,
    GENERAL_EDU: 5,
    BSM: 18,
    ICT_INTRO: 2,
    FREE_ELECTIVE_BASIC: 9,
    FREE_ELECTIVE_MJR: 0,
    MAJOR: 60,
  },
};

const GPA_MAP: Record<string, number> = {
  "A+": 4.5,
  A0: 4.0,
  "B+": 3.5,
  B0: 3.0,
  "C+": 2.5,
  C0: 2.0,
  "D+": 1.5,
  D0: 1.0,
  F: 0.0,
};

const PF_GRADES = new Set(["P", "PD", "PASS"]);

// 0.5 단위 → 유닛(×2)
function toUnits(credit: number | null | undefined): number {
  if (credit == null) return 0;
  return Math.round(credit * 2);
}

// 공백 제거 + 대문자
function normName(s: string | null | undefined): string {
  if (!s) return "";
  return s.replace(/\s+/g, "").toUpperCase();
}

// “기독교 세계관” 한 번만 세기
const COUNT_ONCE_NAMES_NORM = new Set([normName("기독교 세계관")]);
const COUNT_ONCE_ALLOWED_CATS = new Set(["FAITH_WORLDVIEW", "GENERAL_EDU"]);

function isCountOnceTarget(c: CourseDto): boolean {
  return (
    COUNT_ONCE_NAMES_NORM.has(normName(c.name)) &&
    COUNT_ONCE_ALLOWED_CATS.has(c.category)
  );
}

// 등급 정규화
function normalizeGrade(g: string | null | undefined): string {
  if (!g) return "";
  let s = g.trim().toUpperCase();
  if (/^[ABCD]$/.test(s)) s += "0";
  return s;
}

function isPassGrade(grade: string | null | undefined): boolean {
  const g = normalizeGrade(grade);
  if (!g) return false;
  return g !== "F";
}

function englishPassCheck(
  policy: SummaryPolicy,
  major: number,
  liberal: number
): boolean {
  const caseA =
    major >= policy.engMajorMinA && liberal >= policy.engLiberalMinA;
  const caseB =
    major >= policy.engMajorMinB && liberal >= policy.engLiberalMinB;
  return caseA || caseB;
}

// 카테고리 row(백엔드 RowAssembler 포팅)
const ROW_KOR: Record<string, string> = {
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

const ROW_ORDER = [
  "FAITH_WORLDVIEW",
  "PERSONALITY_LEADERSHIP",
  "PRACTICAL_ENGLISH",
  "GENERAL_EDU",
  "BSM",
  "ICT_INTRO",
  "FREE_ELECTIVE_BASIC",
  "FREE_ELECTIVE_MJR",
  "MAJOR",
] as const;

function buildRows(
  courses: CourseDto[],
  policy: SummaryPolicy
): SummaryDto["rows"] {
  const earnedUnits: Record<string, number> = {};
  let designedEarned = 0;

  for (const c of courses) {
    if (!isPassGrade(c.grade)) continue;

    const cat = c.category;
    const units = toUnits(c.credit);
    earnedUnits[cat] = (earnedUnits[cat] ?? 0) + units;

    if (cat === "MAJOR") {
      designedEarned += c.designedCredit ?? 0;
    }
  }

  const rows: SummaryDto["rows"] = [];

  for (const key of ROW_ORDER) {
    if (key === "MAJOR") {
      const eMajor = (earnedUnits["MAJOR"] ?? 0) / 2.0;
      const reqMajor = policy.required["MAJOR"] ?? 0;
      const reqDesigned = policy.majorDesignedRequired;

      const majorPass = eMajor >= reqMajor;
      const designedPass = designedEarned >= reqDesigned;

      rows.push({
        key: "MAJOR",
        name: ROW_KOR["MAJOR"] ?? "전공",
        grad: `${reqMajor}(${reqDesigned})`,
        earned: eMajor,
        designedEarned,
        status: majorPass && designedPass ? "PASS" : "FAIL",
      });
    } else {
      const earned = (earnedUnits[key] ?? 0) / 2.0;
      const req = policy.required[key] ?? 0;

      rows.push({
        key,
        name: ROW_KOR[key] ?? key,
        grad: String(req),
        earned,
        designedEarned: null,
        status: earned >= req ? "PASS" : "FAIL",
      });
    }
  }

  return rows;
}

// 빈 Summary (초기값)
export function createEmptySummary(): SummaryDto {
  return {
    rows: ROW_ORDER.map((key) => {
      if (key === "MAJOR") {
        const reqMajor = SUMMARY_POLICY.required["MAJOR"];
        const reqDesigned = SUMMARY_POLICY.majorDesignedRequired;
        return {
          key: "MAJOR",
          name: ROW_KOR["MAJOR"] ?? "전공",
          grad: `${reqMajor}(${reqDesigned})`,
          earned: 0,
          designedEarned: 0,
          status: "FAIL",
        };
      }
      const req = SUMMARY_POLICY.required[key] ?? 0;
      return {
        key,
        name: ROW_KOR[key] ?? key,
        grad: String(req),
        earned: 0,
        designedEarned: null,
        status: req === 0 ? "PASS" : "FAIL",
      };
    }),
    pfCredits: 0,
    pfLimit: SUMMARY_POLICY.pfMinTotalForLimit * SUMMARY_POLICY.pfRatioMax,
    pfPass: true,
    totalCredits: 0,
    totalPass: false,
    gpa: 0,
    engMajorCredits: 0,
    engLiberalCredits: 0,
    englishPass: false,
    gradEnglishPassed: false,
    deptExtraPassed: false,
    finalPass: false,
  };
}

// 게스트 Summary 계산 (백엔드 SummaryCalculator 포팅)
export function computeGuestSummary(
  courses: CourseDto[],
  toggles?: { gradEnglishPassed: boolean; deptExtraPassed: boolean }
): SummaryDto {
  const policy = SUMMARY_POLICY;

  let totU = 0;
  let pfU = 0;
  let eMajorU = 0;
  let eLibU = 0;

  let gNumU = 0; // Σ(u * gradePoint)
  let gDenU = 0; // Σ(u)

  const seenCountOnce = new Set<string>();

  for (const c of courses) {
    const u = toUnits(c.credit);
    const gradeNorm = normalizeGrade(c.grade);
    const isPF = PF_GRADES.has(gradeNorm);
    const isGpaGrade = gradeNorm in GPA_MAP;

    let countsForTotals = true;
    if (isCountOnceTarget(c)) {
      const key = normName(c.name);
      if (seenCountOnce.has(key)) {
        countsForTotals = false;
      } else {
        seenCountOnce.add(key);
      }
    }

    if (countsForTotals) {
      totU += u;

      if (isPF) {
        pfU += u;
      } else if (isGpaGrade) {
        const gp = GPA_MAP[gradeNorm];
        gNumU += gp * u;
        gDenU += u;
      }

      if (c.isEnglish) {
        const cat = c.category;
        if (cat === "MAJOR") eMajorU += u;
        else eLibU += u;
      }
    }
  }

  const gpa =
    gDenU === 0 ? 0 : Math.round((gNumU / gDenU) * 1000) / 1000;

  const baseU = Math.max(totU, policy.pfMinTotalForLimit * 2);
  const pfLimitU = Math.floor(baseU * policy.pfRatioMax);
  const pfPass = pfU <= pfLimitU;

  const totalPass = totU >= policy.totalCreditsMin * 2;

  const engMajorCreditsD = eMajorU / 2.0;
  const engLiberalCreditsD = eLibU / 2.0;
  const englishPass = englishPassCheck(
    policy,
    engMajorCreditsD,
    engLiberalCreditsD
  );

  const rows = buildRows(courses, policy);
  const allCatPass = rows.every((r) => r.status === "PASS");

  const gradEnglishPassed = toggles?.gradEnglishPassed ?? false;
  const deptExtraPassed = toggles?.deptExtraPassed ?? false;

  const finalPass =
    allCatPass &&
    englishPass &&
    pfPass &&
    totalPass &&
    gradEnglishPassed &&
    deptExtraPassed &&
    gpa >= policy.gpaMin;

  const pfCredits = pfU / 2.0;
  const pfLimit = pfLimitU / 2.0;
  const totalCredits = totU / 2.0;

  return {
    rows,
    pfCredits,
    pfLimit,
    pfPass,
    totalCredits,
    totalPass,
    gpa,
    engMajorCredits: engMajorCreditsD,
    engLiberalCredits: engLiberalCreditsD,
    englishPass,
    gradEnglishPassed,
    deptExtraPassed,
    finalPass,
  };
}
