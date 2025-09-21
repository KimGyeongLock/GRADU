// src/pages/CurriculumPage.tsx
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../lib/axios";
import AddCourseModal from "../components/AddCourseModal";
import s from "./CurriculumTable.module.css";

type CurriculumItem = {
  category: string;            // e.g. "MAJOR", "MAJOR_DESIGNED"
  earnedCredits: number;
  status: "PASS" | "FAIL" | string;
};

type Course = {
  id: number;
  name: string;
  credit: number;
  category: string; // "MAJOR", ...
  grade: string | null; // "A+", "P", ...
  isEnglish?: boolean;
};

type RowUI = {
  key: string;
  name: string;
  grad: string;
  earned: number;
  status: string;
  designedEarned?: number;     // 전공행에서만 사용
};

const KOR_LABELS = {
  FAITH_WORLDVIEW: "신앙및세계관",
  PERSONALITY_LEADERSHIP: "인성및리더십",
  PRACTICAL_ENGLISH: "실무영어",
  GENERAL_EDU: "전문교양",
  BSM: "BSM",
  ICT_INTRO: "ICT융합기초",
  FREE_ELECTIVE_BASIC: "자유선택(교양)",
  FREE_ELECTIVE_MJR: "자유선택(교양또는비교양)",
  MAJOR: "전공",
} as const;

