import { useState } from "react";
import s from "../CurriculumDetail.module.css";

import {
  MAJOR_REQUIRED_COURSES,
  MAJOR_ELECTIVE_REQUIRED,
} from "../constants/major";

type MajorInfoBoxProps = {
  takenSet: Set<string>;
  normalize: (s: string) => string;
  electiveTakenCount: number;
};

export function MajorInfoBox({
  takenSet,
  normalize,
  electiveTakenCount,
}: MajorInfoBoxProps) {
  const [open, setOpen] = useState(false);

  return (
    <div className={s.noticeBox}>
      <div
        className={s.noticeToggle}
        onClick={() => setOpen((v) => !v)}
      >
        <span className={s.noticeTitle}>전공 이수 안내</span>
        <span className={s.noticeArrow}>{open ? "▲" : "▼"}</span>
      </div>

      {open && (
        <div className={s.noticeContent}>
          {/* 전공필수 */}
          <div className={s.noticeSection}>
            <div className={s.noticeRow}>
              <div className={s.noticeSectionTitle}>전공필수</div>
              <p className={s.noticeTextInline}>
                아래 과목들은 모든 학생이 <b>반드시 이수해야 하는 전공필수</b>입니다.
              </p>
            </div>

            <div className={s.noticeChips}>
              {MAJOR_REQUIRED_COURSES.map((name) => {
                const taken = takenSet.has(normalize(name));
                return (
                  <span
                    key={name}
                    className={`${s.noticeChip} ${
                      taken ? s.noticeChipActive : ""
                    }`}
                  >
                    {name}
                  </span>
                );
              })}
            </div>
          </div>

          {/* 선택필수 */}
          <div className={s.noticeSection}>
            <div className={s.noticeRow}>
              <div className={s.noticeSectionTitle}>선택필수</div>
              <p className={s.noticeText}>
                아래 과목 중 최소 <b>2과목</b>을 이수해야 합니다.
                <span className={s.noticeBadgeSmall}>
                  현재 {electiveTakenCount}과목 이수
                </span>
              </p>
            </div>

            <div className={s.noticeChips}>
              {MAJOR_ELECTIVE_REQUIRED.map((name) => {
                const taken = takenSet.has(normalize(name));
                return (
                  <span
                    key={name}
                    className={`${s.noticeChip} ${
                      taken ? s.noticeChipActive : ""
                    }`}
                  >
                    {name}
                  </span>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
