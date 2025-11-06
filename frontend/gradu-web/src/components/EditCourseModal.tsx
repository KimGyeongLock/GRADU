// src/components/EditCourseModal.tsx
import { useEffect, useMemo, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { axiosInstance } from "../lib/axios";
import Modal from "./Modal";
import s from "../pages/CurriculumDetail.module.css";
import type { CourseDto } from "../pages/CurriculumDetailPage";

const KOR_LABELS: Record<string, string> = {
  FAITH_WORLDVIEW: "신앙및세계관",
  PERSONALITY_LEADERSHIP: "인성및리더십",
  PRACTICAL_ENGLISH: "실무영어",
  GENERAL_EDU: "전문교양",
  BSM: "BSM",
  ICT_INTRO: "ICT융합기초",
  FREE_ELECTIVE_BASIC: "자유선택(교양)",
  FREE_ELECTIVE_MJR: "자유선택(교양또는비교양)",
  MAJOR: "전공",
};
const CATEGORY_ORDER = Object.keys(KOR_LABELS);

const TERM_OPTIONS = [
  { value: "1", label: "1학기" },
  { value: "2", label: "2학기" },
  { value: "sum", label: "여름학기(summer)" },
  { value: "win", label: "겨울학기(winter)" },
] as const;
type TermCode = typeof TERM_OPTIONS[number]["value"];

type Props = {
  open: boolean;
  course: CourseDto | null;
  sid: string;
  onClose: () => void;
  onSaved: () => void;
};

type FormState = {
  name: string;
  credit: string;          // 문자열로 관리
  designedCredit: string;  // 문자열
  grade: string;
  category: string;
  isEnglish: boolean;
  academicYear: string;    // "2025"
  term: TermCode;          // "1"|"2"|"sum"|"win"
};

export default function EditCourseModal({ open, course, sid, onClose, onSaved }: Props) {
  const [form, setForm] = useState<FormState>({
    name: "",
    credit: "",
    designedCredit: "",
    grade: "",
    category: "MAJOR",
    isEnglish: false,
    academicYear: String(new Date().getFullYear()),
    term: "1",
  });
  const [errMsg, setErrMsg] = useState("");

  const isMajor = useMemo(() => form.category === "MAJOR", [form.category]);

  useEffect(() => {
    if (open && course) {
      setErrMsg("");
      setForm({
        name: course.name ?? "",
        credit: String(course.credit ?? ""),
        designedCredit:
          course.category === "MAJOR" && course.designedCredit != null
            ? String(course.designedCredit)
            : "",
        grade: course.grade ?? "",
        category: course.category ?? "MAJOR",
        isEnglish: !!course.isEnglish,
        academicYear: String(course.academicYear ?? new Date().getFullYear()),
        term: (course.term as TermCode) ?? "1",
      });
    }
  }, [open, course]);

  const onChange = <K extends keyof FormState>(k: K, v: FormState[K]) => {
    if (k === "credit") {
      if (typeof v === "string") {
        // 숫자/점 1개 + 소수 1자리
        v = (v as string)
          .replace(/[^\d.]/g, "")
          .replace(/(\..*)\./g, "$1")
          .replace(/^(\d+)\.(\d?).*$/, "$1.$2") as any;
      }
    }
    if (k === "designedCredit" && typeof v === "string") {
      v = (v as string).replace(/\D/g, "") as any; // 정수만
    }
    if (k === "academicYear" && typeof v === "string") {
      v = (v as string).replace(/[^\d]/g, "").slice(0, 4) as any; // 4자리
    }
    if (k === "category") {
      const nextCat = v as string;
      setForm((f) => ({
        ...f,
        category: nextCat,
        designedCredit: nextCat === "MAJOR" ? f.designedCredit : "",
      }));
      return;
    }
    setForm((f) => ({ ...f, [k]: v }));
  };

  const patchMutation = useMutation({
    mutationFn: async () => {
      if (!course) return;

      const creditNum =
        form.credit.trim() === "" ? null : Number(form.credit.trim());

      // 0.5 단위 검증(입력했을 때만)
      if (creditNum != null) {
        if (Number.isNaN(creditNum)) throw new Error("학점이 올바르지 않습니다.");
        if (Math.round(creditNum * 2) !== creditNum * 2) {
          throw new Error("학점은 0.5 단위로 입력하세요.");
        }
      }

      const designedNumRaw =
        form.designedCredit.trim() === "" ? null : Number(form.designedCredit.trim());
      const designedNum =
        isMajor ? (Number.isNaN(designedNumRaw as number) ? null : designedNumRaw) : null;

      // 학기 검증
      if (!form.academicYear || form.academicYear.length !== 4) {
        throw new Error("연도는 4자리(예: 2025)로 입력하세요.");
      }
      if (!TERM_OPTIONS.find(t => t.value === form.term)) {
        throw new Error("학기를 선택하세요.");
      }

      const body: Partial<{
        name: string;
        credit: number | null;
        designedCredit: number | null;
        grade: string | null;
        category: string | null;
        isEnglish: boolean;
        academicYear: number | null;
        term: TermCode | null;
      }> = {
        name: form.name.trim() === "" ? undefined : form.name.trim(),
        credit: creditNum,
        designedCredit: designedNum,
        grade: form.grade.trim() === "" ? null : form.grade.trim(),
        category: form.category,
        isEnglish: !!form.isEnglish,
        academicYear: Number(form.academicYear),
        term: form.term,
      };

      const url = `/api/v1/students/${encodeURIComponent(sid)}/courses/${course.id}`;
      await axiosInstance.patch(url, body);
    },
    onSuccess: () => onSaved(),
    onError: (e: any) => {
      const msg = e?.message || e?.response?.data?.message;
      if (msg) setErrMsg(String(msg));
      else {
        const status = e?.response?.status;
        const text = e?.response?.data?.message || e?.message || "요청 실패";
        setErrMsg(`저장 실패 (${status ?? "-"}) : ${text}`);
      }
      console.error("[EditCourse] error", e);
    },
  });

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!course) return;
    if (!form.name.trim()) return setErrMsg("과목명을 입력하세요.");
    if (form.credit.trim() === "") return setErrMsg("학점을 입력하세요.");
    if (!form.academicYear || form.academicYear.length !== 4)
      return setErrMsg("연도는 4자리(예: 2025)로 입력하세요.");
    setErrMsg("");
    await patchMutation.mutateAsync();
  };

  return (
    <Modal
      open={open}
      onClose={() => !patchMutation.isPending && onClose()}
      title="과목 수정"
      footer={
        <>
          <button
            type="button"
            className={s.btnGhost}
            disabled={patchMutation.isPending}
            onClick={onClose}
          >
            취소
          </button>
          <button
            form="edit-course-form"
            type="submit"
            className={s.btnPrimary}
            disabled={patchMutation.isPending}
          >
            {patchMutation.isPending ? "저장 중…" : "저장"}
          </button>
        </>
      }
    >
      <form id="edit-course-form" onSubmit={onSubmit} className={s.form}>
        {errMsg && <div className={s.error}>{errMsg}</div>}

        <div className={s.label}>
          과목명
          <input
            className={s.input}
            value={form.name}
            onChange={(e) => onChange("name", e.target.value)}
            placeholder="예: 자료구조"
          />
        </div>

        <div className={s.grid2}>
          <label className={s.label}>
            학점
            <input
              className={s.input}
              type="text"
              inputMode="decimal"
              value={form.credit}
              onChange={(e) => onChange("credit", e.target.value)}
              placeholder="예: 3 / 3.5"
            />
          </label>

          <label className={s.label}>
            카테고리
            <select
              className={s.input}
              value={form.category}
              onChange={(e) => onChange("category", e.target.value)}
            >
              {CATEGORY_ORDER.map((key) => (
                <option key={key} value={key}>
                  {KOR_LABELS[key]}
                </option>
              ))}
            </select>
          </label>
        </div>

        {isMajor && (
          <label className={s.label}>
            설계학점
            <input
              className={s.input}
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={form.designedCredit}
              onChange={(e) => onChange("designedCredit", e.target.value)}
              placeholder="예: 2"
            />
          </label>
        )}

        {/* 학기(연도+학기) */}
        <div className={s.grid2}>
          <label className={s.label}>
            연도
            <input
              className={s.input}
              inputMode="numeric"
              pattern="\d{4}"
              value={form.academicYear}
              onChange={(e) => onChange("academicYear", e.target.value)}
              placeholder="예: 2025"
            />
          </label>

          <label className={s.label}>
            학기
            <select
              className={s.input}
              value={form.term}
              onChange={(e) => onChange("term", e.target.value as TermCode)}
            >
              {TERM_OPTIONS.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </label>
        </div>

        <label className={s.label}>
          성적
          <input
            className={s.input}
            value={form.grade}
            onChange={(e) => onChange("grade", e.target.value)}
            placeholder="예: A+"
          />
        </label>

        <div className={s.label}>
          영어강의 여부
          <div>
            <label style={{ display: "inline-flex", alignItems: "center", gap: 8 }}>
              <input
                type="checkbox"
                checked={form.isEnglish}
                onChange={(e) => onChange("isEnglish", e.target.checked)}
              />
              <span>{form.isEnglish ? "영어강의" : "일반강의"}</span>
            </label>
          </div>
        </div>
      </form>
    </Modal>
  );
}