const ORDER = [
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

const GRAD_REQS: Record<(typeof ORDER)[number], string> = {
  FAITH_WORLDVIEW: "9",
  PERSONALITY_LEADERSHIP: "6",
  PRACTICAL_ENGLISH: "9",
  GENERAL_EDU: "5",
  BSM: "18",
  ICT_INTRO: "2",
  FREE_ELECTIVE_BASIC: "9",
  FREE_ELECTIVE_MJR: "0",
  MAJOR: "60(12)", // 60 전공(설계 12)
};

// GPA(4.5 만점 기준)
const GPA_POINTS: Record<string, number> = {
  "A+": 4.5, A: 4.5, "A0": 4.0,
  "B+": 3.5, "B0": 3.0,
  "C+": 2.5, "C0": 2.0,
  "D+": 1.5, "D0": 1.0,
  "F": 0,
};

const PF_GRADES = new Set(["P", "PD", "PASS"]);

export default function CurriculumPage() {
  const sid = getStudentId() || "";
  const qc = useQueryClient();
  const nav = useNavigate();

  // 카테고리별 합/상태
  const { data: curData = [], isLoading, isError } = useQuery<CurriculumItem[]>({
    queryKey: ["curriculum", sid],
    enabled: !!sid,
    queryFn: async () => {
      const url = `/api/v1/students/${encodeURIComponent(sid)}/curriculum`;
      const { data } = await axiosInstance.get<CurriculumItem[]>(url);
      return data ?? [];
    },
  });

  // 요약 계산을 위해 전체 과목 조회
  const { data: courseList = [] } = useQuery<Course[]>({
    queryKey: ["courses-all", sid],
    enabled: !!sid,
    queryFn: async () => {
      const url = `/api/v1/students/${encodeURIComponent(sid)}/courses`;
      const { data } = await axiosInstance.get<Course[]>(url);
      return data ?? [];
    },
  });

  // 졸업영어시험 / 학부추가졸업요건 토글
  const { data: toggles = { gradEnglishPassed: false, deptExtraPassed: false } } = useQuery({
    queryKey: ["grad-toggles", sid],
    queryFn: async () => {
      try {
        const { data } = await axiosInstance.get(`/api/v1/students/${sid}/summary/toggles`);
        return data ?? { gradEnglishPassed: false, deptExtraPassed: false };
      } catch {
        return { gradEnglishPassed: false, deptExtraPassed: false };
      }
    },
  });
  const [gradEnglishPassed, setGradEnglishPassed] = useState<boolean>(!!toggles.gradEnglishPassed);
  const [deptExtraPassed, setDeptExtraPassed] = useState<boolean>(!!toggles.deptExtraPassed);

  const saveToggles = useMutation({
    mutationFn: async (payload: { gradEnglishPassed: boolean; deptExtraPassed: boolean }) => {
      await axiosInstance.patch(`/api/v1/students/${sid}/summary/toggles`, payload);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["grad-toggles", sid] }),
  });

  // ----- 테이블 행 (전공은 설계와 합쳐서 표기 & 상태는 둘 다 PASS여야 PASS) -----
  const rows: RowUI[] = useMemo(() => {
    const byCat = new Map<string, CurriculumItem>();
    curData.forEach((it) => byCat.set(it.category, it));

    return ORDER.map((k) => {
      if (k === "MAJOR") {
        const major = byCat.get("MAJOR");
        const majorDesigned = byCat.get("MAJOR_DESIGNED"); // DB에 존재

        const earned = major?.earnedCredits ?? 0;
        const designedEarned = majorDesigned?.earnedCredits ?? 0;

        const status =
          major?.status === "PASS" && majorDesigned?.status === "PASS" ? "PASS" : "FAIL";

        return {
          key: k,
          name: KOR_LABELS[k],
          grad: GRAD_REQS[k],
          earned,
          designedEarned,
          status,
        };
      }

      const item = byCat.get(k);
      return {
        key: k,
        name: KOR_LABELS[k],
        grad: GRAD_REQS[k],
        earned: item?.earnedCredits ?? 0,
        status: item?.status ?? "",
      };
    });
  }, [curData]);

  // ===== 요약 계산 =====
  const {
    pfCredits,
    totalCredits,
    gpa,
    engMajorCredits,
    engLiberalCredits,
    englishPass,
    pfPass,
    totalPass,
    finalPass,
  } = useMemo(() => {
    let pf = 0;
    let tot = 0;
    let gpaNum = 0;
    let gpaDen = 0;
    let eMajor = 0;
    let eLib = 0;

    for (const c of courseList) {
      const credit = c.credit ?? 0;
      const grade = (c.grade || "").toUpperCase().trim();

      // 총 취득학점: P 포함
      tot += credit;

      // P/F 과목 합
      if (PF_GRADES.has(grade)) {
        pf += credit;
      } else {
        const p = GPA_POINTS[grade] ?? null;
        if (p != null) {
          gpaNum += credit * p;
          gpaDen += credit;
        }
      }

      // 영어강의
      if (c.isEnglish) {
        if (c.category === "MAJOR") eMajor += credit;
        else eLib += credit;
      }
    }

    const gpaVal = gpaDen > 0 ? gpaNum / gpaDen : 0;

    // 1) P/F 합격: 총 취득학점의 30% 이상
    const pfLimit = Math.floor(tot * 0.3);
    const pfOk = pf >= pfLimit;

    // 2) 총 취득학점: 130학점 이상
    const totalOk = tot >= 130;

    // 영어강의: (전공≥21 & 교양≥9) 또는 (전공≥24 & 교양≥6)
    const englishOk = (eMajor >= 21 && eLib >= 9) || (eMajor >= 24 && eLib >= 6);

    // 최종 판정: 카테고리 PASS(전공은 합쳐진 규칙 반영) + 영어 + PF + 총학점 + 토글 2개
    const allCatPass = rows.every((r) => r.status === "PASS");
    const final = allCatPass && englishOk && pfOk && totalOk && gradEnglishPassed && deptExtraPassed;

    return {
      pfCredits: pf,
      totalCredits: tot,
      gpa: gpaVal,
      engMajorCredits: eMajor,
      engLiberalCredits: eLib,
      englishPass: englishOk,
      pfPass: pfOk,
      totalPass: totalOk,
      finalPass: final,
    };
  }, [courseList, rows, gradEnglishPassed, deptExtraPassed]);

  const statusText = (s: string) => (s === "PASS" ? "합격" : s === "FAIL" ? "불합격" : s || "-");
  const sPass = s.statusPass, sFail = s.statusFail;
  const statusClass = (ok: boolean) => (ok ? sPass : sFail);

  const [open, setOpen] = useState(false);
  const handleSaved = () => {
    qc.invalidateQueries({ queryKey: ["curriculum", sid] });
    qc.invalidateQueries({ queryKey: ["courses-all", sid] });
  };

  if (!sid) return <div className="text-center py-14">로그인 정보를 찾을 수 없습니다.</div>;
  if (isLoading) return <div className="text-center py-14">불러오는 중…</div>;
  if (isError) return <div className="text-center py-14">조회 실패</div>;

  // 화면 표시용: 지금 기준 P/F 하한(총 취득학점 30%)
  const pfLimitNote = Math.floor(totalCredits * 0.3);

  return (
    <div className="relative">
      <div className={s.card}>
        <div className={s.tableWrap}>
          <table className={s.table}>
            <thead>
              <tr>
                <th className={s.th} style={{ width: "32%" }}>카테고리</th>
                <th className={s.th} style={{ width: "20%" }}>졸업기준(설계)</th>
                <th className={s.th} style={{ width: "16%" }}>취득 학점</th>
                <th className={s.th} style={{ width: "16%" }}>상태</th>
                <th className={s.th} style={{ width: "16%" }}>상세</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, i) => (
                <tr key={row.key} className={i % 2 ? s.rowEven : undefined}>
                  <td className={s.td}>{row.name}</td>
                  <td className={s.td} style={{ whiteSpace: "nowrap" }}>{row.grad}</td>

                  {/* 전공은 전공학점(설계학점) */}
                  <td className={s.td}>
                    {row.key === "MAJOR"
                      ? `${row.earned}(${row.designedEarned ?? 0})`
                      : row.earned}
                  </td>

                  <td className={`${s.td} ${row.status === "PASS" ? sPass : sFail}`}>
                    {statusText(row.status)}
                  </td>
                  <td className={s.td}>
                    <button
                      onClick={() => nav(`/curriculum/${row.key.toLowerCase()}`)}
                      className={s.viewBtn}
                    >
                      보기
                    </button>
                  </td>
                </tr>
              ))}

              {/* ----- 요약 섹션 ----- */}
              <tr className={s.summarySep}><td colSpan={5} /></tr>

              <tr>
                <td className={s.tdLabel}>P/F과목 총이수학점</td>
                <td className={s.tdNote}>총 취득학점의 30% 기준: {pfLimitNote < 39 ? 39 : pfLimitNote}학점 이하</td>
                <td className={s.tdValue}>{pfCredits}</td>
                <td className={`${s.td} ${statusClass(pfPass)}`}>{pfPass ? "합격" : "불합격"}</td>
                <td className={s.td} />
              </tr>

              <tr>
                <td className={s.tdLabel}>총 취득학점</td>
                <td className={s.tdNote}>130학점 이상</td>
                <td className={s.tdValue}>{totalCredits}</td>
                <td className={`${s.td} ${statusClass(totalPass)}`}>{totalPass ? "합격" : "불합격"}</td>
                <td className={s.td} />
              </tr>

              <tr>
                <td className={s.tdLabel}>평점 평균</td>
                <td className={s.tdNote}>4.5 만점 환산</td>
                <td className={s.tdValue}>{gpa.toFixed(2)}</td>
                <td className={`${s.td} ${gpa >= 2.0 ? sPass : sFail}`}>{gpa >= 2.0 ? "합격" : "불합격"}</td>
                <td className={s.td} />
              </tr>

              <tr>
                <td className={s.tdLabel}>영어강의 과목이수</td>
                <td className={s.tdNote}>전공:{engMajorCredits} / 교양:{engLiberalCredits}</td>
                <td className={s.tdValue}></td>
                <td className={`${s.td} ${statusClass(englishPass)}`}>{englishPass ? "합격" : "불합격"}</td>
                <td className={s.td} />
              </tr>

              <tr>
                <td className={s.tdLabel}>졸업영어시험</td>
                <td className={s.tdNote}></td>
                <td className={s.tdValue}>
                  <label className={s.toggle}>
                    <input
                      type="checkbox"
                      checked={gradEnglishPassed}
                      onChange={(e) => setGradEnglishPassed(e.target.checked)}
                    />
                    <span />
                  </label>
                </td>
                <td className={`${s.td} ${statusClass(gradEnglishPassed)}`}>{gradEnglishPassed ? "합격" : "불합격"}</td>
                <td className={s.td}>
                  <button
                    className={s.saveBtn}
                    onClick={() => saveToggles.mutate({ gradEnglishPassed, deptExtraPassed })}
                    disabled={saveToggles.isPending}
                  >
                    저장
                  </button>
                </td>
              </tr>

              <tr>
                <td className={s.tdLabel}>학부추가졸업요건</td>
                <td className={s.tdNote}></td>
                <td className={s.tdValue}>
                  <label className={s.toggle}>
                    <input
                      type="checkbox"
                      checked={deptExtraPassed}
                      onChange={(e) => setDeptExtraPassed(e.target.checked)}
                    />
                    <span />
                  </label>
                </td>
                <td className={`${s.td} ${statusClass(deptExtraPassed)}`}>{deptExtraPassed ? "합격" : "불합격"}</td>
                <td className={s.td}>
                  <button
                    className={s.saveBtn}
                    onClick={() => saveToggles.mutate({ gradEnglishPassed, deptExtraPassed })}
                    disabled={saveToggles.isPending}
                  >
                    저장
                  </button>
                </td>
              </tr>

              <tr className={s.summaryFinal}>
                <td className={s.tdLabel}>공학인증 최종 졸업판정</td>
                <td className={s.tdNote}></td>
                <td className={s.tdValue}></td>
                <td className={`${s.td} ${statusClass(finalPass)}`}>
                  {finalPass ? "졸업가능" : "졸업불가능"}
                </td>
                <td className={s.td} />
              </tr>
            </tbody>
          </table>
        </div>

        <div className={s.plusArea}>
          <button
            onClick={() => setOpen(true)}
            aria-label="과목 추가"
            title="과목 추가"
            className={s.plusBtn}
          >
            +
          </button>
        </div>
      </div>

      <AddCourseModal open={open} sid={sid} onClose={() => setOpen(false)} onSaved={handleSaved} />
    </div>
  );
}
