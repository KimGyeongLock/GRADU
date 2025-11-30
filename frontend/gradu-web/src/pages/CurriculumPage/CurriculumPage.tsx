// src/pages/CurriculumPage/CurriculumPage.tsx
import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../../lib/axios";
import AddCourseModal from "./modal/AddCourseModal";
import {
  type SummaryDto,
  type CourseDto,
  type Term,
  TERM_ORDER,
} from "./curriculumTypes";
import { SummaryView } from "./SummaryView";
import { SemesterView } from "./SemesterView";
import s from "./CurriculumTable.module.css";
import { AiCaptureModal } from "./modal/AiCaptureModal";

/* confetti util은 그대로 위쪽에 선언 */
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

type View = "summary" | "semester";

// ✅ PlannedGroup도 CourseDto 기반으로 변경
type PlannedGroup = { key: string; year: number; term: Term; items: CourseDto[] };

function nextSemester(y: number, t: Term): { year: number; term: Term } {
  if (t === "1") return { year: y, term: "sum" };
  if (t === "sum") return { year: y, term: "2" };
  if (t === "2") return { year: y, term: "win" };
  return { year: y + 1, term: "1" };
}

// 🎉 학번별로 “축하 이미 함” 여부를 저장할 localStorage key
const celebrateKey = (sid: string) => `gradu_celebrated_${sid}`;

