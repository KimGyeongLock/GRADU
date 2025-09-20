import { useEffect, type ReactNode, type CSSProperties } from "react";
import { createPortal } from "react-dom";

type ModalProps = {
  open: boolean;
  onClose?: () => void;
  title?: string;
  children?: ReactNode;
  footer?: ReactNode;
  closeOnBackdrop?: boolean;
};

const wrapStyle: CSSProperties = {
  position: "fixed",
  inset: 0,
  zIndex: 1000,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

const overlayStyle: CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(0,0,0,.45)",
  backdropFilter: "blur(1px)",
};

const dialogStyle: CSSProperties = {
  position: "relative",
  background: "#fff",
  borderRadius: 16,
  width: "min(640px, 92vw)",
  maxHeight: "88vh",
  overflow: "auto",
  boxShadow: "0 24px 64px rgba(0,0,0,.28)",
};

export default function Modal({
  open,
  onClose,
  title,
  children,
  footer,
  closeOnBackdrop = true,
}: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose?.();
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  if (!open) return null;

  return createPortal(
    <div style={wrapStyle}>
      <div style={overlayStyle} onClick={() => closeOnBackdrop && onClose?.()} />
      <div
        style={dialogStyle}
        onClick={(e) => e.stopPropagation()}
        className="relative bg-white text-gray-900 [color-scheme:light] modal-reset ring-1 ring-black/10"
      >
        {/* Header */}
        {title && (
          <div
            className="px-6 py-4 border-b bg-gradient-to-r from-blue-50 to-white rounded-t-2xl"
            style={{ padding: "16px 24px" }}  // padding 보장(fallback)
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">{title}</h2>
              <button
                aria-label="닫기"
                onClick={onClose}
                style={{ position: "absolute", top: 12, right: 12 }}
                className="px-2 text-xl leading-none bg-transparent text-gray-700 hover:text-black"
              >
                ×
              </button>
            </div>
          </div>
        )}

        {/* Body */}
        <div className="p-6" style={{ padding: 24 }}>
          {children}
        </div>

        {/* Footer */}
        {footer && (
          <div
            className="px-6 py-4 border-t bg-white sticky bottom-0"
            style={{ padding: "16px 24px" }}
          >
            <div className="flex justify-end gap-2">{footer}</div>
          </div>
        )}
      </div>
    </div>,
    document.body
  );
}
