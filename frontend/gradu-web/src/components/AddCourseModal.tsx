// src/components/AddCourseModal.tsx
import { useState, useEffect } from "react";
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

/** 화면 입력 상태: 숫자도 문자열로 관리 */
type CourseInput = {
  name: string;
  credit: string;          // "3" 처럼 문자열
  designedCredit: string;  // 전공(MAJOR)일 때만 사용
  category: keyof typeof KOR_LABELS;
  grade: string;           // "A+", "P" 등
  isEnglish: boolean;      // ✅ 영어강의 여부
};

/** 서버 전송 payload */
type CourseRequest = {
  name: string;
  credit: number;
  designedCredit: number | null;  // 전공 아니면 null
  category: keyof typeof KOR_LABELS;
  grade: string | null;
  isEnglish: boolean;             // ✅ 서버로 전송
};

export default function AddCourseModal({ open, sid, onClose, onSaved }: Props) {
  const [form, setForm] = useState<CourseInput>({
    name: "",
    credit: "",
    designedCredit: "",
    category: "FAITH_WORLDVIEW",
    grade: "",
    isEnglish: false,
  });
  const [errMsg, setErrMsg] = useState("");

  const isMajor = form.category === "MAJOR";

  /** 모달 열릴 때마다 폼 초기화 */
  useEffect(() => {
    if (open) {
      setErrMsg("");
      setForm({
        name: "",
        credit: "",
        designedCredit: "",
        category: "FAITH_WORLDVIEW",
        grade: "",
        isEnglish: false,
      });
    }
  }, [open]);

  /** 입력 변경 */
  const onChange = <K extends keyof CourseInput>(k: K, v: CourseInput[K]) => {
    // 숫자 필드 정제
    if (k === "credit" || k === "designedCredit") {
      if (typeof v === "string") v = (v as string).replace(/\D/g, "") as any;
    }
    // 카테고리 바꾸면 전공이 아닌 경우 설계학점 클리어
    if (k === "category") {
      const nextCat = v as CourseInput["category"];
      setForm((f) => ({
        ...f,
        category: nextCat,
        designedCredit: nextCat === "MAJOR" ? f.designedCredit : "",
      }));
      return;
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

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.name.trim()) {
      setErrMsg("과목명을 입력하세요.");
      return;
    }
    if (form.credit === "") {
      setErrMsg("학점을 입력하세요.");
      return;
    }
    setErrMsg("");

    // 문자열 → 숫자 변환
    const creditNum = Number(form.credit);
    const designedNum =
      isMajor && form.designedCredit !== "" ? Number(form.designedCredit) : null;

    const payload: CourseRequest = {
      name: form.name.trim(),
      credit: Number.isNaN(creditNum) ? 0 : creditNum,
      designedCredit: designedNum,
      category: form.category,
      grade: form.grade.trim() === "" ? null : form.grade.trim(),
      isEnglish: !!form.isEnglish, // ✅ 영어강의 여부
    };

    await addCourse.mutateAsync(payload);
  }

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
    row: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 } as const,
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
    hint: { color: "#6b7280", fontWeight: 400 } as const,
    toggleWrap: { display: "flex", alignItems: "center", gap: 10 } as const,
  };

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
            disabled={addCourse.isPending || !form.name || form.credit === ""}
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

        {/* 학점 / (전공일 때만) 설계학점 */}
        <div className="addcourse-row" style={s.row}>
          <div>
            <label style={s.label}>학점</label>
            <input
              type="text"                 // 문자열로 관리
              inputMode="numeric"         // 모바일 숫자 키패드
              pattern="[0-9]*"            // 숫자만
              style={s.input}
              value={form.credit}
              onChange={(e) => onChange("credit", e.target.value)}
              placeholder="0"
            />
          </div>

          {isMajor ? (
            <div>
              <label style={s.label}>
                설계학점 <span style={s.hint}>(전공만 해당)</span>
              </label>
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
          ) : (
            <div>
              <label style={s.label}>
                설계학점 <span style={s.hint}>(전공만 입력 가능)</span>
              </label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                style={{ ...s.input, background: "#f3f4f6" }}
                value=""
                disabled
                placeholder="-"
              />
            </div>
          )}
        </div>

        {/* 카테고리 */}
        <div style={s.field}>
          <label style={s.label}>카테고리</label>
          <select
            className="addcourse-select"
            style={s.input}
            value={form.category}
            onChange={(e) => onChange("category", e.target.value as CourseInput["category"])}
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
            성적 <span style={s.hint}>(예: A+, A0, B+, P 등)</span>
          </label>
          <input
            style={s.input}
            placeholder="예: A+, B0, P"
            value={form.grade}
            onChange={(e) => onChange("grade", e.target.value)}
          />
        </div>

        {/* 영어강의 여부 */}
        <div style={s.field}>
          <label style={s.label}>영어강의 여부</label>
          <div style={s.toggleWrap}>
            <label className="acm-toggle">
              <input
                type="checkbox"
                checked={form.isEnglish}
                onChange={(e) => onChange("isEnglish", e.target.checked)}
              />
              <span />
            </label>
            <span>{form.isEnglish ? "영어강의" : "일반강의"}</span>
          </div>
        </div>
      </form>
    </Modal>
  );
}
