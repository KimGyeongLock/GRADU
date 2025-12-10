// src/pages/CurriculumPage/modal/CourseOverwriteModal.tsx
import { createPortal } from "react-dom";
import type { ReactNode, MouseEvent } from "react";

type Props = {
  open: boolean;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  onConfirm: () => void;
  confirmDisabled?: boolean;
  cancelLabel?: string;
  onCancel?: () => void;
};

export function CourseOverwriteModal({
  open,
  title,
  description,
  confirmLabel,
  onConfirm,
  confirmDisabled,
  cancelLabel,
  onCancel,
}: Props) {
  if (!open) return null;

  const handleBackdropClick = () => {
    if (confirmDisabled) return;
    // 취소 버튼이 있는 경우에는 취소와 동일하게 동작
    if (onCancel) onCancel();
  };

  const stop = (e: MouseEvent<HTMLDivElement>) => {
    e.stopPropagation();
  };

  return createPortal(
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        // 🔥 웬만한 것보다 무조건 위에 오도록 크게 설정
        zIndex: 2147483647,
      }}
      onClick={handleBackdropClick}
    >
      <div
        onClick={stop}
        style={{
          background: "white",
          borderRadius: 16,
          padding: "20px 24px",
          maxWidth: 420,
          width: "90%",
          boxShadow: "0 18px 45px rgba(15,23,42,0.35)",
          // 카드도 한 단계 더 위
          zIndex: 2147483647,
        }}
      >
        <h3
          style={{
            fontSize: 18,
            fontWeight: 600,
            marginBottom: 8,
          }}
        >
          {title}
        </h3>

        <div
          style={{
            fontSize: 14,
            color: "#4b5563",
            marginBottom: 16,
            lineHeight: 1.6,
          }}
        >
          {description}
        </div>

        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: 8,
            marginTop: 8,
          }}
        >
          {cancelLabel && onCancel && (
            <button
              type="button"
              className="cm-btn cm-btn-ghost"
              onClick={onCancel}
              disabled={!!confirmDisabled}
            >
              {cancelLabel}
            </button>
          )}
          <button
            type="button"
            className="cm-btn cm-btn-primary"
            onClick={onConfirm}
            disabled={!!confirmDisabled}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
