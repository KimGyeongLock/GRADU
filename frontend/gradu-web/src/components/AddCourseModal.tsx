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

type CourseInput = {
  name: string;
  credit: string;          // 소수 허용 문자열 e.g. "3.5"
  designedCredit: string;  // 전공만 사용, 정수만
  category: keyof typeof KOR_LABELS;
  grade: string;
  isEnglish: boolean;
};

type CourseRequest = {
  name: string;
  credit: number;                // 0.5 단위 허용
  designedCredit: number | null; // 정수, 전공이 아니면 null
  category: keyof typeof KOR_LABELS;
  grade: string | null;
  isEnglish: boolean;
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

  const onChange = <K extends keyof CourseInput>(k: K, v: CourseInput[K]) => {
    if (k === "credit" && typeof v === "string") {
      // 숫자/점 1개만 허용 + 소수 1자리까지
      const cleaned = v
        .replace(/[^\d.]/g, "")
        .replace(/(\..*)\./g, "$1")
        .replace(/^(\d+)\.(\d?).*$/, "$1.$2");
      setForm((f) => ({ ...f, credit: cleaned }));
      return;
    }
    if (k === "designedCredit" && typeof v === "string") {
      // 정수만
      const cleaned = v.replace(/\D/g, "");
      setForm((f) => ({ ...f, designedCredit: cleaned }));
      return;
    }
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
      await axiosInstance.post(url, input, { headers: { "Content-Type": "application/json" } });
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
    if (!form.name.trim()) return setErrMsg("과목명을 입력하세요.");
    if (form.credit === "") return setErrMsg("학점을 입력하세요.");

    // 소수 검증: 0.5 단위만 허용
    const creditNum = Number(form.credit);
    if (Number.isNaN(creditNum)) return setErrMsg("학점이 올바르지 않습니다.");
    if (Math.round(creditNum * 2) !== creditNum * 2) {
      return setErrMsg("학점은 0.5 단위로 입력하세요.");
    }

    const designedNum =
      isMajor && form.designedCredit !== "" ? Number(form.designedCredit) : null;

    const payload: CourseRequest = {
      name: form.name.trim(),
      credit: creditNum,
      designedCredit: designedNum,
      category: form.category,
      grade: form.grade.trim() === "" ? null : form.grade.trim(),
      isEnglish: !!form.isEnglish,
    };

    await addCourse.mutateAsync(payload);
  }

  const ui = {
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
            style={ui.footerBtnBase}
            disabled={addCourse.isPending}
            onClick={onClose}
          >
            취소
          </button>
          <button
            form="add-course-form"
            type="submit"
            className="addcourse-btn addcourse-btn--primary"
            style={ui.footerBtnBase}
            disabled={addCourse.isPending || !form.name || form.credit === ""}
          >
            {addCourse.isPending ? "저장 중..." : "저장"}
          </button>
        </>
      }
    >
      <form id="add-course-form" onSubmit={onSubmit} style={ui.form}>
        {errMsg && <div style={ui.error}>{errMsg}</div>}

        {/* 과목명 */}
        <div style={ui.field}>
          <label style={ui.label}>과목명</label>
          <input
            style={ui.input}
            value={form.name}
            onChange={(e) => onChange("name", e.target.value)}
            placeholder="예: 객체지향 설계패턴"
          />
        </div>

        {/* 학점 / 설계학점(전공만) */}
        <div style={ui.row}>
          <div>
            <label style={ui.label}>학점</label>
            <input
              type="number"
              step="0.5"
              min="0"
              inputMode="decimal"
              style={ui.input}
              value={form.credit}
              onChange={(e) => onChange("credit", e.target.value)}
              placeholder="0 / 0.5 / 1.0 / 1.5 ..."
            />
          </div>

          {isMajor ? (
            <div>
              <label style={ui.label}>
                설계학점 <span style={ui.hint}>(정수, 전공만)</span>
              </label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                style={ui.input}
                value={form.designedCredit}
                onChange={(e) => onChange("designedCredit", e.target.value)}
                placeholder="0"
              />
            </div>
          ) : (
            <div>
              <label style={ui.label}>
                설계학점 <span style={ui.hint}>(전공만 입력 가능)</span>
              </label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                style={{ ...ui.input, background: "#f3f4f6" }}
                value=""
                disabled
                placeholder="-"
              />
            </div>
          )}
        </div>

        {/* 카테고리 */}
        <div style={ui.field}>
          <label style={ui.label}>카테고리</label>
          <select
            className="addcourse-select"
            style={ui.input}
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
        <div style={ui.field}>
          <label style={ui.label}>
            성적 <span style={ui.hint}>(예: A+, A0, B+, P 등)</span>
          </label>
          <input
            style={ui.input}
            placeholder="예: A+, B0, P"
            value={form.grade}
            onChange={(e) => onChange("grade", e.target.value)}
          />
        </div>

        {/* 영어강의 여부 */}
        <div style={ui.field}>
          <label style={ui.label}>영어강의 여부</label>
          <div style={ui.toggleWrap}>
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
