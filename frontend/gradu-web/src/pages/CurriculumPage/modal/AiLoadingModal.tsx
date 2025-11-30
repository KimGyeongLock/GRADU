// src/pages/CurriculumPage/modal/AiLoadingModal.tsx
import type { MouseEvent } from "react";
import { createPortal } from "react-dom";
import s from "./AiCaptureModal.module.css";

interface AiLoadingModalProps {
  open: boolean;
  onClose: () => void;      // X 버튼 클릭 시
}

export function AiLoadingModal({ open, onClose }: AiLoadingModalProps) {
  if (!open) return null;

  const stop = (e: MouseEvent<HTMLDivElement>) => {
    e.stopPropagation();
  };

  return createPortal(
    <div className={s.aiLoadingOverlay}>
      <div className={s.aiLoadingCard} onClick={stop}>
        <button
          type="button"
          className={s.aiLoadingClose}
          onClick={onClose}
          aria-label="분석 취소"
        >
          ✕
        </button>

        <h3 className={s.aiLoadingTitle}>AI가 성적표를 분석 중입니다</h3>
        <p className={s.aiLoadingSubtitle}>
          과목명 / 학점 / 성적 / 카테고리를 추출하는 중이에요…
        </p>

        {/* 가로 타원형 로딩 바 */}
        <div className={s.aiLoadingBarTrack}>
          <div className={s.aiLoadingBarFill} />
        </div>
      </div>
    </div>,
    document.body
  );
}
