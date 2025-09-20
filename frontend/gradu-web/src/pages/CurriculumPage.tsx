import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../lib/axios";
import AddCourseModal from "../components/AddCourseModal";
import s from "./CurriculumTable.module.css";

type CurriculumItem = {
  category: string;
  earnedCredits: number;
  status: "PASS" | "FAIL" | string;
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
  MAJOR: "60(12)",
};

export default function CurriculumPage() {
  const sid = getStudentId() || "";
  const qc = useQueryClient();
  const nav = useNavigate();

  const { data = [], isLoading, isError } = useQuery<CurriculumItem[]>({
    queryKey: ["curriculum", sid],
    enabled: !!sid,
    queryFn: async () => {
      const url = `/api/v1/students/${encodeURIComponent(sid)}/curriculum`;
      const { data } = await axiosInstance.get<CurriculumItem[]>(url);
      return data ?? [];
    },
  });

  const rows = useMemo(() => {
    const byCat = new Map<string, CurriculumItem>();
    data.forEach((it) => byCat.set(it.category, it));
    return ORDER.map((k) => {
      const item = byCat.get(k);
      return {
        key: k,
        name: KOR_LABELS[k],
        grad: GRAD_REQS[k],
        earned: item?.earnedCredits ?? 0,
        status: item?.status ?? "",
      };
    });
  }, [data]);

  const [open, setOpen] = useState(false);
  const handleSaved = () => qc.invalidateQueries({ queryKey: ["curriculum", sid] });

  if (!sid) return <div className="text-center py-14">로그인 정보를 찾을 수 없습니다.</div>;
  if (isLoading) return <div className="text-center py-14">불러오는 중…</div>;
  if (isError) return <div className="text-center py-14">조회 실패</div>;

  const statusText = (s: string) => (s === "PASS" ? "합격" : s === "FAIL" ? "불합격" : s || "-");
  const statusClass = (s: string) =>
    s === "PASS" ? sPass : s === "FAIL" ? sFail : sNeutral;
  const sPass = s.statusPass, sFail = s.statusFail, sNeutral = s.statusNeutral;

  return (
    <div className="relative">
      {/* 카드 */}
      <div className={s.card}>
        {/* 표 */}
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
                  <td className={`${s.td}`} style={{ whiteSpace: "nowrap" }}>{row.grad}</td>
                  <td className={s.td}>{row.earned}</td>
                  <td className={`${s.td} ${statusClass(row.status)}`}>
                    {statusText(row.status)}
                  </td>
                  <td className={s.td}>
                    <button
                      onClick={() => nav(`/curriculum/${row.key.toLowerCase()}`)}
                      className={s.viewBtn}
                      title="해당 구분 과목 상세 보기"
                    >
                      보기
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* 표 아래 오른쪽 + 버튼 */}
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
