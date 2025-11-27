// src/pages/CurriculumPage.tsx
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../lib/axios";
import AddCourseModal from "../components/AddCourseModal";
import s from "./CurriculumTable.module.css";

/* ===================== 공통 유틸 ===================== */
const fmtCred = (n?: number | null) => {
  if (n == null || Number.isNaN(n)) return "-";
  const v = Math.round(n * 10) / 10;
  return Number.isInteger(v) ? String(v) : v.toFixed(1);
};

type ConfettiFn = (opts?: any) => void;
let _confetti: ConfettiFn | null = null;
async function getConfetti(): Promise<ConfettiFn> {
  if (_confetti) return _confetti;
  const mod = await import("canvas-confetti");
  _confetti = mod.default;
  return _confetti!;
}
async function fireConfetti(duration = 1800) {
  const confetti = await getConfetti();
  const end = Date.now() + duration;
  (function frame() {
    confetti({ particleCount: 5, angle: 60, spread: 65, origin: { x: 0 } });
    confetti({ particleCount: 5, angle: 120, spread: 65, origin: { x: 1 } });
    if (Date.now() < end) requestAnimationFrame(frame);
  })();
}

/* ===================== 서버 타입 ===================== */
type SummaryRow = {
  key: string;
  name: string;
  grad: string;
  earned: number;
  designedEarned?: number;
  status: "PASS" | "FAIL" | string;
};
type SummaryDto = {
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

/* ===================== 학기/과목 타입 ===================== */
export type Term = "1" | "2" | "sum" | "win";
type CourseLite = {
  id: number;
  name: string;
  category: string;
  credit: number;
  grade: string | null;
  isEnglish: boolean;
  academicYear: number; // ex) 2025
  term: Term;           // '1' | 'sum' | '2' | 'win'
};

const TERM_ORDER: Record<Term, number> = { "1": 0, sum: 1, "2": 2, win: 3 };
const CATEGORY_LABELS: Record<string, string> = {
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

function formatSemester(yy: number, term: Term) {
  const y2 = String(yy).slice(-2);
  const t = term === "1" || term === "2" ? term : term === "sum" ? "summer" : "winter";
  return `${y2}-${t}`;
}
function nextSemester(y: number, t: Term): { year: number; term: Term } {
  if (t === "1") return { year: y, term: "sum" };
  if (t === "sum") return { year: y, term: "2" };
  if (t === "2") return { year: y, term: "win" };
  return { year: y + 1, term: "1" }; // win 다음은 다음해 1학기
}

/* ===================== 뷰 타입 ===================== */
type View = "summary" | "semester";

/* 임시로 만든(아직 서버에 없는) 새 학기 그룹 구조 */
type PlannedGroup = { key: string; year: number; term: Term; items: CourseLite[] };

export default function CurriculumPage() {
  const sid = getStudentId() || "";
  const qc = useQueryClient();
  const nav = useNavigate();

  /* ===== 탭 상태 ===== */
  const [view, setView] = useState<View>("summary");

  /* ===== 요약 데이터 ===== */
  const { data: summary, isLoading, isError } = useQuery<SummaryDto>({
    queryKey: ["summary", sid],
    enabled: !!sid,
    queryFn: async () => {
      const { data } = await axiosInstance.get<SummaryDto>(
        `/api/v1/students/${encodeURIComponent(sid)}/summary`
      );
      return data;
    },
    refetchOnWindowFocus: false,
  });

  /* ===== 학기별: 서버 과목 전체 ===== */
  const {
    data: allCourses = [],
    isLoading: isLoadingSem,
    isError: isErrorSem,
  } = useQuery<CourseLite[]>({
    queryKey: ["courses-semester", sid],
    enabled: !!sid && view === "semester",
    queryFn: async () => {
      const { data } = await axiosInstance.get<CourseLite[]>(
        `/api/v1/students/${encodeURIComponent(sid)}/courses/all`
      );

      // 백엔드 CourseResponseDto 구조에 맞춰 매핑
      return data.map((c: any): CourseLite => ({
        id: c.id,
        name: c.name,
        category: c.category,          // (string or enum name)
        credit: c.credit,
        grade: c.grade ?? null,
        isEnglish: !!c.isEnglish,
        academicYear: c.academicYear,
        term: c.term,                  // '1' | 'sum' | '2' | 'win'
      }));
    },
  });

  /* ===== 서버 과목을 학기 단위로 그룹 ===== */
  const serverGroups = useMemo(() => {
    if (!allCourses?.length) return [] as { key: string; year: number; term: Term; items: CourseLite[] }[];
    const sorted = [...allCourses].sort((a, b) => {
      if (a.academicYear !== b.academicYear) return a.academicYear - b.academicYear;
      return TERM_ORDER[a.term] - TERM_ORDER[b.term];
    });
    const groups: { key: string; year: number; term: Term; items: CourseLite[] }[] = [];
    for (const c of sorted) {
      const key = `${c.academicYear}-${c.term}`;
      const last = groups[groups.length - 1];
      if (last && last.key === key) last.items.push(c);
      else groups.push({ key, year: c.academicYear, term: c.term, items: [c] });
    }
    return groups;
  }, [allCourses]);

  /* ===== 사용자가 방금 만든 임시 학기들 ===== */
  const [planned, setPlanned] = useState<PlannedGroup[]>([]);

  /* ===== 두 소스(서버/임시)를 합친 뷰용 그룹 ===== */
  const mergedGroups = useMemo(() => {
    const all = [...serverGroups, ...planned];
    return all.sort((a, b) => {
      if (a.year !== b.year) return a.year - b.year;
      return TERM_ORDER[a.term] - TERM_ORDER[b.term];
    });
  }, [serverGroups, planned]);

  /* ===== 요약 토글 저장 ===== */
  const [gradEnglishPassed, setGradEnglishPassed] = useState(false);
  const [deptExtraPassed, setDeptExtraPassed] = useState(false);
  useEffect(() => {
    if (summary) {
      setGradEnglishPassed(!!summary.gradEnglishPassed);
      setDeptExtraPassed(!!summary.deptExtraPassed);
    }
  }, [summary]);

  const saveToggles = useMutation({
    mutationFn: async (payload: { gradEnglishPassed: boolean; deptExtraPassed: boolean }) => {
      await axiosInstance.patch(`/api/v1/students/${sid}/summary/toggles`, payload);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["summary", sid] }),
  });

  const statusText = (sTxt: string) =>
    sTxt === "PASS" ? "합격" : sTxt === "FAIL" ? "불합격" : sTxt || "-";
  const statusClass = (ok: boolean) => (ok ? s.statusPass : s.statusFail);

  /* ===== 과목 추가 모달 (초기 학기 프리필) ===== */
  const [addOpen, setAddOpen] = useState(false);
  const [prefill, setPrefill] = useState<{ year?: number; term?: Term }>({});
  function openAddFor(year?: number, term?: Term) {
    setPrefill({ year, term });
    setAddOpen(true);
  }
  const closeAdd = () => setAddOpen(false);

  const afterAddSaved = () => {
    // 서버 요약/학기 목록 갱신
    qc.invalidateQueries({ queryKey: ["summary", sid] });
    qc.invalidateQueries({ queryKey: ["courses-semester", sid] });
    setAddOpen(false);
  };

  /* ===== 새 학기(빈 표) 추가 ===== */
  const lastOfMerged = useMemo(() => {
    if (mergedGroups.length > 0) return mergedGroups[mergedGroups.length - 1];
    // 아무 것도 없으면 현재년도 1학기부터 시작
    const nowY = new Date().getFullYear();
    return { key: `${nowY}-1`, year: nowY, term: "1" as Term, items: [] as CourseLite[] };
  }, [mergedGroups]);

  function handleCreateNextSemester() {
    const { year: ny, term: nt } = nextSemester(lastOfMerged.year, lastOfMerged.term);
    const key = `${ny}-${nt}`;
    // 이미 존재하는 학기면 중복 추가 방지
    if (mergedGroups.some((g) => g.key === key)) {
      // 그래도 모달은 열어준다(바로 과목 추가)
      openAddFor(ny, nt);
      return;
    }
    setPlanned((prev) => [...prev, { key, year: ny, term: nt, items: [] }]);
    openAddFor(ny, nt); // 방금 만든 학기로 프리필
  }

  /* ===== 축하 연출 ===== */
  const hasCelebratedRef = useRef(false);
  const [showBanner, setShowBanner] = useState(false);
  useEffect(() => {
    if (!summary) return;
    if (summary.finalPass && !hasCelebratedRef.current) {
      hasCelebratedRef.current = true;
      fireConfetti(1800);
      setShowBanner(true);
      const t = setTimeout(() => setShowBanner(false), 3000);
      return () => clearTimeout(t);
    }
    if (!summary.finalPass) hasCelebratedRef.current = false;
  }, [summary?.finalPass]);

  /* ===== 가드 ===== */
  if (!sid) return <div className="text-center py-14">로그인 정보를 찾을 수 없습니다.</div>;
  if (isLoading) return <div className="text-center py-14">불러오는 중…</div>;
  if (isError || !summary) return <div className="text-center py-14">조회 실패</div>;

  const pfLimitNote = Math.max(39, summary.pfLimit);

  /* ===================== 렌더 ===================== */
  return (
    <div className="relative">
      {/* 🎉 축하 배너 */}
      {showBanner && (
        <div
          role="status"
          style={{
            position: "fixed",
            top: 16,
            right: 16,
            zIndex: 50,
            background: "linear-gradient(90deg,#fef3c7,#fde68a)",
            border: "1px solid #f59e0b",
            color: "#92400e",
            padding: "10px 14px",
            borderRadius: 12,
            boxShadow: "0 6px 16px rgba(0,0,0,0.15)",
            fontWeight: 700,
          }}
        >
          🎓 졸업을 축하합니다!
        </div>
      )}

      {/* 📌 포스트잇 탭 */}
      <div className={s.ribbonWrap}>
        <button
          type="button"
          className={`${s.ribbon} ${s.ribbonLeft} ${view === "summary" ? s.ribbonActive : ""}`}
          onClick={() => setView("summary")}
        >
          종합 보기
        </button>
        <button
          type="button"
          className={`${s.ribbon} ${s.ribbonLeft2} ${view === "semester" ? s.ribbonActive : ""}`}
          onClick={() => setView("semester")}
        >
          학기별 보기
        </button>
      </div>

      {/* 카드 */}
      <div className={`${s.card} ${view === "summary" ? s.cardOnTop : s.cardDimmed}`}>
        <div className={s.tableWrap}>
          {view === "summary" ? (
            /* ===================== 종합 보기 ===================== */
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
                {summary.rows.map((row, i) => (
                  <tr key={row.key} className={i % 2 ? s.rowEven : undefined}>
                    <td className={s.td}>{row.name}</td>
                    <td className={s.td} style={{ whiteSpace: "nowrap" }}>{row.grad}</td>
                    <td className={s.td}>
                      {row.key === "MAJOR"
                        ? `${fmtCred(row.earned)}(${row.designedEarned ?? 0})`
                        : fmtCred(row.earned)}
                    </td>
                    <td className={`${s.td} ${row.status === "PASS" ? s.statusPass : s.statusFail}`}>
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
                  <td className={s.tdNote}>총 취득학점의 30% 기준: {fmtCred(pfLimitNote)}학점 이하</td>
                  <td className={s.tdValue}>{fmtCred(summary.pfCredits)}</td>
                  <td className={`${s.td} ${summary.pfPass ? s.statusPass : s.statusFail}`}>
                    {summary.pfPass ? "합격" : "불합격"}
                  </td>
                  <td className={s.td} />
                </tr>

                <tr>
                  <td className={s.tdLabel}>총 취득학점</td>
                  <td className={s.tdNote}>130학점 이상</td>
                  <td className={s.tdValue}>{fmtCred(summary.totalCredits)}</td>
                  <td className={`${s.td} ${summary.totalPass ? s.statusPass : s.statusFail}`}>
                    {summary.totalPass ? "합격" : "불합격"}
                  </td>
                  <td className={s.td} />
                </tr>

                <tr>
                  <td className={s.tdLabel}>평점 평균</td>
                  <td className={s.tdNote}>2.0 이상</td>
                  <td className={s.tdValue}>{(summary.gpa ?? 0).toFixed(2)}</td>
                  <td className={`${s.td} ${(summary.gpa ?? 0) >= 2.0 ? s.statusPass : s.statusFail}`}>
                    {(summary.gpa ?? 0) >= 2.0 ? "합격" : "불합격"}
                  </td>
                  <td className={s.td} />
                </tr>

                <tr>
                  <td className={s.tdLabel}>영어강의 과목이수</td>
                  <td className={s.tdNote}>
                    전공:{fmtCred(summary.engMajorCredits)} / 교양:{fmtCred(summary.engLiberalCredits)}
                  </td>
                  <td className={s.tdValue}></td>
                  <td className={`${s.td} ${summary.englishPass ? s.statusPass : s.statusFail}`}>
                    {summary.englishPass ? "합격" : "불합격"}
                  </td>
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
                  <td className={`${s.td} ${statusClass(gradEnglishPassed)}`}>
                    {gradEnglishPassed ? "합격" : "불합격"}
                  </td>
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
                  <td className={`${s.td} ${statusClass(deptExtraPassed)}`}>
                    {deptExtraPassed ? "합격" : "불합격"}
                  </td>
                  <td className={s.td}>
                    <button
                      type="button"
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
                  <td className={`${s.td} ${statusClass(summary.finalPass)}`}>
                    {summary.finalPass ? "졸업가능" : "졸업불가능"}
                  </td>
                  <td className={s.td}></td>
                </tr>
              </tbody>
            </table>
          ) : (
            /* ===================== 학기별 보기 ===================== */
            <div className={s.semesterWrap}>
              {isLoadingSem ? (
                <div className="text-center py-10">학기별 데이터를 불러오는 중…</div>
              ) : isErrorSem ? (
                <div className="text-center py-10">학기별 데이터를 불러오지 못했습니다.</div>
              ) : mergedGroups.length === 0 ? (
                <div className="text-center py-10">등록된 과목이 없습니다.</div>
              ) : (
                <>
                  {mergedGroups.map((g) => (
                    <div className={s.semesterCard} key={g.key}>
                      <div className={s.semesterHeader}>
                        {formatSemester(g.year, g.term)}
                        <button
                          className={s.semesterAddSmall}
                          onClick={() => openAddFor(g.year, g.term)}
                          title="이 학기에 과목 추가"
                        >
                          과목 추가
                        </button>
                      </div>

                      <table className={s.table}>
                        <thead>
                          <tr>
                            <th className={s.th} style={{ width: "40%" }}>과목명</th>
                            <th className={s.th} style={{ width: "20%" }}>카테고리</th>
                            <th className={s.th} style={{ width: "12%" }}>학점</th>
                            <th className={s.th} style={{ width: "14%" }}>성적</th>
                            <th className={s.th} style={{ width: "14%" }}>작업</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(g.items.length ? g.items : []).map((c, idx) => (
                            <tr key={c.id ?? `${c.name}-${idx}`} className={idx % 2 ? s.rowEven : undefined}>
                              <td className={s.td}>
                                {c.name} {c.isEnglish ? <span className={s.badgeEng}>ENG</span> : null}
                              </td>
                              <td className={s.td}>{CATEGORY_LABELS[c.category] ?? c.category}</td>
                              <td className={s.td}>{fmtCred(c.credit)}</td>
                              <td className={s.td}>{c.grade ?? "-"}</td>
                              <td className={s.td}>
                                <button
                                  className={s.viewBtn}
                                  onClick={() => nav(`/curriculum/${c.category.toLowerCase()}`)}
                                  title="카테고리 상세로 이동"
                                >
                                  보기
                                </button>
                              </td>
                            </tr>
                          ))}
                          {/* 임시로 만든 빈 표에도 안내 한 줄 */}
                          {g.items.length === 0 && (
                            <tr>
                              <td className={s.td} colSpan={5} style={{ color: "#6b7280" }}>
                                이 학기에 아직 과목이 없습니다. &nbsp;
                                <button className={s.viewBtn} onClick={() => openAddFor(g.year, g.term)}>
                                  과목 추가
                                </button>
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  ))}

                  {/* 하단 바 버튼 (파란 점선) */}
                  {view === "semester" && (
                    <div className={s.semesterAddBar}>
                      <button
                        type="button"
                        className={s.semesterAddBtn}
                        onClick={handleCreateNextSemester}
                        aria-label="새 학기 시간표 만들기"
                        title="새 학기 시간표 만들기"
                      >
                        + 새 학기 시간표 만들기
                      </button>
                    </div>
                  )}

                </>
              )}
            </div>
          )}
        </div>

        {/* + 버튼은 종합 보기에서만 노출, 카드 우하단 */}
        {view === "summary" && (
          <div className={s.plusArea}>
            <button
              onClick={() => openAddFor(undefined, undefined)}
              aria-label="과목 추가"
              title="과목 추가"
              className={s.plusBtn}
            >
              +
            </button>
          </div>
        )}
      </div>

      {/* 과목 추가 모달 (년도/학기 프리필) */}
      <AddCourseModal
        open={addOpen}
        sid={sid}
        onClose={closeAdd}
        onSaved={afterAddSaved}
        initialYear={prefill.year}
        initialTerm={prefill.term}
      />
    </div>
  );
}
