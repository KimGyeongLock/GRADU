// src/pages/CurriculumPage.tsx
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { axiosInstance, getStudentId } from "../lib/axios";
import AddCourseModal from "../components/AddCourseModal";
import s from "./CurriculumTable.module.css";

// 숫자 포맷: 정수면 정수, 아니면 소수1자리
const fmtCred = (n?: number | null) => {
  if (n == null || Number.isNaN(n)) return "-";
  const v = Math.round(n * 10) / 10; // 부동소수 오차 보정
  return Number.isInteger(v) ? String(v) : v.toFixed(1);
};

// ---- 컨페티 & 빵빠레 (지연 로드 + WebAudio) ----
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

// 서버 Summary 응답 타입
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

export default function CurriculumPage() {
  const sid = getStudentId() || "";
  const qc = useQueryClient();
  const nav = useNavigate();

  const { data: summary, isLoading, isError } = useQuery<SummaryDto>({
    queryKey: ["summary", sid],
    enabled: !!sid,
    queryFn: async () => {
      const url = `/api/v1/students/${encodeURIComponent(sid)}/summary`;
      const { data } = await axiosInstance.get<SummaryDto>(url);
      return data;
    },
    refetchOnWindowFocus: false,
  });

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
  const sPassCls = s.statusPass,
    sFailCls = s.statusFail;
  const statusClass = (ok: boolean) => (ok ? sPassCls : sFailCls);

  const [open, setOpen] = useState(false);
  const handleSaved = () => {
    qc.invalidateQueries({ queryKey: ["summary", sid] });
  };

  const hasCelebratedRef = useRef(false);
  const [showBanner, setShowBanner] = useState(false);

  // finalPass가 true로 "전환"될 때 1회 축하
  useEffect(() => {
    if (!summary) return;
    if (summary.finalPass && !hasCelebratedRef.current) {
      hasCelebratedRef.current = true;
      fireConfetti(1800);                            // 🎉 컨페티만 실행
      setShowBanner(true);
      const t = setTimeout(() => setShowBanner(false), 3000);
      return () => clearTimeout(t);
    }
    if (!summary.finalPass) {
      hasCelebratedRef.current = false;
    }
  }, [summary?.finalPass]);

  if (!sid) return <div className="text-center py-14">로그인 정보를 찾을 수 없습니다.</div>;
  if (isLoading) return <div className="text-center py-14">불러오는 중…</div>;
  if (isError || !summary) return <div className="text-center py-14">조회 실패</div>;

  // P/F 기준 설명용 (정책상 최소 39 적용 시)
  const pfLimitNote = Math.max(39, summary.pfLimit);

  return (
    <div className="relative">
      {/* 축하 배너 */}
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

      <div className={s.card}>
        <div className={s.tableWrap}>
          <table className={s.table}>
            <thead>
              <tr>
                <th className={s.th} style={{ width: "32%" }}>
                  카테고리
                </th>
                <th className={s.th} style={{ width: "20%" }}>
                  졸업기준(설계)
                </th>
                <th className={s.th} style={{ width: "16%" }}>
                  취득 학점
                </th>
                <th className={s.th} style={{ width: "16%" }}>
                  상태
                </th>
                <th className={s.th} style={{ width: "16%" }}>
                  상세
                </th>
              </tr>
            </thead>
            <tbody>
              {summary.rows.map((row, i) => (
                <tr key={row.key} className={i % 2 ? s.rowEven : undefined}>
                  <td className={s.td}>{row.name}</td>
                  <td className={s.td} style={{ whiteSpace: "nowrap" }}>
                    {row.grad}
                  </td>
                  <td className={s.td}>
                    {row.key === "MAJOR"
                      ? `${fmtCred(row.earned)}(${row.designedEarned ?? 0})`
                      : fmtCred(row.earned)}
                  </td>
                  <td className={`${s.td} ${row.status === "PASS" ? sPassCls : sFailCls}`}>
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
              <tr className={s.summarySep}>
                <td colSpan={5} />
              </tr>

              <tr>
                <td className={s.tdLabel}>P/F과목 총이수학점</td>
                <td className={s.tdNote}>
                  총 취득학점의 30% 기준: {fmtCred(pfLimitNote)}학점 이하
                </td>
                <td className={s.tdValue}>{fmtCred(summary.pfCredits)}</td>
                <td className={`${s.td} ${statusClass(summary.pfPass)}`}>
                  {summary.pfPass ? "합격" : "불합격"}
                </td>
                <td className={s.td} />
              </tr>

              <tr>
                <td className={s.tdLabel}>총 취득학점</td>
                <td className={s.tdNote}>130학점 이상</td>
                <td className={s.tdValue}>{fmtCred(summary.totalCredits)}</td>
                <td className={`${s.td} ${statusClass(summary.totalPass)}`}>
                  {summary.totalPass ? "합격" : "불합격"}
                </td>
                <td className={s.td} />
              </tr>

              <tr>
                <td className={s.tdLabel}>평점 평균</td>
                <td className={s.tdNote}>2.0 이상</td>
                <td className={s.tdValue}>{(summary.gpa ?? 0).toFixed(2)}</td>
                <td className={`${s.td} ${(summary.gpa ?? 0) >= 2.0 ? sPassCls : sFailCls}`}>
                  {(summary.gpa ?? 0) >= 2.0 ? "합격" : "불합격"}
                </td>
                <td className={s.td} />
              </tr>

              <tr>
                <td className={s.tdLabel}>영어강의 과목이수</td>
                <td className={s.tdNote}>
                  전공:{fmtCred(summary.engMajorCredits)} / 교양:
                  {fmtCred(summary.engLiberalCredits)}
                </td>
                <td className={s.tdValue}></td>
                <td className={`${s.td} ${statusClass(summary.englishPass)}`}>
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
                <td className={s.tdNote}>
                  {/* <label style={{ display: "inline-flex", alignItems: "center", gap: 8 }}>
                    <input
                      type="checkbox"
                      checked={soundOn}
                      onChange={(e) => setSoundOn(e.target.checked)}
                      aria-label="효과음 켜기"
                    />
                    🔊 효과음
                  </label> */}
                </td>
                <td className={s.tdValue}></td>
                <td className={`${s.td} ${statusClass(summary.finalPass)}`}>
                  {summary.finalPass ? "졸업가능" : "졸업불가능"}
                </td>
                <td className={s.td}>
                  {/* <button
                    className={s.viewBtn}
                    onClick={() => {
                      fireConfetti(1500);
                      if (soundOn) playFanfare().catch(() => {});
                      setShowBanner(true);
                      setTimeout(() => setShowBanner(false), 3000);
                    }}
                    title="축하 연출 다시 보기"
                    aria-label="축하 연출 다시 보기"
                  >
                    축하 다시보기
                  </button> */}
                </td>
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