export default function CurriculumPage() {
  const sid = getStudentId() || "";
  const qc = useQueryClient();

  const [view, setView] = useState<View>("summary");

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

  const {
    data: allCourses = [],
    isLoading: isLoadingSem,
    isError: isErrorSem,
  } = useQuery<CourseDto[]>({
    queryKey: ["courses-semester", sid],
    enabled: !!sid && view === "semester",
    queryFn: async () => {
      const { data } = await axiosInstance.get<CourseDto[]>(
        `/api/v1/students/${encodeURIComponent(sid)}/courses/all`
      );
      return data;
    },
  });

  const serverGroups = useMemo(() => {
    if (!allCourses?.length) return [] as PlannedGroup[];
    const sorted = [...allCourses].sort((a, b) => {
      if (a.academicYear !== b.academicYear)
        return a.academicYear - b.academicYear;
      return TERM_ORDER[a.term] - TERM_ORDER[b.term];
    });
    const groups: PlannedGroup[] = [];
    for (const c of sorted) {
      const key = `${c.academicYear}-${c.term}`;
      const last = groups[groups.length - 1];
      if (last && last.key === key) last.items.push(c);
      else groups.push({ key, year: c.academicYear, term: c.term, items: [c] });
    }
    return groups;
  }, [allCourses]);

  const [planned, setPlanned] = useState<PlannedGroup[]>([]);
  const mergedGroups = useMemo(
    () =>
      [...serverGroups, ...planned].sort((a, b) =>
        a.year === b.year
          ? TERM_ORDER[a.term] - TERM_ORDER[b.term]
          : a.year - b.year
      ),
    [serverGroups, planned]
  );

  const [gradEnglishPassed, setGradEnglishPassed] = useState(false);
  const [deptExtraPassed, setDeptExtraPassed] = useState(false);

  useEffect(() => {
    if (summary) {
      setGradEnglishPassed(!!summary.gradEnglishPassed);
      setDeptExtraPassed(!!summary.deptExtraPassed);
    }
  }, [summary]);

  const saveToggles = useMutation({
    mutationFn: async (payload: {
      gradEnglishPassed: boolean;
      deptExtraPassed: boolean;
    }) => {
      await axiosInstance.patch(
        `/api/v1/students/${sid}/summary/toggles`,
        payload
      );
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["summary", sid] }),
  });

  const [addOpen, setAddOpen] = useState(false);
  const [prefill, setPrefill] = useState<{ year?: number; term?: Term }>({});
  const openAddFor = (year?: number, term?: Term) => {
    setPrefill({ year, term });
    setAddOpen(true);
  };
  const closeAdd = () => setAddOpen(false);
  const afterAddSaved = () => {
    qc.invalidateQueries({ queryKey: ["summary", sid] });
    qc.invalidateQueries({ queryKey: ["courses-semester", sid] });
    setAddOpen(false);
  };

  const lastOfMerged = useMemo(() => {
    if (mergedGroups.length > 0) return mergedGroups[mergedGroups.length - 1];
    const nowY = new Date().getFullYear();
    return { key: `${nowY}-1`, year: nowY, term: "1" as Term, items: [] };
  }, [mergedGroups]);

  const handleCreateNextSemester = () => {
    const { year: ny, term: nt } = nextSemester(
      lastOfMerged.year,
      lastOfMerged.term
    );
    const key = `${ny}-${nt}`;
    if (mergedGroups.some((g) => g.key === key)) {
      openAddFor(ny, nt);
      return;
    }
    setPlanned((prev) => [...prev, { key, year: ny, term: nt, items: [] }]);
    openAddFor(ny, nt);
  };

  // 🎉 축하 배너 & 컨페티 (학번별로 딱 한 번만)
  const hasCelebratedRef = useRef(false);
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => {
    if (!summary || !sid) return;

    if (!summary.finalPass) return;

    const key = celebrateKey(sid);

    const checkAlreadyCelebrated = (storageKey: string): boolean => {
      try {
        if (typeof window === "undefined") return false;
        return window.localStorage.getItem(storageKey) === "1";
      } catch {
        return false;
      }
    };
    const alreadyCelebrated = checkAlreadyCelebrated(key);

    if (alreadyCelebrated || hasCelebratedRef.current) {
      return;
    }

    hasCelebratedRef.current = true;
    try {
      if (typeof window !== "undefined") {
        window.localStorage.setItem(key, "1");
      }
    } catch {
      // ignore
    }

    fireConfetti(1800);
    setShowBanner(true);
    const t = setTimeout(() => setShowBanner(false), 3000);
    return () => clearTimeout(t);
  }, [summary?.finalPass, sid]);

  // ✅ 플로팅 FAB 상태
  const [fabOpen, setFabOpen] = useState(false);
  const [aiCaptureOpen, setAiCaptureOpen] = useState(false); // AI 캡쳐 모달 열기용

  if (!sid)
    return (
      <div className="text-center py-14">로그인 정보를 찾을 수 없습니다.</div>
    );
  if (isLoading)
    return <div className="text-center py-14">불러오는 중…</div>;
  if (isError || !summary)
    return <div className="text-center py-14">조회 실패</div>;

  const pfLimitNote = Math.max(39, summary.pfLimit);

  return (
    <div className="relative">
      {showBanner && (
        <div className={s.congratsBanner} role="status">
          🎓 졸업을 축하합니다!
        </div>
      )}

      <div className={s.ribbonWrap}>
        <button
          className={`${s.ribbon} ${s.ribbonLeft} ${
            view === "summary" ? s.ribbonActive : ""
          }`}
          onClick={() => setView("summary")}
        >
          종합 보기
        </button>
        <button
          className={`${s.ribbon} ${s.ribbonLeft2} ${
            view === "semester" ? s.ribbonActive : ""
          }`}
          onClick={() => setView("semester")}
        >
          학기별 보기
        </button>
      </div>

      <div className={s.card}>
        {view === "summary" ? (
          <SummaryView
            summary={summary}
            pfLimitNote={pfLimitNote}
            gradEnglishPassed={gradEnglishPassed}
            deptExtraPassed={deptExtraPassed}
            onChangeGradEnglishPassed={setGradEnglishPassed}
            onChangeDeptExtraPassed={setDeptExtraPassed}
            onClickSaveToggles={() =>
              saveToggles.mutate({ gradEnglishPassed, deptExtraPassed })
            }
            savingToggles={saveToggles.isPending}
          />
        ) : (
          <SemesterView
            mergedGroups={mergedGroups}
            isLoadingSem={isLoadingSem}
            isErrorSem={isErrorSem}
            view={view}
            onOpenAddFor={openAddFor}
            onCreateNextSemester={handleCreateNextSemester}
          />
        )}

        {/* ✅ 플로팅 FAB – summary 뷰에서만 표시 */}
        {view === "summary" && (
          <div className={s.plusArea}>
            {fabOpen && (
              <div className={s.fabMenu}>
                <button
                  type="button"
                  className={s.fabItem}
                  onClick={() => {
                    setFabOpen(false);
                    openAddFor(undefined, undefined); // 기존 단일 과목 추가
                  }}
                >
                  단일 과목 추가
                </button>
                <button
                  type="button"
                  className={`${s.fabItem} ${s.fabItemNew}`}
                  onClick={() => {
                    setFabOpen(false);
                    setAiCaptureOpen(true);
                  }}
                >
                  <span className={s.newBadge}>NEW</span>
                  <span>AI 캡쳐로 일괄 추가</span>
                </button>
              </div>
            )}

            <button
              type="button"
              onClick={() => setFabOpen((prev) => !prev)}
              className={`${s.plusBtn} ${fabOpen ? s.plusBtnOpen : ""}`}
              aria-label={fabOpen ? "메뉴 닫기" : "과목 추가 옵션 열기"}
              aria-expanded={fabOpen}
            >
              <span className={s.plusIcon} />
            </button>
          </div>
        )}
      </div>

      <AddCourseModal
        open={addOpen}
        sid={sid}
        onClose={closeAdd}
        onSaved={afterAddSaved}
        initialYear={prefill.year}
        initialTerm={prefill.term}
      />

      <AiCaptureModal
        open={aiCaptureOpen}
        sid={sid}
        onClose={() => setAiCaptureOpen(false)}
        onSaved={afterAddSaved}
        exampleImageUrl="/course_example.png"
      />
    </div>
  );
}
