// src/pages/CurriculumPage/modal/AiResultModal.tsx
import type { MouseEvent } from "react";
import { createPortal } from "react-dom";
import s from "./AiCaptureModal.module.css";
import { getCategoryLabel } from "../curriculumTypes";

export interface AiCourseResult {
  id: string;              
  name: string;
  credit: number;
  designedCredit: number | null;
  category: string;
  grade: string;
  isEnglish: boolean;      // Java의 isEnglish
  academicYear: number;
  term: string;
}

interface AiResultModalProps {
  open: boolean;
  courses: AiCourseResult[];
  checked: boolean[];
  isSaving: boolean;

  onToggleChecked: (index: number) => void;
  onSaveSelected: () => void;
  onSaveAll: () => void;
  onClose: () => void;
}

export function AiResultModal({
  open,
  courses,
  checked,
  isSaving,
  onToggleChecked,
  onSaveSelected,
  onSaveAll,
  onClose,
}: AiResultModalProps) {
  if (!open) return null;

  const stop = (e: MouseEvent<HTMLDivElement>) => {
    e.stopPropagation();
  };

  const anyChecked = checked.some(Boolean);

  return createPortal(
    <div className={s.aiResultOverlay} onClick={onClose}>
      <div className={s.aiResultModal} onClick={stop}>
        <button
          type="button"
          className={s.aiResultClose}
          onClick={onClose}
          aria-label="닫기"
          disabled={isSaving}
        >
          ✕
        </button>

        <div className={s.aiResultHeader}>
          <h3>AI 인식 결과</h3>
          <span className={s.aiResultCount}>총 {courses.length}개 과목 인식됨</span>
        </div>
        <p className={s.aiResultHint}>
          저장하기 전에 인식 결과를 한 번 확인해 주세요. AI는 실수를 할 수 있습니다. 중요한 정보는 재차 확인하세요.
        </p>

        <div className={s.aiResultTableWrap}>
          <table className={s.aiResultTable}>
            <thead>
              <tr>
                <th>선택</th>
                <th>과목명</th>
                <th>학점</th>
                <th>설계</th>
                <th>성적</th>
                <th>카테고리</th>
                <th>영어</th>
                <th>연도</th>
                <th>학기</th>
              </tr>
            </thead>
            <tbody>
              {Array.isArray(courses) &&
                courses.map((c, i) => (
                  <tr key={c.id}> 
                    <td>
                      <input
                        type="checkbox"
                        checked={checked[i] ?? false}
                        onChange={() => onToggleChecked(i)}
                        disabled={isSaving}
                      />
                    </td>
                    <td>{c.name}</td>
                    <td>{c.credit}</td>
                    <td>{c.designedCredit ?? "-"}</td>
                    <td>{c.grade}</td>
                    <td>{getCategoryLabel(c.category)}</td>
                    <td>{c.isEnglish ? "Y" : "N"}</td>
                    <td>{c.academicYear}</td>
                    <td>{c.term}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>

        <div className={s.aiResultFooter}>
          <button
            type="button"
            className={s.aiSecondaryBtn}
            onClick={onSaveSelected}
            disabled={!anyChecked || isSaving}
          >
            선택 항목 저장
          </button>
          <button
            type="button"
            className={s.aiPrimaryBtn}
            onClick={onSaveAll}
            disabled={!courses.length || isSaving}
          >
            전체 저장
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
