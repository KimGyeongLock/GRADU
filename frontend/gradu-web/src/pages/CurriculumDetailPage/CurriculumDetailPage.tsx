import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../../lib/axios";
import EditCourseModal from "../CurriculumPage/modal/EditCourseModal";
import s from "./CurriculumDetail.module.css";
import type { CourseDto } from "../CurriculumPage/curriculumTypes";
import {
  formatSemester,
  CATEGORY_LABELS,
} from "../CurriculumPage/curriculumTypes";
import { isGuestMode } from "../../lib/auth";
import {
  loadGuestCourses,
  removeGuestCourse,
} from "../CurriculumPage/guest/guestStorage";
import { CourseInfoBox } from "./components/CourseInfoBox";
import { MajorInfoBox } from "./components/MajorInfoBox";
import { FaithInfoBox } from "./components/FaithInfoBox";
import { PersonalityInfoBox } from "./components/PersonalityInfoBox";

import { GENERAL_EDU_COURSES } from "./constants/generalEdu";
import { BSM_MATH_COURSES, BSM_COURSE_ALIASES } from "./constants/bsm";
import { MAJOR_ELECTIVE_REQUIRED, MAJOR_COURSE_ALIASES } from "./constants/major";
import { PRACTICAL_ENGLISH_COURSES } from "./constants/practicalEnglish";
import { ICT_INTRO_COURSES } from "./constants/ictIntro";

export const CATEGORY_ORDER = Object.keys(CATEGORY_LABELS);
const ALLOWED = new Set(CATEGORY_ORDER);

