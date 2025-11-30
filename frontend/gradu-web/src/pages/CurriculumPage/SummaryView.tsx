// src/pages/CurriculumPage/SummaryView.tsx
import { useNavigate } from "react-router-dom";
import type { SummaryDto } from "./curriculumTypes";
import { fmtCred, statusText } from "./curriculumTypes";
import s from "./CurriculumTable.module.css";

type Props = {
  summary: SummaryDto;
  pfLimitNote: number;
  gradEnglishPassed: boolean;
  deptExtraPassed: boolean;
  onChangeGradEnglishPassed: (v: boolean) => void;
  onChangeDeptExtraPassed: (v: boolean) => void;
  onClickSaveToggles: () => void;
  savingToggles: boolean;
};

const statusClass = (ok: boolean) => (ok ? s.statusPass : s.statusFail);

export function SummaryView({
  summary,
  pfLimitNote,
  gradEnglishPassed,
  deptExtraPassed,
  onChangeGradEnglishPassed,
  onChangeDeptExtraPassed,
  onClickSaveToggles,
  savingToggles,
}: Props) {
  const nav = useNavigate();

  return (
    <div>
      {/* 💻 데스크톱용 테이블 */}
      <div className={s.desktopOnly}>
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
                <td
                  className={`${s.td} ${
                    row.status === "PASS" ? s.statusPass : s.statusFail
                  }`}
                >
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
              <td
                className={`${s.td} ${
                  summary.pfPass ? s.statusPass : s.statusFail
                }`}
              >
                {summary.pfPass ? "합격" : "불합격"}
              </td>
              <td className={s.td} />
            </tr>

            <tr>
              <td className={s.tdLabel}>총 취득학점</td>
              <td className={s.tdNote}>130학점 이상</td>
              <td className={s.tdValue}>{fmtCred(summary.totalCredits)}</td>
              <td
                className={`${s.td} ${
                  summary.totalPass ? s.statusPass : s.statusFail
                }`}
              >
                {summary.totalPass ? "합격" : "불합격"}
              </td>
              <td className={s.td} />
            </tr>

            <tr>
              <td className={s.tdLabel}>평점 평균</td>
              <td className={s.tdNote}>2.0 이상</td>
              <td className={s.tdValue}>{(summary.gpa ?? 0).toFixed(2)}</td>
              <td
                className={`${s.td} ${
                  (summary.gpa ?? 0) >= 2.0 ? s.statusPass : s.statusFail
                }`}
              >
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
              <td
                className={`${s.td} ${
                  summary.englishPass ? s.statusPass : s.statusFail
                }`}
              >
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
                    onChange={(e) => onChangeGradEnglishPassed(e.target.checked)}
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
                  onClick={onClickSaveToggles}
                  disabled={savingToggles}
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
                    onChange={(e) => onChangeDeptExtraPassed(e.target.checked)}
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
                  onClick={onClickSaveToggles}
                  disabled={savingToggles}
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
      </div>

      {/* 📱 모바일용 카드 레이아웃 */}
      <div className={s.mobileOnly}>
        <div className={s.mobileSummaryWrap}>
          {/* 카테고리별 카드 */}
          {summary.rows.map((row) => (
            <div key={row.key} className={s.mobileCard}>
              <div className={s.mobileCardHeader}>
                <span className={s.mobileCategory}>{row.name}</span>
                <span
                  className={`${s.mobileStatusBadge} ${
                    row.status === "PASS" ? s.statusPass : s.statusFail
                  }`}
                >
                  {statusText(row.status)}
                </span>
              </div>

              <div className={s.mobileCardBody}>
                <div className={s.mobileRow}>
                  <span className={s.mobileLabel}>졸업기준</span>
                  <span className={s.mobileValue}>{row.grad}</span>
                </div>
                <div className={s.mobileRow}>
                  <span className={s.mobileLabel}>취득 학점</span>
                  <span className={s.mobileValue}>
                    {row.key === "MAJOR"
                      ? `${fmtCred(row.earned)}(${row.designedEarned ?? 0})`
                      : fmtCred(row.earned)}
                  </span>
                </div>
              </div>

              <div className={s.mobileCardFooter}>
                <button
                  className={s.mobileViewBtn}
                  onClick={() => nav(`/curriculum/${row.key.toLowerCase()}`)}
                >
                  상세 보기
                </button>
              </div>
            </div>
          ))}

          {/* P/F 과목 */}
          <div className={s.mobileCard}>
            <div className={s.mobileCardHeader}>
              <span className={s.mobileCategory}>P/F 과목</span>
              <span
                className={`${s.mobileStatusBadge} ${
                  summary.pfPass ? s.statusPass : s.statusFail
                }`}
              >
                {summary.pfPass ? "합격" : "불합격"}
              </span>
            </div>
            <div className={s.mobileCardBody}>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>기준</span>
                <span className={s.mobileValue}>
                  총 취득학점의 30% 이하 ({fmtCred(pfLimitNote)}학점)
                </span>
              </div>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>취득 P/F</span>
                <span className={s.mobileValue}>
                  {fmtCred(summary.pfCredits)}
                </span>
              </div>
            </div>
          </div>

          {/* 총 취득학점 */}
          <div className={s.mobileCard}>
            <div className={s.mobileCardHeader}>
              <span className={s.mobileCategory}>총 취득학점</span>
              <span
                className={`${s.mobileStatusBadge} ${
                  summary.totalPass ? s.statusPass : s.statusFail
                }`}
              >
                {summary.totalPass ? "합격" : "불합격"}
              </span>
            </div>
            <div className={s.mobileCardBody}>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>기준</span>
                <span className={s.mobileValue}>130학점 이상</span>
              </div>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>취득</span>
                <span className={s.mobileValue}>
                  {fmtCred(summary.totalCredits)}
                </span>
              </div>
            </div>
          </div>

          {/* 평점 평균 */}
          <div className={s.mobileCard}>
            <div className={s.mobileCardHeader}>
              <span className={s.mobileCategory}>평점 평균</span>
              <span
                className={`${s.mobileStatusBadge} ${
                  (summary.gpa ?? 0) >= 2.0 ? s.statusPass : s.statusFail
                }`}
              >
                {(summary.gpa ?? 0) >= 2.0 ? "합격" : "불합격"}
              </span>
            </div>
            <div className={s.mobileCardBody}>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>기준</span>
                <span className={s.mobileValue}>2.0 이상</span>
              </div>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>평점</span>
                <span className={s.mobileValue}>
                  {(summary.gpa ?? 0).toFixed(2)}
                </span>
              </div>
            </div>
          </div>

          {/* 영어강의 과목이수 */}
          <div className={s.mobileCard}>
            <div className={s.mobileCardHeader}>
              <span className={s.mobileCategory}>영어강의 과목이수</span>
              <span
                className={`${s.mobileStatusBadge} ${
                  summary.englishPass ? s.statusPass : s.statusFail
                }`}
              >
                {summary.englishPass ? "합격" : "불합격"}
              </span>
            </div>
            <div className={s.mobileCardBody}>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>전공</span>
                <span className={s.mobileValue}>
                  {fmtCred(summary.engMajorCredits)}
                </span>
              </div>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>교양</span>
                <span className={s.mobileValue}>
                  {fmtCred(summary.engLiberalCredits)}
                </span>
              </div>
            </div>
          </div>

          {/* 졸업영어시험 */}
          <div className={s.mobileCard}>
            <div className={s.mobileCardHeader}>
              <span className={s.mobileCategory}>졸업영어시험</span>
              <span
                className={`${s.mobileStatusBadge} ${statusClass(
                  gradEnglishPassed
                )}`}
              >
                {gradEnglishPassed ? "합격" : "불합격"}
              </span>
            </div>
            <div className={s.mobileCardBody}>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>상태</span>
                <span className={s.mobileValue}>
                  <label className={s.toggle}>
                    <input
                      type="checkbox"
                      checked={gradEnglishPassed}
                      onChange={(e) =>
                        onChangeGradEnglishPassed(e.target.checked)
                      }
                    />
                    <span />
                  </label>
                </span>
              </div>
            </div>
            <div className={s.mobileCardFooter}>
              <button
                className={s.saveBtn}
                onClick={onClickSaveToggles}
                disabled={savingToggles}
              >
                저장
              </button>
            </div>
          </div>

          {/* 학부추가졸업요건 */}
          <div className={s.mobileCard}>
            <div className={s.mobileCardHeader}>
              <span className={s.mobileCategory}>학부추가졸업요건</span>
              <span
                className={`${s.mobileStatusBadge} ${statusClass(
                  deptExtraPassed
                )}`}
              >
                {deptExtraPassed ? "합격" : "불합격"}
              </span>
            </div>
            <div className={s.mobileCardBody}>
              <div className={s.mobileRow}>
                <span className={s.mobileLabel}>상태</span>
                <span className={s.mobileValue}>
                  <label className={s.toggle}>
                    <input
                      type="checkbox"
                      checked={deptExtraPassed}
                      onChange={(e) =>
                        onChangeDeptExtraPassed(e.target.checked)
                      }
                    />
                    <span />
                  </label>
                </span>
              </div>
            </div>
            <div className={s.mobileCardFooter}>
              <button
                className={s.saveBtn}
                onClick={onClickSaveToggles}
                disabled={savingToggles}
              >
                저장
              </button>
            </div>
          </div>

          {/* 최종 졸업판정 */}
          <div className={s.mobileCard}>
            <div className={s.mobileCardHeader}>
              <span className={s.mobileCategory}>
                공학인증 최종 졸업판정
              </span>
              <span
                className={`${s.mobileStatusBadge} ${statusClass(
                  summary.finalPass
                )}`}
              >
                {summary.finalPass ? "졸업가능" : "졸업불가능"}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
