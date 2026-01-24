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
import { isGuestMode } from "../../lib/auth";
import {
  loadGuestCourses,
  loadGuestToggles,
  saveGuestToggles,
} from "./guest/guestStorage";
import {
  createEmptySummary,
  computeGuestSummary,
} from "./guest/guestSummary";
import { fireConfetti } from "../../components/confetti";
import { useOverlayUI } from "../../ui/OverlayUIContext";
import CourseRankingSection from "./ranking/CourseRankingSection";

type View = "summary" | "semester";

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
  const { isRankingOpen, closeRanking } = useOverlayUI();
  const isGuest = isGuestMode();
  const realSid = getStudentId();
  const sid = isGuest ? "guest" : realSid || "";

  const qc = useQueryClient();
  const [view, setView] = useState<View>("summary");

  // 게스트용 상태
  const [guestCourses, setGuestCourses] = useState<CourseDto[]>([]);
  const [guestSummary, setGuestSummary] =
    useState<SummaryDto>(createEmptySummary());

  const [gradEnglishPassed, setGradEnglishPassed] = useState(false);

  // 🔹 게스트 모드 초기 로드
  useEffect(() => {
    if (!isGuest) return;

    const cs = loadGuestCourses();
    setGuestCourses(cs);

    const savedToggles = loadGuestToggles();
    if (savedToggles) {
      setGradEnglishPassed(savedToggles.gradEnglishPassed);
    }

    const baseSummary = computeGuestSummary(
      cs,
      savedToggles ?? {
        gradEnglishPassed: false,
      }
    );
    setGuestSummary(baseSummary);
  }, [isGuest]);

  // 🔹 서버 요약(로그인 전용)
  const { data: summary, isLoading, isError } = useQuery<SummaryDto>({
    queryKey: ["summary", sid],
    enabled: !!sid && !isGuest,
    queryFn: async () => {
      const { data } = await axiosInstance.get<SummaryDto>(
        `/api/v1/students/${encodeURIComponent(sid)}/summary`
      );
      return data;
    },
    refetchOnWindowFocus: false,
  });

  // 🔹 서버 과목 목록(로그인 전용)
  const {
    data: serverCourses = [],
    isLoading: isLoadingSem,
    isError: isErrorSem,
  } = useQuery<CourseDto[]>({
    queryKey: ["courses-semester", sid],
    enabled: !!sid && !isGuest,
    queryFn: async () => {
      const { data } = await axiosInstance.get<CourseDto[]>(
        `/api/v1/students/${encodeURIComponent(sid)}/courses/all`
      );
      return data;
    },
  });

  // ✅ 최종 과목 목록 (게스트/로그인 공용)
  const allCourses: CourseDto[] = isGuest ? guestCourses : serverCourses;

  // ✅ 최종 summary (게스트/로그인 공용)
  const effectiveSummary: SummaryDto = isGuest
    ? guestSummary
    : summary ?? createEmptySummary();

  // 🔹 학기별 그룹 (서버 + planned)
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

  const mergedGroups = useMemo(() => {
    const map = new Map<
      string,
      { key: string; year: number; term: Term; items: CourseDto[] }
    >();

    // 1) 서버 그룹
    for (const g of serverGroups) {
      map.set(g.key, { ...g, items: [...g.items] });
    }

    // 2) 새 학기(planned) 그룹 머지
    for (const g of planned) {
      const ex = map.get(g.key);
      if (!ex) {
        map.set(g.key, { ...g, items: [...g.items] });
      } else if (g.items.length) {
        ex.items = [...ex.items, ...g.items];
      }
    }

    return Array.from(map.values()).sort((a, b) =>
      a.year === b.year
        ? TERM_ORDER[a.term] - TERM_ORDER[b.term]
        : a.year - b.year
    );
  }, [serverGroups, planned]);

  // 🔹 서버 summary 기준 토글 초기값 (로그인일 때만)
  useEffect(() => {
    if (isGuest) return;
    if (effectiveSummary) {
      setGradEnglishPassed(!!effectiveSummary.gradEnglishPassed);
    }
  }, [isGuest, effectiveSummary]);

  // 🔹 토글 저장 (로그인 전용)
  const saveToggles = useMutation({
    mutationFn: async (payload: { gradEnglishPassed: boolean }) => {
      if (isGuest) return;
      await axiosInstance.patch(
        `/api/v1/students/${sid}/summary/toggles`,
        payload
      );
    },
    onSuccess: () => {
      if (!isGuest) {
        qc.invalidateQueries({ queryKey: ["summary", sid] });
      }
    },
  });

  const handleSaveToggles = () => {
    if (isGuest) {
      const toggles = { gradEnglishPassed };
      const next = computeGuestSummary(guestCourses, toggles);
      setGuestSummary(next);
      saveGuestToggles(toggles);
    } else {
      saveToggles.mutate({ gradEnglishPassed });
    }
  };

  // 🔹 과목 추가 모달
  const [addOpen, setAddOpen] = useState(false);
  const [prefill, setPrefill] = useState<{ year?: number; term?: Term }>({});
  const openAddFor = (year?: number, term?: Term) => {
    setPrefill({ year, term });
    setAddOpen(true);
  };
  const closeAdd = () => setAddOpen(false);

  const reloadGuestData = () => {
    if (!isGuest) return;
    const cs = loadGuestCourses();
    setGuestCourses(cs);
    const nextSummary = computeGuestSummary(cs, {
      gradEnglishPassed,
    });
    setGuestSummary(nextSummary);
  };


  const afterAddSaved = () => {
    if (isGuest) {
      reloadGuestData();
    } else {
      qc.invalidateQueries({ queryKey: ["summary", sid] });
      qc.invalidateQueries({ queryKey: ["courses-semester", sid] });
    }
    setAddOpen(false);
  };

  // 🔹 새 학기 생성
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
      // 이미 존재하면 그대로 그 학기에 과목 추가만
      openAddFor(ny, nt);
      return;
    }
    // 없으면 planned에 빈 학기 하나 추가
    setPlanned((prev) => [...prev, { key, year: ny, term: nt, items: [] }]);
    openAddFor(ny, nt);
  };

  // 🎉 축하 배너 & 컨페티 (학번별로 딱 한 번만) — 게스트는 finalPass가 항상 false라 자연스럽게 안 뜸
  const hasCelebratedRef = useRef(false);
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => {
    if (!summary || !realSid) return; // ✅ 실제 로그인 유저만 축하 고려
    if (!summary.finalPass) return;

    const key = celebrateKey(realSid);

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
  }, [summary?.finalPass, realSid]);

  // ✅ 플로팅 FAB 상태
  const [fabOpen, setFabOpen] = useState(false);
  const [aiCaptureOpen, setAiCaptureOpen] = useState(false);

  // 🔥 "로그인도 아니고 게스트도 아닐 때만" 오류 메시지
  if (!sid && !isGuest)
    return (
      <div className="text-center py-14">로그인 정보를 찾을 수 없습니다.</div>
    );

  // 서버 기반 요약 로딩 상태는 로그인 사용자에게만 의미 있음
  if (!isGuest && isLoading)
    return <div className="text-center py-14">불러오는 중…</div>;
  if (!isGuest && (isError || !effectiveSummary))
    return <div className="text-center py-14">조회 실패</div>;

  const pfLimitNote = Math.max(39, effectiveSummary.pfLimit);


  return (
    <div className={s.pageRoot}>
      <div
        className={`${s.rankingDim} ${isRankingOpen ? s.rankingDimOn : ""}`}
        onClick={closeRanking}
        aria-hidden={!isRankingOpen}
      />

      <div className={s.pageRow}>
        <div className={s.leftPane}>
          <div className={`${s.leftGrid} ${isRankingOpen ? s.leftGridOpen : ""}`}>
            <div className={s.leftMain}>
              <div className="relative">
                {showBanner && (
                  <div className={s.congratsBanner} role="status">
                    🎓 졸업을 축하합니다!
                  </div>
                )}

                <div className={s.ribbonWrap}>
                  <button
                    className={`${s.ribbon} ${s.ribbonLeft} ${view === "summary" ? s.ribbonActive : ""
                      }`}
                    onClick={() => setView("summary")}
                  >
                    종합 보기
                  </button>
                  <button
                    className={`${s.ribbon} ${s.ribbonLeft2} ${view === "semester" ? s.ribbonActive : ""
                      }`}
                    onClick={() => setView("semester")}
                  >
                    학기별 보기
                  </button>
                </div>

                <div className={s.cardWrap}>
                  {view === "summary" && (
                    <div className={s.topRightNote}>25-2 수강편람 참고</div>
                  )}

                  <div className={s.card}>
                    {view === "summary" ? (
                      <SummaryView
                        summary={effectiveSummary}
                        pfLimitNote={pfLimitNote}
                        gradEnglishPassed={gradEnglishPassed}
                        onChangeGradEnglishPassed={setGradEnglishPassed}
                        onClickSaveToggles={handleSaveToggles}
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
                        onGuestChange={reloadGuestData}
                      />
                    )}

                    {/* ✅ 플로팅 FAB – summary 뷰에서만 표시 */}
                    {view === "summary" && (
                      <div className={`${s.plusArea} ${isRankingOpen ? s.plusAreaShifted : ""}`}>
                        {fabOpen && (
                          <div className={s.fabMenu}>
                            <button
                              type="button"
                              className={s.fabItem}
                              onClick={() => {
                                setFabOpen(false);
                                openAddFor(undefined, undefined);
                              }}
                            >
                              단일 과목 추가
                            </button>

                            <button
                              type="button"
                              className={`${s.fabItem} ${s.fabItemNew}`}
                              onClick={() => {
                                setFabOpen(false);
                                if (isGuest) {
                                  alert(
                                    "AI 캡쳐 기능은 로그인 후 이용할 수 있어요.\n로그인 후 다시 시도해 주세요."
                                  );
                                  return;
                                }
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
                </div>

                {/* 안내 */}
                {view === "summary" && (
                  <div className={s.noticeWrap} role="note" aria-label="안내">
                    <p className={s.noticeText}>
                      GRADU는 학업 이수 관리를 돕기 위한 서비스이며, 학교의{" "}
                      <b>공식 시스템이 아닙니다.</b>
                      <br />
                      수강편람/학사 공지와 <b>기준이 달라지거나 반영이 지연</b>될 수
                      있으니,
                      <b> 최종 확인은 학교 공식 자료</b>(수강편람, 졸업심사안내,
                      공지사항)를 기준으로 참고 해주세요.
                    </p>
                  </div>
                )}

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
            </div>

            {/* ✅ 오른쪽: 랭킹 패널 */}
            <aside
              className={`${s.rankingPane} ${isRankingOpen ? s.rankingPaneOn : ""}`}
              aria-hidden={!isRankingOpen}
            >
              <div className={s.rankingPaneInner}>
                <div className={s.rankingSticky}>
                  <div className={s.rankingCardMock}>
                    <div className={s.rankingHeader}>과목 랭킹</div>
                    <div className={s.rankingBody}>
                      <CourseRankingSection />
                    </div>
                  </div>
                </div>
              </div>
            </aside>
          </div>

        </div>
      </div>
    </div>
  );
}
