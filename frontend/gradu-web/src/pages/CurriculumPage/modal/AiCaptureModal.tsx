// src/pages/CurriculumPage/modal/AiCaptureModal.tsx
import { useState, useEffect } from "react";
import type { ChangeEvent, MouseEvent } from "react";
import { axiosInstance } from "../../../lib/axios";
import s from "./AiCaptureModal.module.css";
import { AiLoadingModal } from "./AiLoadingModal";
import { AiResultModal } from "./AiResultModal";
import type { AiCourseResult } from "./AiResultModal";

interface AiCaptureModalProps {
  open: boolean;
  sid: string;
  onClose: () => void;
  onSaved: () => void;
  exampleImageUrl?: string;
}

export function AiCaptureModal({
  open,
  sid,
  onClose,
  onSaved,
  exampleImageUrl,
}: AiCaptureModalProps) {
  const [files, setFiles] = useState<File[]>([]);

  // 모달 상태
  const [showUpload, setShowUpload] = useState(true);
  const [showLoading, setShowLoading] = useState(false);
  const [showResult, setShowResult] = useState(false);

  const [aiResult, setAiResult] = useState<AiCourseResult[]>([]);
  const [checked, setChecked] = useState<boolean[]>([]);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // 공통 파일 추가 함수 (input + 클립보드에서 같이 사용)
  const appendFiles = (incoming: File[]) => {
    if (!incoming.length) return;

    setFiles(prev => {
      const merged = [...prev, ...incoming];
      return merged.slice(0, 5); // 최대 5장
    });
  };

  // 모달 열릴 때 뒷 스크롤 막기
  useEffect(() => {
    if (!open) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [open]);

  // 모달 닫힐 때 상태 리셋
  useEffect(() => {
    if (!open) {
      setFiles([]);
      setAiResult([]);
      setChecked([]);
      setShowUpload(true);
      setShowLoading(false);
      setShowResult(false);
      setIsAnalyzing(false);
      setIsSaving(false);
    }
  }, [open]);

  // 클립보드에서 이미지 붙여넣기 지원 (모달 열려 있을 때만)
  useEffect(() => {
    if (!open) return;

    const handlePaste = (e: ClipboardEvent) => {
      // 인풋/텍스트 영역에 포커스 중이면 기본 붙여넣기 유지
      const active = document.activeElement;
      if (
        active &&
        (active.tagName === "INPUT" ||
          active.tagName === "TEXTAREA" ||
          (active as HTMLElement).isContentEditable)
      ) {
        return;
      }

      const items = e.clipboardData?.items;
      if (!items) return;

      const images: File[] = [];
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        if (item.type.startsWith("image/")) {
          const file = item.getAsFile();
          if (file) images.push(file);
        }
      }

      if (!images.length) return;

      // 우리가 이미지로 처리할 거니 기본 붙여넣기는 막아도 됨
      e.preventDefault();
      appendFiles(images);
    };

    window.addEventListener("paste", handlePaste);
    return () => {
      window.removeEventListener("paste", handlePaste);
    };
  }, [open]);

  if (!open) return null;

  const handleBackdropClick = () => {
    // 로딩/결과 모달이 떠 있을 때는 배경 클릭으로 닫히지 않음
    if (showLoading || showResult) return;
    if (isAnalyzing || isSaving) return;
    onClose();
  };

  const stopPropagation = (e: MouseEvent<HTMLDivElement>) => {
    e.stopPropagation();
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(e.target.files ?? []);
    if (!selected.length) return;

    appendFiles(selected);
    e.target.value = "";
  };

  const handleRemoveFile = (index: number) => {
    setFiles(prev => prev.filter((_, i) => i !== index));
  };

  /** 1단계: 분석 요청 */
  const handleAnalyze = async () => {
    if (!files.length || isAnalyzing) return;

    setIsAnalyzing(true);
    setShowLoading(true);

    try {
      const formData = new FormData();
      files.forEach(file => formData.append("images", file));

      const { data } = await axiosInstance.post(
        "/api/v1/ai/course-capture",
        formData,
      );

      let list: AiCourseResult[] = [];

      if (Array.isArray(data)) {
        list = data as AiCourseResult[];
      } else if (data && Array.isArray((data as any).courses)) {
        list = (data as any).courses as AiCourseResult[];
      } else {
        console.warn("Unexpected AI response shape:", data);
      }

      setAiResult(list);
      setChecked(new Array(list.length).fill(true));

      // 업로드/로딩 모달 내려가고 결과 모달만 남김
      setShowUpload(false);
      setShowLoading(false);
      setShowResult(true);
    } catch (err) {
      console.error(err);
      alert("AI 분석 중 오류가 발생했습니다. 다시 시도해 주세요.");
      setShowLoading(false);
    } finally {
      setIsAnalyzing(false);
    }
  };

  /** 로딩 모달 X 버튼 */
  const handleLoadingClose = () => {
    setShowLoading(false);
    setIsAnalyzing(false);
  };

  /** 결과 체크 토글 */
  const toggleChecked = (idx: number) => {
    setChecked(prev => {
      const copy = [...prev];
      copy[idx] = !copy[idx];
      return copy;
    });
  };

  /** 선택 저장 */
  const handleSaveSelected = async () => {
    const payload = aiResult.filter((_, i) => checked[i]);
    if (!payload.length) {
      alert("저장할 과목을 하나 이상 선택해 주세요.");
      return;
    }

    setIsSaving(true);
    try {
      await axiosInstance.post(`/api/v1/students/${sid}/courses/bulk`, payload);

      alert(`선택한 ${payload.length}개 과목을 저장했습니다.`);
      onSaved();
      onClose();
    } catch (e) {
      console.error(e);
      alert("저장 중 오류가 발생했습니다.");
    } finally {
      setIsSaving(false);
    }
  };

  /** 전체 저장 */
  const handleSaveAll = async () => {
    if (!aiResult.length) return;

    setIsSaving(true);
    try {
      await axiosInstance.post(`/api/v1/students/${sid}/courses/bulk`, aiResult);

      alert(`총 ${aiResult.length}개 과목을 저장했습니다.`);
      onSaved();
      onClose();
    } catch (e) {
      console.error(e);
      alert("저장 중 오류가 발생했습니다.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleResultClose = () => {
    if (isSaving) return;
    onClose();
  };

  return (
    <>
      {/* 기본 업로드/분석 모달은 기존처럼 backdrop + 카드 */}
      <div className={s.modalBackdrop} onClick={handleBackdropClick}>
        {showUpload && (
          <div className={s.aiModal} onClick={stopPropagation}>
            {/* 헤더 */}
            <div className={s.aiHeader}>
              <h2>AI 캡쳐로 과목 한꺼번에 추가</h2>
              <p className={s.aiSub}>
                학사 시스템 화면을 캡쳐해서 올려주면, 과목/학점/성적을 자동으로 추출해 드려요.
              </p>
            </div>

            {/* 예시 이미지 영역 */}
            <div className={s.aiExampleBox}>
              <div className={s.aiExampleTopRow}>
                <span className={s.aiExampleBadge}>예시 이미지</span>
                <span className={s.aiExampleCaption}>
                  이런 형태로 캡쳐하면 인식률이 더 좋아요.
                </span>
              </div>

              {exampleImageUrl ? (
                <img
                  src={exampleImageUrl}
                  alt="예시 성적표 화면"
                  className={s.aiExampleImg}
                />
              ) : (
                <div className={s.aiExamplePlaceholder}>
                  <div className={s.aiExampleMockHeader} />
                  <div className={s.aiExampleMockRow} />
                  <div className={s.aiExampleMockRow} />
                  <div className={s.aiExampleMockRowShort} />
                  <span className={s.aiExampleLabel}>예시 이미지가 들어갈 자리</span>
                </div>
              )}
            </div>

            {/* 설명 텍스트 */}
            <div className={s.aiHintBlock}>
              <p className={s.aiHintTitle}>이미지 업로드 안내</p>
              <p className={s.aiHintText}>
                Hisnet <span>&gt;</span> 학사정보 <span>&gt;</span> 졸업 탭{" "}
                <span>&gt;</span> 졸업심사결과조회 <span>&gt;</span> 졸업심사 결과보기 화면에서{" "}
                <strong>카테고리(ex 신앙및세계관) / 연도 / 학기 / 과목명 / 학점(설계) / 성적</strong>
                이 모두 보이도록 예시 이미지처럼 캡쳐해 주세요.
              </p>
            </div>

            {/* 업로드 영역 */}
            <div className={s.aiUploadRow}>
              <label className={s.aiUploadBtn}>
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  onChange={handleFileChange}
                  hidden
                />
                <span className={s.aiUploadBtnIcon}>📷</span>
                <span>이미지 선택</span>
              </label>

              <span className={s.aiUploadInfo}>
                학사 페이지 캡쳐 이미지를 <strong>최대 5장</strong>까지 업로드할 수 있어요.<br/>복사/붙여놓기로도 가능해요.
              </span>
            </div>

            {/* 선택된 파일 목록 */}
            {files.length > 0 && (
              <div className={s.aiFileListWrap}>
                <div className={s.aiFileListHeader}>
                  선택된 이미지 <span>({files.length}/5)</span>
                </div>
                <ul className={s.aiFileList}>
                  {files.map((f, idx) => (
                    <li key={idx} className={s.aiFileItem}>
                      <span className={s.aiFileName}>{f.name}</span>
                      <button
                        type="button"
                        className={s.aiFileRemove}
                        onClick={() => handleRemoveFile(idx)}
                        aria-label="이미지 제거"
                        disabled={isAnalyzing || isSaving}
                      >
                        ✕
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* 푸터 버튼 */}
            <div className={s.aiFooter}>
              <button
                type="button"
                className={s.aiSecondaryBtn}
                onClick={onClose}
                disabled={isAnalyzing || isSaving}
              >
                취소
              </button>
              <button
                type="button"
                className={s.aiPrimaryBtn}
                onClick={handleAnalyze}
                disabled={!files.length || isAnalyzing}
              >
                {isAnalyzing ? "분석 중…" : "AI로 분석하기"}
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 로딩 모달 (전체 화면 덮는 overlay, X만 누를 수 있음) */}
      <AiLoadingModal open={showLoading} onClose={handleLoadingClose} />

      {/* 결과 모달 (업로드/로딩은 내려간 상태에서 이것만 보이게) */}
      <AiResultModal
        open={showResult}
        courses={aiResult}
        checked={checked}
        isSaving={isSaving}
        onToggleChecked={toggleChecked}
        onSaveSelected={handleSaveSelected}
        onSaveAll={handleSaveAll}
        onClose={handleResultClose}
      />
    </>
  );
}
