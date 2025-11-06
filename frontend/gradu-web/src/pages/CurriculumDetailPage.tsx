import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../lib/axios";
import EditCourseModal from "../components/EditCourseModal";
import s from "./CurriculumDetail.module.css";

export type CourseDto = {
  id: number;
  name: string;
  category: string;
  credit: number;
  designedCredit: number | null;
  grade: string | null;
  isEnglish: boolean;
  academicYear: number;             // ← 서버에서 내려오는 연도(예: 2025)
  term: "1" | "2" | "sum" | "win";  // ← 1, 2, sum, win
  // displaySemester?: string;       // (서버가 제공 시 우선 사용 가능)
};

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

/** ex) 2025 + '1'  -> '25-1'
 *      2025 + 'sum'-> '25-summer'
 *      2025 + 'win'-> '25-winter'
 */
function formatSemester(year?: number, term?: CourseDto["term"]) {
  if (!year || !term) return "-";
  const yy = String(year).slice(-2);
  const t = term === "1" || term === "2" ? term : term === "sum" ? "summer" : "winter";
  return `${yy}-${t}`;
}

export default function CurriculumDetailPage() {
  const { category = "" } = useParams();
  const sid = getStudentId() || "";
  const nav = useNavigate();
  const qc = useQueryClient();

  const categoryEnum = useMemo(
    () => category.toUpperCase().replace(/-/g, "_"),
    [category]
  );
  const isValid = ALLOWED.has(categoryEnum);
  const label = isValid ? KOR_LABELS[categoryEnum] : categoryEnum;
  const isMajor = categoryEnum === "MAJOR";

  const { data = [], isLoading, isError, error } = useQuery<CourseDto[]>({
    queryKey: ["courses-by-category", sid, categoryEnum],
    enabled: !!sid && isValid,
    queryFn: async () => {
      const url = `/api/v1/students/${encodeURIComponent(
        sid
      )}/courses/categories/${encodeURIComponent(categoryEnum)}`;
      const { data } = await axiosInstance.get<CourseDto[]>(url);
      return data ?? [];
    },
  });

  // 삭제
  const deleteMutation = useMutation({
    mutationFn: async (courseId: number) => {
      const url = `/api/v1/students/${encodeURIComponent(sid)}/courses/${courseId}`;
      await axiosInstance.delete(url);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["courses-by-category", sid, categoryEnum] });
    },
  });

  // 수정 모달 상태
  const [editing, setEditing] = useState<CourseDto | null>(null);
  const closeEdit = () => setEditing(null);
  const handleEdited = () => {
    qc.invalidateQueries({ queryKey: ["courses-by-category", sid, categoryEnum] });
    closeEdit();
  };

  if (!sid) return <div className={s.centerNotice}>로그인 정보를 찾을 수 없습니다.</div>;
  if (!isValid) {
    return (
      <div className={s.centerNotice}>
        잘못된 카테고리입니다: <b>{category}</b>
      </div>
    );
  }

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
        {isLoading ? (
          <div className={s.loading}>불러오는 중…</div>
        ) : isError ? (
          <div className={s.error}>
            조회 중 오류가 발생했습니다.
            <div className={s.errorSub}>{(error as any)?.message ?? ""}</div>
          </div>
        ) : data.length === 0 ? (
          <div className={s.empty}>등록된 과목이 없습니다.</div>
        ) : (
          <table className={s.table}>
            <thead>
              <tr>
                <th className={s.th} style={{ width: "31%" }}>과목명</th>
                <th className={s.th} style={{ width: "10%" }}>학점</th>
                {isMajor && <th className={s.th} style={{ width: "12%" }}>설계학점</th>}
                <th className={s.th} style={{ width: isMajor ? "13%" : "23%" }}>성적</th>
                <th className={s.th} style={{ width: "14%" }}>학기</th> {/* ← 추가 */}
                <th className={s.th} style={{ width: "20%" }}>작업</th>
              </tr>
            </thead>
            <tbody>
              {data.map((c, idx) => (
                <tr key={c.id ?? `${c.name}-${idx}`} className={idx % 2 ? s.rowEven : undefined}>
                  <td className={s.td}>{c.name}</td>
                  <td className={s.td}>{c.credit}</td>
                  {isMajor && <td className={s.td}>{c.designedCredit ?? "-"}</td>}
                  <td className={s.td}>{c.grade || "-"}</td>
                  <td className={s.td}>
                    {formatSemester(c.academicYear, c.term)}
                  </td>

                  <td className={s.tdActions}>
                    <div className={s.btnGroup}>
                      <button className={s.btnGhost} onClick={() => setEditing(c)}>수정</button>
                      <button
                        className={s.btnDanger}
                        onClick={() => {
                          if (window.confirm(`"${c.name}" 과목을 삭제할까요?`)) {
                            deleteMutation.mutate(c.id);
                          }
                        }}
                        disabled={deleteMutation.isPending}
                        title="삭제"
                      >
                        {deleteMutation.isPending ? "삭제 중…" : "삭제"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* 수정 모달 */}
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
