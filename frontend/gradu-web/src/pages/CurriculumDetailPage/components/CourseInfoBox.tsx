import { useState } from "react";
import s from "../CurriculumDetail.module.css";

type CourseInfoBoxProps = {
  title: string;
  description?: React.ReactNode;
  courses: string[];
  takenSet: Set<string>;
  normalize: (s: string) => string;
  defaultOpen?: boolean;
};

export function CourseInfoBox({
  title,
  description,
  courses,
  takenSet,
  normalize,
  defaultOpen = false,
}: CourseInfoBoxProps) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className={s.noticeBox}>
      <div
        className={s.noticeToggle}
        onClick={() => setOpen((v) => !v)}
      >
        <span className={s.noticeTitle}>{title}</span>
        <span className={s.noticeArrow}>{open ? "▲" : "▼"}</span>
      </div>

      {open && (
        <div className={s.noticeContent}>
          {description && (
            <p className={s.noticeText}>{description}</p>
          )}

          <div className={s.noticeChips}>
            {courses.map((name) => {
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
      )}
    </div>
  );
}
