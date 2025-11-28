// src/pages/CurriculumPage/SemesterView.tsx
import { useNavigate } from "react-router-dom";
import type { CourseLite, Term } from "./curriculumTypes";
import { CATEGORY_LABELS, TERM_ORDER, fmtCred } from "./curriculumTypes";
import s from "./CurriculumTable.module.css";

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
  const nav = useNavigate();

  return (
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
                {`${String(g.year).slice(-2)}-${
                  ["1", "summer", "2", "winter"][TERM_ORDER[g.term]]
                }`}
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
                      <th className={s.th} style={{ width: "14%" }}>
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
                            <button
                              className={s.viewBtn}
                              onClick={() =>
                                nav(
                                  `/curriculum/${c.category.toLowerCase()}`
                                )
                              }
                              title="카테고리 상세로 이동"
                            >
                              보기
                            </button>
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
                          {c.isEnglish && (
                            <span className={s.badgeEng}>ENG</span>
                          )}
                        </div>
                        <div className={s.mobileCourseMeta}>
                          <span>
                            {CATEGORY_LABELS[c.category] ?? c.category}
                          </span>
                          <span>{fmtCred(c.credit)}학점</span>
                          <span>{c.grade ?? "-"}</span>
                        </div>
                        <button
                          className={s.mobileViewBtn}
                          onClick={() =>
                            nav(`/curriculum/${c.category.toLowerCase()}`)
                          }
                        >
                          카테고리 보기
                        </button>
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
  );
}
