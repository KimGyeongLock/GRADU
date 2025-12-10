// src/components/EditCourseModal.tsx
import { useEffect, useMemo, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { axiosInstance } from "../../../lib/axios";
import Modal from "../../../components/Modal";
import type { CourseDto } from "../curriculumTypes";
import "../../../components/CourseModal.css";
import { isGuestMode } from "../../../lib/auth";
import { updateGuestCourse } from "../guest/guestStorage";
import { CourseOverwriteModal } from "./CourseOverwriteModal"; // 경로는 실제 위치에 맞게 조정 필요

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
type TermCode = (typeof TERM_OPTIONS)[number]["value"];

type Props = {
  open: boolean;
  course: CourseDto | null;
  sid: string;
  onClose: () => void;
  onSaved: () => void;
};

type FormState = {
  name: string;
  credit: string;
  designedCredit: string;
  grade: string;
  category: string;
  isEnglish: boolean;
  academicYear: string;
  term: TermCode;
};

export default function EditCourseModal({
  open,
  course,
  sid,
  onClose,
  onSaved,
}: Props) {
  const guest = isGuestMode();

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
  const [showNameConflictModal, setShowNameConflictModal] = useState(false);

  const isMajor = useMemo(() => form.category === "MAJOR", [form.category]);

  useEffect(() => {
    if (open && course) {
      setErrMsg("");
      setShowNameConflictModal(false);
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
        academicYear: String(
          course.academicYear ?? new Date().getFullYear()
        ),
        term: (course.term as TermCode) ?? "1",
      });
    }
  }, [open, course]);

  const onChange = <K extends keyof FormState>(k: K, v: FormState[K]) => {
    if (k === "credit" && typeof v === "string") {
      v = v
        .replace(/[^\d.]/g, "")
        .replace(/(\..*)\./g, "$1")
        .replace(/^(\d+)\.(\d?).*$/, "$1.$2") as any;
    }
    if (k === "designedCredit" && typeof v === "string") {
      v = v.replace(/\D/g, "") as any;
    }
    if (k === "academicYear" && typeof v === "string") {
      v = v.replace(/[^\d]/g, "").slice(0, 4) as any;
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
      if (creditNum != null) {
        if (Number.isNaN(creditNum)) throw new Error("학점이 올바르지 않습니다.");
        if (Math.round(creditNum * 2) !== creditNum * 2) {
          throw new Error("학점은 0.5 단위로 입력하세요.");
        }
      }

      const designedNumRaw =
        form.designedCredit.trim() === ""
          ? null
          : Number(form.designedCredit.trim());
      const designedNum = isMajor
        ? Number.isNaN(designedNumRaw as number)
          ? null
          : designedNumRaw
        : null;

      if (!form.academicYear || form.academicYear.length !== 4) {
        throw new Error("연도는 4자리(예: 2025)로 입력하세요.");
      }
      if (!TERM_OPTIONS.find((t) => t.value === form.term)) {
        throw new Error("학기를 선택하세요.");
      }

      if (guest) {
        updateGuestCourse(course.id, {
          name: form.name.trim(),
          credit: creditNum ?? course.credit,
          designedCredit: designedNum,
          grade:
            form.grade.trim() === "" ? null : form.grade.trim(),
          category: form.category,
          isEnglish: !!form.isEnglish,
          academicYear: Number(form.academicYear),
          term: form.term,
        });
        return;
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

      const url = `/api/v1/students/${encodeURIComponent(
        sid
      )}/courses/${course.id}`;
      await axiosInstance.patch(url, body);
    },
    onSuccess: () => onSaved(),
    onError: (e: any) => {
      const data = e?.response?.data;
      const status = e?.response?.status;
      const code =
        data?.code || data?.errorCode || data?.error || null;
      const text = data?.message || e?.message || "요청 실패";

      const isDuplicateByCode = code === "COURSE_DUPLICATE_EXCEPTION";
      const isDuplicateByStatusAndMsg =
        status === 409 &&
        typeof text === "string" &&
        text.includes("이미 동일한 과목이 존재합니다");

      if (isDuplicateByCode || isDuplicateByStatusAndMsg) {
        // 이름 중복: 배너는 숨기고 안내 모달만 띄움
        setErrMsg("");
        setShowNameConflictModal(true);
        return;
      }

      const msg = e?.message || e?.response?.data?.message;
      if (msg) setErrMsg(String(msg));
      else {
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
    setErrMsg("");
    await patchMutation.mutateAsync();
  };

  return (
    <>
      <Modal
        open={open}
        onClose={() => !patchMutation.isPending && onClose()}
        title="과목 수정"
        footer={
          <>
            <button
              type="button"
              className="cm-btn cm-btn-ghost"
              disabled={patchMutation.isPending}
              onClick={onClose}
            >
              취소
            </button>
            <button
              form="edit-course-form"
              type="submit"
              className="cm-btn cm-btn-primary"
              disabled={patchMutation.isPending}
            >
              {patchMutation.isPending ? "저장 중…" : "저장"}
            </button>
          </>
        }
      >
        <form
          id="edit-course-form"
          onSubmit={onSubmit}
          className="cm-form"
        >
          {errMsg && <div className="cm-error">{errMsg}</div>}

          <div className="cm-field">
            <label className="cm-label">과목명</label>
            <input
              className="cm-input"
              value={form.name}
              onChange={(e) => onChange("name", e.target.value)}
              placeholder="예: 자료구조"
            />
          </div>

          <div className="cm-grid2">
            <div className="cm-field">
              <label className="cm-label">학점</label>
              <input
                className="cm-input"
                type="text"
                inputMode="decimal"
                value={form.credit}
                onChange={(e) => onChange("credit", e.target.value)}
                placeholder="예: 3 / 3.5"
              />
            </div>

            <div className="cm-field">
              <label className="cm-label">카테고리</label>
              <select
                className="cm-input"
                value={form.category}
                onChange={(e) => onChange("category", e.target.value)}
              >
                {CATEGORY_ORDER.map((key) => (
                  <option key={key} value={key}>
                    {KOR_LABELS[key]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {isMajor && (
            <div className="cm-field">
              <label className="cm-label">설계학점</label>
              <input
                className="cm-input"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={form.designedCredit}
                onChange={(e) => onChange("designedCredit", e.target.value)}
                placeholder="예: 2"
              />
            </div>
          )}

          <div className="cm-grid2">
            <div className="cm-field">
              <label className="cm-label">연도</label>
              <input
                className="cm-input"
                inputMode="numeric"
                pattern="\d{4}"
                value={form.academicYear}
                onChange={(e) => onChange("academicYear", e.target.value)}
                placeholder="예: 2025"
              />
            </div>

            <div className="cm-field">
              <label className="cm-label">학기</label>
              <select
                className="cm-input"
                value={form.term}
                onChange={(e) => onChange("term", e.target.value as TermCode)}
              >
                {TERM_OPTIONS.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="cm-field">
            <label className="cm-label">성적</label>
            <input
              className="cm-input"
              value={form.grade}
              onChange={(e) => onChange("grade", e.target.value)}
              placeholder="예: A+"
            />
          </div>

          <div className="cm-field">
            <label className="cm-label">영어강의 여부</label>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <label className="cm-toggle">
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

      {/* 과목명 중복 안내 모달 */}
      <CourseOverwriteModal
        open={showNameConflictModal}
        title="과목명 중복"
        description={
          <>
            이미{" "}
            <strong>{form.name.trim() || "해당 이름"}</strong> 과목이
            존재합니다.
            <br />
            다른 이름으로 변경한 뒤 다시 저장해 주세요.
          </>
        }
        confirmLabel="확인"
        onConfirm={() => setShowNameConflictModal(false)}
      />
    </>
  );
}
