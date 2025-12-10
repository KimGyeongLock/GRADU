// src/components/AddCourseModal.tsx
import { useState, useEffect, useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import { axiosInstance } from "../../../lib/axios";
import Modal from "../../../components/Modal";
import "../../../components/CourseModal.css";
import { isGuestMode } from "../../../lib/auth";
import { addGuestCourse } from "../guest/guestStorage";
import type { CourseDto } from "../curriculumTypes";
import { CourseOverwriteModal } from "./CourseOverwriteModal";

type Props = {
  open: boolean;
  sid: string;
  onClose: () => void;
  onSaved: () => void;
  /** 모달 오픈 시 기본 학기 (선택사항) */
  initialYear?: number;
  initialTerm?: "1" | "2" | "sum" | "win";
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

const TERM_OPTIONS = [
  { value: "1", label: "1학기" },
  { value: "2", label: "2학기" },
  { value: "sum", label: "여름학기(summer)" },
  { value: "win", label: "겨울학기(winter)" },
] as const;
type TermCode = (typeof TERM_OPTIONS)[number]["value"];

type CourseInput = {
  name: string;
  credit: string;
  designedCredit: string;
  category: keyof typeof KOR_LABELS;
  grade: string;
  isEnglish: boolean;
  academicYear: string;
  term: TermCode;
};

type CourseRequest = {
  name: string;
  credit: number;
  designedCredit: number | null;
  category: keyof typeof KOR_LABELS;
  grade: string | null;
  isEnglish: boolean;
  academicYear: number;
  term: TermCode;
};

export default function AddCourseModal({
  open,
  sid,
  onClose,
  onSaved,
  initialYear,
  initialTerm,
}: Props) {
  const guest = isGuestMode();
  const defaultYear = new Date().getFullYear();
  const defaultTerm: TermCode = "1";

  const [form, setForm] = useState<CourseInput>({
    name: "",
    credit: "",
    designedCredit: "",
    category: "FAITH_WORLDVIEW",
    grade: "",
    isEnglish: false,
    academicYear: String(defaultYear),
    term: defaultTerm,
  });
  const [errMsg, setErrMsg] = useState("");

  const isMajor = form.category === "MAJOR";

  // 🔽 덮어쓰기 확인 모달 상태
  const [showOverwriteModal, setShowOverwriteModal] = useState(false);
  const [isOverwriteSaving, setIsOverwriteSaving] = useState(false);
  const lastPayloadRef = useRef<CourseRequest | null>(null);

  // 모달 열릴 때마다 초기값 세팅
  useEffect(() => {
    if (open) {
      setErrMsg("");
      setShowOverwriteModal(false);
      setIsOverwriteSaving(false);
      lastPayloadRef.current = null;

      setForm({
        name: "",
        credit: "",
        designedCredit: "",
        category: "FAITH_WORLDVIEW",
        grade: "",
        isEnglish: false,
        academicYear: String(initialYear ?? defaultYear),
        term: (initialTerm ?? defaultTerm) as TermCode,
      });
    }
  }, [open, initialYear, initialTerm, defaultYear, defaultTerm]);

  const onChange = <K extends keyof CourseInput>(
    k: K,
    v: CourseInput[K]
  ) => {
    if (k === "credit" && typeof v === "string") {
      const cleaned = v
        .replace(/[^\d.]/g, "")
        .replace(/(\..*)\./g, "$1")
        .replace(/^(\d+)\.(\d?).*$/, "$1.$2");
      setForm((f) => ({ ...f, credit: cleaned }));
      return;
    }
    if (k === "designedCredit" && typeof v === "string") {
      const cleaned = v.replace(/\D/g, "");
      setForm((f) => ({ ...f, designedCredit: cleaned }));
      return;
    }
    if (k === "academicYear" && typeof v === "string") {
      const cleaned = v.replace(/[^\d]/g, "").slice(0, 4);
      setForm((f) => ({ ...f, academicYear: cleaned }));
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
      if (guest) {
        // 게스트 모드: sessionStorage에만 저장
        addGuestCourse({
          name: input.name,
          credit: input.credit,
          designedCredit: input.designedCredit,
          category: input.category as any,
          grade: input.grade,
          isEnglish: input.isEnglish,
          academicYear: input.academicYear,
          term: input.term,
        } as Omit<CourseDto, "id">);
        return;
      }

      const url = `/api/v1/students/${encodeURIComponent(sid)}/courses`;
      await axiosInstance.post(url, input, {
        headers: { "Content-Type": "application/json" },
      });
    },
    onSuccess: () => {
      setErrMsg("");
      onSaved();
      onClose();
    },
    onError: (e: any) => {
  if (guest) {
    setErrMsg("저장 중 오류가 발생했습니다.");
    return;
  }

  const status = e?.response?.status;
  const data = e?.response?.data;
  const code =
    data?.code || data?.errorCode || data?.error || null; // 혹시 다른 키를 쓰고 있을 수도 있어서
  const text = data?.message || e?.message || "요청 실패";

  // ✅ 1) 명시적인 code 로 체크
  const isDuplicateByCode = code === "COURSE_DUPLICATE_EXCEPTION";

  // ✅ 2) code 가 없더라도, 409 + 메시지 내용으로 중복 판정
  const isDuplicateByStatusAndMsg =
    status === 409 && typeof text === "string" &&
    text.includes("이미 동일한 과목이 존재합니다");

  if (isDuplicateByCode || isDuplicateByStatusAndMsg) {
    // 중복 과목 → 덮어쓰기 모달만 띄우고, 배너는 감춤
    setErrMsg("");
    setShowOverwriteModal(true);
    return;
  }

  // 그 외 일반 에러
  setErrMsg(`저장 실패 (${status ?? "-"}) : ${text}`);
  console.error("[addCourse] error", e);
},

  });

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.name.trim()) return setErrMsg("과목명을 입력하세요.");
    if (form.credit === "") return setErrMsg("학점을 입력하세요.");
    if (!form.academicYear || form.academicYear.length !== 4) {
      return setErrMsg("연도는 4자리(예: 2025)로 입력하세요.");
    }
    if (!TERM_OPTIONS.find((t) => t.value === form.term)) {
      return setErrMsg("학기를 선택하세요.");
    }

    const creditNum = Number(form.credit);
    if (Number.isNaN(creditNum))
      return setErrMsg("학점이 올바르지 않습니다.");
    if (Math.round(creditNum * 2) !== creditNum * 2) {
      return setErrMsg("학점은 0.5 단위로 입력하세요.");
    }

    const designedNum =
      isMajor && form.designedCredit !== ""
        ? Number(form.designedCredit)
        : null;

    const payload: CourseRequest = {
      name: form.name.trim(),
      credit: creditNum,
      designedCredit: designedNum,
      category: form.category,
      grade: form.grade.trim() === "" ? null : form.grade.trim(),
      isEnglish: !!form.isEnglish,
      academicYear: Number(form.academicYear),
      term: form.term,
    };

    // 마지막 제출값 저장 (덮어쓰기 시 재사용)
    lastPayloadRef.current = payload;

    try {
      await addCourse.mutateAsync(payload);
    } catch {
      // onError에서 처리하므로 여기선 무시
    }
  }

  const handleOverwriteCancel = () => {
    setShowOverwriteModal(false);
    setErrMsg(
      "이미 같은 이름의 과목이 등록되어 있습니다. 과목명을 바꾸거나 기존 과목을 수정해 주세요."
    );
  };

  const handleOverwriteConfirm = async () => {
    const payload = lastPayloadRef.current;
    if (!payload || guest) {
      setShowOverwriteModal(false);
      return;
    }

    try {
      setIsOverwriteSaving(true);
      const url = `/api/v1/students/${encodeURIComponent(
        sid
      )}/courses?overwrite=true`;

      await axiosInstance.post(url, payload, {
        headers: { "Content-Type": "application/json" },
      });

      setShowOverwriteModal(false);
      setErrMsg("");
      onSaved();
      onClose();
    } catch (e: any) {
      console.error("[addCourse overwrite] error", e);
      const status = e?.response?.status;
      const text = e?.response?.data?.message || e?.message || "요청 실패";
      setErrMsg(`저장 실패 (${status ?? "-"}) : ${text}`);
      setShowOverwriteModal(false);
    } finally {
      setIsOverwriteSaving(false);
    }
  };

  return (
    <>
      <Modal
        open={open}
        onClose={() =>
          !addCourse.isPending && !isOverwriteSaving && onClose()
        }
        title="과목 추가"
        footer={
          <>
            <button
              type="button"
              className="cm-btn cm-btn-ghost"
              disabled={addCourse.isPending || isOverwriteSaving}
              onClick={onClose}
            >
              취소
            </button>
            <button
              form="add-course-form"
              type="submit"
              className="cm-btn cm-btn-primary"
              disabled={
                addCourse.isPending ||
                isOverwriteSaving ||
                !form.name ||
                form.credit === ""
              }
            >
              {addCourse.isPending || isOverwriteSaving ? "저장 중..." : "저장"}
            </button>
          </>
        }
      >
        <form
          id="add-course-form"
          onSubmit={onSubmit}
          className="cm-form"
        >
          {errMsg && <div className="cm-error">{errMsg}</div>}

          {/* 과목명 */}
          <div className="cm-field">
            <label className="cm-label">과목명</label>
            <input
              className="cm-input"
              value={form.name}
              onChange={(e) => onChange("name", e.target.value)}
              placeholder="예: 객체지향 설계패턴"
            />
          </div>

          {/* 학점 / 설계학점 */}
          <div className="cm-grid2">
            <div className="cm-field">
              <label className="cm-label">학점</label>
              <input
                type="text"
                inputMode="decimal"
                className="cm-input"
                value={form.credit}
                onChange={(e) => onChange("credit", e.target.value)}
                placeholder="0 / 0.5 / 1.0 / 1.5 ..."
              />
            </div>

            <div className="cm-field">
              <label className="cm-label">
                설계학점
                <span className="cm-hint">(전공만 입력)</span>
              </label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                className="cm-input"
                value={isMajor ? form.designedCredit : ""}
                onChange={(e) => onChange("designedCredit", e.target.value)}
                placeholder={isMajor ? "0" : "-"}
                disabled={!isMajor}
                style={!isMajor ? { background: "#f3f4f6" } : undefined}
              />
            </div>
          </div>

          {/* 카테고리 */}
          <div className="cm-field">
            <label className="cm-label">카테고리</label>
            <select
              className="cm-input"
              value={form.category}
              onChange={(e) =>
                onChange("category", e.target.value as CourseInput["category"])
              }
            >
              {ORDER.map((key) => (
                <option key={key} value={key}>
                  {KOR_LABELS[key]}
                </option>
              ))}
            </select>
          </div>

          {/* 학기(연도 + 학기) */}
          <div className="cm-grid2">
            <div className="cm-field">
              <label className="cm-label">연도</label>
              <input
                className="cm-input"
                inputMode="numeric"
                pattern="\d{4}"
                placeholder="예: 2025"
                value={form.academicYear}
                onChange={(e) => onChange("academicYear", e.target.value)}
              />
            </div>

            <div className="cm-field">
              <label className="cm-label">학기</label>
              <select
                className="cm-input"
                value={form.term}
                onChange={(e) =>
                  onChange("term", e.target.value as TermCode)
                }
              >
                {TERM_OPTIONS.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 성적 */}
          <div className="cm-field">
            <label className="cm-label">
              성적 <span className="cm-hint">(예: A+, A0, B+, P 등)</span>
            </label>
            <input
              className="cm-input"
              placeholder="예: A+, B0, P"
              value={form.grade}
              onChange={(e) => onChange("grade", e.target.value)}
            />
          </div>

          {/* 영어강의 여부 */}
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

      {/* 덮어쓰기 확인 모달 */}
      {showOverwriteModal && (
        <CourseOverwriteModal
        open={showOverwriteModal}
        title="과목 덮어쓰기"
        description={
          <>
            이미 <strong>{form.name.trim() || "해당 이름"}</strong> 과목이
            등록되어 있습니다.
            <br />
            현재 입력한 내용으로 기존 과목 정보를{" "}
            <strong>덮어쓰기</strong> 하시겠어요?
          </>
        }
        confirmLabel={isOverwriteSaving ? "덮어쓰는 중..." : "덮어쓰기"}
        onConfirm={handleOverwriteConfirm}
        confirmDisabled={isOverwriteSaving}
        cancelLabel="취소"
        onCancel={handleOverwriteCancel}
      />
      )}
    </>
  );
}
