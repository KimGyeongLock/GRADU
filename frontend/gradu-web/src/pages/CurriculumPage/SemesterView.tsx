// src/pages/CurriculumPage/SemesterView.tsx
import { useState } from "react";
import type { CourseLite, Term } from "./curriculumTypes";
import {
  CATEGORY_LABELS,
  fmtCred,
  formatSemester,
} from "./curriculumTypes";
import s from "./CurriculumTable.module.css";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../../lib/axios";
import EditCourseModal from "./modal/EditCourseModal";

type Group = { key: string; year: number; term: Term; items: CourseLite[] };

type Props = {
  mergedGroups: Group[];
  isLoadingSem: boolean;
  isErrorSem: boolean;
  view: "summary" | "semester";
  onOpenAddFor: (year?: number, term?: Term) => void;
  onCreateNextSemester: () => void;
};

export function SemesterView({
  mergedGroups,
  isLoadingSem,
  isErrorSem,
  view,
  onOpenAddFor,
  onCreateNextSemester,
}: Props) {
  const sid = getStudentId() || "";
  const qc = useQueryClient();

  // 수정 모달용 상태
  const [editing, setEditing] = useState<CourseLite | null>(null);
  const closeEdit = () => setEditing(null);
  const handleEdited = () => {
    // ✅ 학기/요약 데이터 다시 가져오기 (queryKey는 실제 사용 중인 키로 변경)
    qc.invalidateQueries({
      queryKey: ["courses-semester", sid],
    });
    closeEdit();
  };

  // 삭제 mutation
  const deleteMutation = useMutation({
    mutationFn: async (courseId: number) => {
      const url = `/api/v1/students/${encodeURIComponent(
        sid
      )}/courses/${courseId}`;
      await axiosInstance.delete(url);
    },
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["courses-semester", sid],
      });
    },
  });

  const handleDelete = (course: CourseLite) => {
    if (window.confirm(`"${course.name}" 과목을 삭제할까요?`)) {
      if (!course.id) return;
      deleteMutation.mutate(course.id);
    }
  };

  return (
    <>
      <div className={s.semesterWrap}>
        {isLoadingSem ? (
          <div className="text-center py-10">학기별 데이터를 불러오는 중…</div>
        ) : isErrorSem ? (
          <div className="text-center py-10">
            학기별 데이터를 불러오지 못했습니다.
          </div>
        ) : mergedGroups.length === 0 ? (
          <div className="text-center py-10">등록된 과목이 없습니다.</div>
        ) : (
          <>
            {mergedGroups.map((g) => (
              <div key={g.key} className={s.semesterCard}>
                <div className={s.semesterHeader}>
                  {formatSemester(g.year, g.term)}
                  <button
                    className={s.semesterAddSmall}
                    onClick={() => onOpenAddFor(g.year, g.term)}
                    title="이 학기에 과목 추가"
                  >
                    과목 추가
                  </button>
                </div>

                {/* 💻 데스크탑용 테이블 */}
                <div className={s.desktopOnly}>
                  <table className={s.table}>
                    <thead>
                      <tr>
                        <th className={s.th} style={{ width: "40%" }}>
                          과목명
                        </th>
                        <th className={s.th} style={{ width: "20%" }}>
                          카테고리
                        </th>
                        <th className={s.th} style={{ width: "12%" }}>
                          학점
                        </th>
                        <th className={s.th} style={{ width: "14%" }}>
                          성적
                        </th>
                        <th className={s.th} style={{ width: "20%" }}>
                          작업
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {g.items.length ? (
                        g.items.map((c, idx) => (
                          <tr
                            key={c.id ?? `${c.name}-${idx}`}
                            className={idx % 2 ? s.rowEven : undefined}
                          >
                            <td className={s.td}>
                              {c.name}{" "}
                              {c.isEnglish ? (
                                <span className={s.badgeEng}>ENG</span>
                              ) : null}
                            </td>
                            <td className={s.td}>
                              {CATEGORY_LABELS[c.category] ?? c.category}
                            </td>
                            <td className={s.td}>{fmtCred(c.credit)}</td>
                            <td className={s.td}>{c.grade ?? "-"}</td>
                            <td className={s.td}>
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
                                  disabled={deleteMutation.isPending}
                                  title="삭제"
                                >
                                  {deleteMutation.isPending ? "삭제 중…" : "삭제"}
                                </button>
                              </div>
                            </td>

                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td
                            className={s.td}
                            colSpan={5}
                            style={{ color: "#6b7280" }}
                          >
                            이 학기에 아직 과목이 없습니다.&nbsp;
                            <button
                              className={s.viewBtn}
                              onClick={() => onOpenAddFor(g.year, g.term)}
                            >
                              과목 추가
                            </button>
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* 📱 모바일 카드 */}
                <div className={s.mobileOnly}>
                  {g.items.length ? (
                    <div className={s.mobileCourseList}>
                      {g.items.map((c) => (
                        <div key={c.id} className={s.mobileCourseCard}>
                          <div className={s.mobileCourseTitleRow}>
                            <span className={s.mobileCourseName}>{c.name}</span>
                            {c.isEnglish && <span className={s.badgeEng}>ENG</span>}
                          </div>

                          <div className={s.mobileCourseMeta}>
                            <span>{CATEGORY_LABELS[c.category] ?? c.category}</span>
                            <span>{fmtCred(c.credit)}학점</span>
                            <span>{c.grade ?? "-"}</span>
                          </div>

                          <div className={s.mobileCardFooter}>
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
                                disabled={deleteMutation.isPending}
                                title="삭제"
                              >
                                {deleteMutation.isPending ? "삭제 중…" : "삭제"}
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className={s.mobileEmpty}>
                      이 학기에 아직 과목이 없습니다.&nbsp;
                      <button
                        className={s.mobileViewBtn}
                        onClick={() => onOpenAddFor(g.year, g.term)}
                      >
                        과목 추가
                      </button>
                    </div>
                  )}
                </div>

              </div>
            ))}

            {view === "semester" && (
              <div className={s.semesterAddBar}>
                <button
                  type="button"
                  className={s.semesterAddBtn}
                  onClick={onCreateNextSemester}
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

      <EditCourseModal
        open={!!editing}
        course={editing as any}
        sid={sid}
        onClose={closeEdit}
        onSaved={handleEdited}
      />
    </>
  );
}