export default function CurriculumDetailPage() {
  const { category = "" } = useParams();
  const isGuest = isGuestMode();
  const realSid = getStudentId();
  const sid = isGuest ? "guest" : realSid || "";

  const nav = useNavigate();
  const qc = useQueryClient();

  const categoryEnum = useMemo(
    () => category.toUpperCase().replace(/-/g, "_"),
    [category]
  );

  const isValid = ALLOWED.has(categoryEnum);
  const label = isValid ? CATEGORY_LABELS[categoryEnum] : categoryEnum;

  const isMajor = categoryEnum === "MAJOR";
  const isGeneralEdu = categoryEnum === "GENERAL_EDU";
  const isBSM = categoryEnum === "BSM";
  const isFaith = categoryEnum === "FAITH_WORLDVIEW";
  const isPersonality = categoryEnum === "PERSONALITY_LEADERSHIP";
  const isPracticalEnglish = categoryEnum === "PRACTICAL_ENGLISH";
  const isIctIntro = categoryEnum === "ICT_INTRO";

  // 🔹 게스트용 로컬 과목 목록
  const [guestCourses, setGuestCourses] = useState<CourseDto[]>([]);

  useEffect(() => {
    if (isGuest) {
      setGuestCourses(loadGuestCourses());
    }
  }, [isGuest]);

  // 🔹 서버에서 카테고리별 과목 조회 (로그인 사용자만)
  const {
    data: serverCourses = [],
    isLoading,
    isError,
    error,
  } = useQuery<CourseDto[]>({
    queryKey: ["courses-by-category", sid, categoryEnum],
    enabled: !!sid && !isGuest && isValid,
    queryFn: async () => {
      const url = `/api/v1/students/${encodeURIComponent(
        sid
      )}/courses/categories/${encodeURIComponent(categoryEnum)}`;
      const { data } = await axiosInstance.get<CourseDto[]>(url);
      return data ?? [];
    },
  });

  // ✅ 실제 화면에 쓸 리스트 (게스트/로그인 공용)
  const list: CourseDto[] = useMemo(() => {
    if (!isValid) return [];
    if (isGuest) {
      return guestCourses.filter((c) => c.category === categoryEnum);
    }
    return serverCourses;
  }, [isGuest, guestCourses, serverCourses, categoryEnum, isValid]);

  // ✅ 전문교양 칩 하이라이트용: 이미 이수한 과목 이름 Set
  const normalize = (str: string) =>
    str.trim().replace(/\s+/g, "").toUpperCase();

  const takenGeneralEduSet = useMemo(() => {
    if (!isGeneralEdu) return new Set<string>();
    return new Set(list.map((c) => normalize(c.name)));
  }, [isGeneralEdu, list]);

  const takenBsmMathSet = useMemo(() => {
    if (!isBSM) return new Set<string>();
    return new Set(list.map((c) => normalize(c.name)));
  }, [isBSM, list]);

  const takenMajorSet = useMemo(() => {
    if (!isMajor) return new Set<string>();
    return new Set(list.map((c) => normalize(c.name)));
  }, [isMajor, list]);

  const takenPersonalitySet = useMemo(() => {
    if (!isPersonality) return new Set<string>();
    return new Set(list.map((c) => normalize(c.name)));
  }, [isPersonality, list]);

  // ---------------------------
  // ✅ 가이드 표시/이수 판정 resolver
  // ---------------------------
  const hasHangul = (str: string) => /[가-힣]/.test(str);

  function buildGuideDisplayResolver(
    list: CourseDto[],
    normalize: (s: string) => string,
    aliases: Record<string, string[]>
  ) {
    const takenByNorm = new Map<string, string>();
    for (const c of list) takenByNorm.set(normalize(c.name), c.name);

    const resolveDisplayName = (guideName: string) => {
      // ✅ "이미 과목 이름이 영어"인 가이드 항목은 무시 (그대로 보여줌)
      if (!hasHangul(guideName)) return guideName;

      // 1) 한국어 그대로 들었으면 한국어로 표시(=실제 이름)
      const direct = takenByNorm.get(normalize(guideName));
      if (direct) return direct;

      // 2) 영어 alias로 들었으면, 실제 저장된 영어 이름으로 표시
      const aliasList = aliases[guideName] ?? [];
      for (const a of aliasList) {
        const hit = takenByNorm.get(normalize(a));
        if (hit) return hit;
      }

      // 3) 아직 안 들었으면 한국어 디폴트 유지
      return guideName;
    };

    const isTaken = (guideName: string) => {
      // 가이드가 영어인 경우: 원래 방식대로만 체크(변환 없음)
      if (!hasHangul(guideName)) {
        return takenByNorm.has(normalize(guideName));
      }

      if (takenByNorm.has(normalize(guideName))) return true;

      const aliasList = aliases[guideName] ?? [];
      return aliasList.some((a) => takenByNorm.has(normalize(a)));
    };

    return { resolveDisplayName, isTaken };
  }

  // ✅ BSM: alias 적용 예시 (다른 카테고리도 동일하게 확장 가능)
  const bsmGuide = useMemo(() => {
    if (!isBSM) return null;
    return buildGuideDisplayResolver(list, normalize, BSM_COURSE_ALIASES);
  }, [isBSM, list]);
  const majorGuide = useMemo(() => {
    if (!isMajor) return null;
    return buildGuideDisplayResolver(list, normalize, MAJOR_COURSE_ALIASES);
  }, [isMajor, list]);

  const majorElectiveTakenCount = useMemo(() => {
    if (!isMajor) return 0;

    const isTakenFn =
      majorGuide?.isTaken ?? ((name: string) => takenMajorSet.has(normalize(name)));

    return MAJOR_ELECTIVE_REQUIRED.filter((name) => isTakenFn(name)).length;
  }, [isMajor, majorGuide, takenMajorSet]);

  const takenPracticalEnglishSet = useMemo(() => {
    if (!isPracticalEnglish) return new Set<string>();
    return new Set(list.map((c) => normalize(c.name)));
  }, [isPracticalEnglish, list]);

  const takenIctIntroSet = useMemo(() => {
    if (!isIctIntro) return new Set<string>();
    return new Set(list.map((c) => normalize(c.name)));
  }, [isIctIntro, list]);




  // 삭제 (로그인 사용자)
  const deleteMutation = useMutation({
    mutationFn: async (courseId: number) => {
      const url = `/api/v1/students/${encodeURIComponent(
        sid
      )}/courses/${courseId}`;
      await axiosInstance.delete(url);
    },
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["courses-by-category", sid, categoryEnum],
      });
      qc.invalidateQueries({ queryKey: ["summary", sid] });
      qc.invalidateQueries({ queryKey: ["courses-semester", sid] });
    },
  });

  const handleDelete = (course: CourseDto) => {
    if (!window.confirm(`"${course.name}" 과목을 삭제할까요?`)) return;
    if (!course.id) return;

    if (isGuest) {
      // 🔹 게스트: 로컬에서 삭제 후 상태 갱신
      removeGuestCourse(course.id);
      const cs = loadGuestCourses();
      setGuestCourses(cs);
    } else {
      deleteMutation.mutate(course.id);
    }
  };

  // 수정 모달 상태
  const [editing, setEditing] = useState<CourseDto | null>(null);
  const closeEdit = () => setEditing(null);
  const handleEdited = () => {
    if (isGuest) {
      // 🔹 게스트: 로컬에서 다시 읽어오기
      const cs = loadGuestCourses();
      setGuestCourses(cs);
    } else {
      qc.invalidateQueries({
        queryKey: ["courses-by-category", sid, categoryEnum],
      });
      qc.invalidateQueries({ queryKey: ["summary", sid] });
      qc.invalidateQueries({ queryKey: ["courses-semester", sid] });
    }
    closeEdit();
  };

  // ❌ 완전 비로그인 + 게스트 모드도 아닐 때만 안내
  if (!sid && !isGuest) {
    return <div className={s.centerNotice}>로그인 정보를 찾을 수 없습니다.</div>;
  }

  if (!isValid) {
    return (
      <div className={s.centerNotice}>
        잘못된 카테고리입니다: <b>{category}</b>
      </div>
    );
  }

  const loading = !isGuest && isLoading;
  const errorState = !isGuest && isError;

  return (
    <div className={s.page}>
      {/* 상단 */}
      <div className={s.header}>
        <div>
          <div className={s.titleSub}>카테고리 상세</div>
          <h2 className={s.title}>{label}</h2>
        </div>
        <button onClick={() => nav(-1)} className={s.backBtn}>
          뒤로
        </button>
      </div>

      {/* 이수 안내 가이드 */}
      {isGeneralEdu && (
        <CourseInfoBox
          title="전문교양 이수 안내"
          description="전문교양은 아래 과목들 중에서 선택하여 이수하시면 됩니다."
          courses={GENERAL_EDU_COURSES}
          takenSet={takenGeneralEduSet}
          normalize={normalize}
        />
      )}

      {isBSM && (
        <CourseInfoBox
          title="BSM 이수 안내"
          description={
            <>
              <p>BSM은 아래 과목들 중에서 선택하여 이수하시면 됩니다.</p>
              <p>
                <b>- (물리학개론 + 물리학실험1)</b> 또는{" "}
                <b>(물리학1 + 물리학실험1)</b> 또는{" "}
                <b>(물리학2 + 물리학실험1)</b> 또는{" "}
                <b>(일반화학 + 일반화학실험)</b> 중 <b>하나 이상 필수 이수</b>
              </p>
              <p>
                <b>- 이산수학 필수 이수</b>
              </p>
            </>
          }
          courses={BSM_MATH_COURSES}
          takenSet={takenBsmMathSet} // fallback용(있어도 됨)
          normalize={normalize}
          resolveDisplayName={bsmGuide?.resolveDisplayName}
          isTaken={bsmGuide?.isTaken}
        />
      )}

      {isMajor && (
        <MajorInfoBox
          takenSet={takenMajorSet}
          normalize={normalize}
          electiveTakenCount={majorElectiveTakenCount}
          resolveDisplayName={majorGuide?.resolveDisplayName}
          isTaken={majorGuide?.isTaken}
        />
      )}

      {isFaith && (
        <FaithInfoBox
          takenSet={new Set(list.map((c) => normalize(c.name)))}
          normalize={normalize}
        />
      )}

      {categoryEnum === "PERSONALITY_LEADERSHIP" && (
        <PersonalityInfoBox takenSet={takenPersonalitySet} normalize={normalize} />
      )}

      {isPracticalEnglish && (
        <CourseInfoBox
          title="실무영어 이수 안내"
          description="실무영어는 아래 과목들을 순서대로 이수하시면 됩니다.  ※ EAP 필수"
          courses={PRACTICAL_ENGLISH_COURSES}
          takenSet={takenPracticalEnglishSet}
          normalize={normalize}
        />
      )}

      {isIctIntro && (
        <CourseInfoBox
          title="ICT융합기초 이수 안내"
          description="ICT융합기초는 아래 과목들 중에서 선택하여 이수하시면 됩니다."
          courses={ICT_INTRO_COURSES}
          takenSet={takenIctIntroSet}
          normalize={normalize}
        />
      )}

      {/* 본문 카드 */}
      <div className={s.card}>
        {loading ? (
          <div className={s.loading}>불러오는 중…</div>
        ) : errorState ? (
          <div className={s.error}>
            조회 중 오류가 발생했습니다.
            <div className={s.errorSub}>{(error as any)?.message ?? ""}</div>
          </div>
        ) : list.length === 0 ? (
          <div className={s.empty}>등록된 과목이 없습니다.</div>
        ) : (
          <>
            {/* 💻 데스크톱: 테이블 */}
            <div className={s.desktopOnly}>
              <table className={s.table}>
                <thead>
                  <tr>
                    <th className={s.th} style={{ width: "31%" }}>
                      과목명
                    </th>
                    <th className={s.th} style={{ width: "10%" }}>
                      학점
                    </th>
                    {isMajor && (
                      <th className={s.th} style={{ width: "12%" }}>
                        설계학점
                      </th>
                    )}
                    <th
                      className={s.th}
                      style={{ width: isMajor ? "13%" : "23%" }}
                    >
                      성적
                    </th>
                    <th className={s.th} style={{ width: "14%" }}>
                      학기
                    </th>
                    <th className={s.th} style={{ width: "20%" }}>
                      작업
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((c, idx) => (
                    <tr
                      key={c.id ?? `${c.name}-${idx}`}
                      className={idx % 2 ? s.rowEven : undefined}
                    >
                      <td className={s.td}>
                        {c.name}
                        {c.isEnglish && <span className={s.badgeEng}>ENG</span>}
                      </td>
                      <td className={s.td}>{c.credit}</td>
                      {isMajor && (
                        <td className={s.td}>{c.designedCredit ?? "-"}</td>
                      )}
                      <td className={s.td}>{c.grade || "-"}</td>
                      <td className={s.td}>
                        {formatSemester(c.academicYear, c.term)}
                      </td>

                      <td className={s.tdActions}>
                        <div className={s.btnGroup}>
                          <button
                            className={s.btnGhost}
                            onClick={() => setEditing(c)}
                          >
                            수정
                          </button>
                          <button
                            className={s.btnDanger}
                            onClick={() => handleDelete(c)}
                            disabled={!isGuest && deleteMutation.isPending}
                            title="삭제"
                          >
                            {!isGuest && deleteMutation.isPending
                              ? "삭제 중…"
                              : "삭제"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 📱 모바일: 카드 리스트 */}
            <div className={s.mobileOnly}>
              <div className={s.mobileList}>
                {list.map((c) => (
                  <div key={c.id} className={s.mobileCard}>
                    <div className={s.mobileCardHeader}>
                      <div className={s.mobileCourseTitle}>
                        <span className={s.mobileCourseName}>{c.name}</span>
                        {c.isEnglish && <span className={s.badgeEng}>ENG</span>}
                      </div>
                      <span className={s.mobileSemester}>
                        {formatSemester(c.academicYear, c.term)}
                      </span>
                    </div>

                    <div className={s.mobileCardBody}>
                      <div className={s.mobileRow}>
                        <span className={s.mobileLabel}>학점</span>
                        <span className={s.mobileValue}>{c.credit}</span>
                      </div>

                      {isMajor && (
                        <div className={s.mobileRow}>
                          <span className={s.mobileLabel}>설계학점</span>
                          <span className={s.mobileValue}>
                            {c.designedCredit ?? "-"}
                          </span>
                        </div>
                      )}

                      <div className={s.mobileRow}>
                        <span className={s.mobileLabel}>성적</span>
                        <span className={s.mobileValue}>{c.grade || "-"}</span>
                      </div>
                    </div>

                    <div className={s.mobileCardFooter}>
                      <button className={s.btnGhost} onClick={() => setEditing(c)}>
                        수정
                      </button>
                      <button
                        className={s.btnDanger}
                        onClick={() => handleDelete(c)}
                        disabled={!isGuest && deleteMutation.isPending}
                        title="삭제"
                      >
                        {!isGuest && deleteMutation.isPending ? "삭제 중…" : "삭제"}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>

      {/* 수정 모달 (게스트/로그인 공용) */}
      <EditCourseModal
        open={!!editing}
        course={editing}
        sid={sid}
        onClose={closeEdit}
        onSaved={handleEdited}
      />
    </div>
  );
}
