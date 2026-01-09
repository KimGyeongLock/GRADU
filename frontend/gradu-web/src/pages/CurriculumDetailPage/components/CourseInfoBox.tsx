import { useState } from "react";
import s from "../CurriculumDetail.module.css";

type CourseInfoBoxProps = {
  title: string;
  description?: React.ReactNode;
  courses: string[];
  takenSet: Set<string>;
  normalize: (s: string) => string;
  defaultOpen?: boolean;

  resolveDisplayName?: (name: string) => string;
  isTaken?: (name: string) => boolean;
};

export function CourseInfoBox({
  title,
  description,
  courses,
  takenSet,
  normalize,
  defaultOpen = false,
  resolveDisplayName,
  isTaken,
}: CourseInfoBoxProps) {
  const [open, setOpen] = useState(defaultOpen);

  const fallbackIsTaken = (name: string) => takenSet.has(normalize(name));
  const getIsTaken = isTaken ?? fallbackIsTaken;
  const getDisplayName = resolveDisplayName ?? ((x: string) => x);

  return (
    <div className={s.noticeBox}>
      <div className={s.noticeToggle} onClick={() => setOpen((v) => !v)}>
        <span className={s.noticeTitle}>{title}</span>
        <span className={s.noticeArrow}>{open ? "▲" : "▼"}</span>
      </div>

      {open && (
        <div className={s.noticeContent}>
          {description && <p className={s.noticeText}>{description}</p>}

          <div className={s.noticeChips}>
            {courses.map((name) => {
              const taken = getIsTaken(name);
              const displayName = getDisplayName(name);

              return (
                <span
                  key={name}
                  className={`${s.noticeChip} ${
                    taken ? s.noticeChipActive : ""
                  }`}
                  title={displayName !== name ? `${name} → ${displayName}` : name}
                >
                  {displayName}
                </span>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
