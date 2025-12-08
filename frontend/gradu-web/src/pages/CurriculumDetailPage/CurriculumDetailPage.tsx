// src/pages/CurriculumDetailPage.tsx
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../../lib/axios";
import EditCourseModal from "../CurriculumPage/modal/EditCourseModal";
import s from "./CurriculumDetail.module.css";
import type { CourseDto } from "../CurriculumPage/curriculumTypes";
import { formatSemester } from "../CurriculumPage/curriculumTypes";
import { isGuestMode } from "../../lib/auth";
import {
  loadGuestCourses,
  removeGuestCourse,
} from "../CurriculumPage/guest/guestStorage";

export const KOR_LABELS: Record<string, string> = {
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
export const CATEGORY_ORDER = Object.keys(KOR_LABELS);
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
  const label = isValid ? KOR_LABELS[categoryEnum] : categoryEnum;
  const isMajor = categoryEnum === "MAJOR";

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
  if (!sid && !isGuest)
    return (
      <div className={s.centerNotice}>
        로그인 정보를 찾을 수 없습니다.
      </div>
    );

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
                        {c.isEnglish && (
                          <span className={s.badgeEng}>ENG</span>
                        )}
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
                        {c.isEnglish && (
                          <span className={s.badgeEng}>ENG</span>
                        )}
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
