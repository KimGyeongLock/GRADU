// src/components/AddCourseModal.tsx
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { axiosInstance } from "../lib/axios";
import Modal from "./Modal";
import "./AddCourseModal.css";

type Props = {
  open: boolean;
  sid: string;
  onClose: () => void;
  onSaved: () => void;
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

const ORDER = Object.keys(KOR_LABELS) as Array<keyof typeof KOR_LABELS>;

/** 화면 입력 상태: 숫자 필드도 문자열로 관리 */
type CourseInput = {
  name: string;
  credit: string;          // ← string
  designedCredit: string;  // ← string
  category: keyof typeof KOR_LABELS;
  grade: string;
};

/** 서버 전송 payload: 숫자로 변환 */
type CourseRequest = {
  name: string;
  credit: number;
  designedCredit: number;
  category: keyof typeof KOR_LABELS;
  grade: string;
};

export default function AddCourseModal({ open, sid, onClose, onSaved }: Props) {
  const [form, setForm] = useState<CourseInput>({
    name: "",
    credit: "",           // ← 빈 문자열 (0 안보임)
    designedCredit: "",   // ← 빈 문자열
    category: "FAITH_WORLDVIEW",
    grade: "",
  });
  const [errMsg, setErrMsg] = useState("");

  /** 입력 변경 */
  const onChange = <K extends keyof CourseInput>(k: K, v: string) => {
    if (k === "credit" || k === "designedCredit") {
      v = v.replace(/\D/g, ""); // 숫자 이외 제거
    }
    setForm((f) => ({ ...f, [k]: v }));
  };

  const addCourse = useMutation({
    mutationFn: async (input: CourseRequest) => {
      const url = `/api/v1/students/${encodeURIComponent(sid)}/courses`;
      const { data } = await axiosInstance.post(url, input, {
        headers: { "Content-Type": "application/json" },
      });
      return data;
    },
    onSuccess: () => {
      setErrMsg("");
      setForm({
        name: "",
        credit: "",
        designedCredit: "",
        category: "FAITH_WORLDVIEW",
        grade: "",
      });
      onSaved();
      onClose();
    },
    onError: (e: any) => {
      const status = e?.response?.status;
      const text = e?.response?.data?.message || e?.message || "요청 실패";
      setErrMsg(`저장 실패 (${status ?? "-"}) : ${text}`);
      console.error("[addCourse] error", e);
    },
  });

  // ---- inline style tokens ----
  const s = {
    form: { fontSize: 14, display: "block" } as const,
    field: { marginBottom: 16 } as const,
    label: { display: "block", marginBottom: 6, color: "#111827", fontWeight: 600 } as const,
    input: {
      width: "100%",
      border: "1px solid #d1d5db",
      borderRadius: 8,
      padding: "8px 12px",
      background: "#fff",
      color: "#111",
      boxSizing: "border-box",
    } as const,
    row: { gap: 16 } as const,
    error: {
      borderRadius: 8,
      background: "#fef2f2",
      color: "#b91c1c",
      border: "1px solid #fecaca",
      padding: "8px 12px",
      marginBottom: 16,
    } as const,
    footerBtnBase: {
      borderRadius: 8,
      padding: "8px 16px",
      cursor: "pointer",
      fontSize: 14,
    } as const,
  };

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.name.trim()) {
      setErrMsg("과목명을 입력하세요.");
      return;
    }
    setErrMsg("");

    // 문자열 → 숫자 변환 (빈 문자열이면 0)
    const payload: CourseRequest = {
      name: form.name.trim(),
      credit: form.credit === "" ? 0 : Number(form.credit),
      designedCredit: form.designedCredit === "" ? 0 : Number(form.designedCredit),
      category: form.category,
      grade: form.grade.trim(),
    };

    await addCourse.mutateAsync(payload);
  }

  return (
    <Modal
      open={open}
      onClose={() => !addCourse.isPending && onClose()}
      title="과목 추가"
      footer={
        <>
          <button
            type="button"
            className="addcourse-btn addcourse-btn--ghost"
            style={s.footerBtnBase}
            disabled={addCourse.isPending}
            onClick={onClose}
          >
            취소
          </button>
          <button
            form="add-course-form"
            type="submit"
            className="addcourse-btn addcourse-btn--primary"
            style={s.footerBtnBase}
            disabled={addCourse.isPending || !form.name}
          >
            {addCourse.isPending ? "저장 중..." : "저장"}
          </button>
        </>
      }
    >
      <form id="add-course-form" onSubmit={onSubmit} style={s.form}>
        {errMsg && <div style={s.error}>{errMsg}</div>}

        {/* 과목명 */}
        <div style={s.field}>
          <label style={s.label}>과목명</label>
          <input
            style={s.input}
            value={form.name}
            onChange={(e) => onChange("name", e.target.value)}
            placeholder="예: 객체지향 설계패턴"
          />
        </div>

        {/* 학점/설계학점 */}
        <div className="addcourse-row" style={s.row}>
          <div>
            <label style={s.label}>학점</label>
            <input
              type="text"                 // ← text로 두고
              inputMode="numeric"         // 모바일 숫자 키패드
              pattern="[0-9]*"            // 숫자만
              style={s.input}
              value={form.credit}
              onChange={(e) => onChange("credit", e.target.value)}
              placeholder="0"
            />
          </div>
          <div>
            <label style={s.label}>설계학점</label>
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              style={s.input}
              value={form.designedCredit}
              onChange={(e) => onChange("designedCredit", e.target.value)}
              placeholder="0"
            />
          </div>
        </div>

        {/* 카테고리 */}
        <div style={s.field}>
          <label style={s.label}>카테고리</label>
          <select
            className="addcourse-select"
            style={s.input}
            value={form.category}
            onChange={(e) => onChange("category", e.target.value)}
          >
            {ORDER.map((key) => (
              <option key={key} value={key}>
                {KOR_LABELS[key]}
              </option>
            ))}
          </select>
        </div>

        {/* 성적 */}
        <div style={s.field}>
          <label style={s.label}>
            성적 <span style={{ color: "#9ca3af", fontWeight: 400 }}>(예: A, B+, P)</span>
          </label>
          <input
            style={s.input}
            placeholder="예: A, B+, P"
            value={form.grade}
            onChange={(e) => onChange("grade", e.target.value)}
          />
        </div>
      </form>
    </Modal>
  );
}
