import { useState } from "react";
import s from "../CurriculumDetail.module.css";

type Props = {
  takenSet: Set<string>;
  normalize: (v: string) => string;
};

const REQUIRED = [
  {
    group: "공동체리더십훈련",
    note: "(0.5학점 × 6 = 3학점)",
    items: [
      "공동체리더십훈련1",
      "공동체리더십훈련2",
      "공동체리더십훈련3",
      "공동체리더십훈련4",
      "공동체리더십훈련5",
      "공동체리더십훈련6",
    ],
  },
  {
    group: "사회봉사",
    note: "(총 2학점)",
    items: ["사회봉사1", "사회봉사2", "사회봉사3", "사회봉사4"],
  },
  {
    group: "한동인성교육",
    note: "(1학점)",
    items: ["한동인성교육"],
  },
] as const;

export function PersonalityInfoBox({ takenSet, normalize }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <div className={s.noticeBox}>
      <div className={s.noticeToggle} onClick={() => setOpen(v => !v)}>
        <span className={s.noticeTitle}>인성 및 리더십 이수 안내</span>
        <span className={s.noticeArrow}>{open ? "▲" : "▼"}</span>
      </div>

      {open && (
        <div className={s.noticeContent}>
          <div className={s.noticeSection}>
            <div className={s.noticeRow}>
              <div className={s.noticeSectionTitle}>필수</div>
              <p className={s.noticeTextInline}>
                아래 과목군은 <b>모두 이수</b>해야 합니다.
              </p>
            </div>

            {REQUIRED.map((g) => {
              const takenGroup = g.items.some(it =>
                takenSet.has(normalize(it))
              );

              return (
                <div
                  key={g.group}
                  className={`${s.faithGroup} ${takenGroup ? s.faithGroupActive : ""
                    }`}
                >
                  <div className={s.faithGroupHeader}>
                    <span
                      className={`${s.faithGroupTitle} ${takenGroup ? s.faithGroupTitleActive : ""
                        }`}
                    >
                      {g.group}
                    </span>
                    <span className={s.faithGroupNote}>{g.note}</span>
                  </div>

                  <div className={`${s.noticeChips} ${s.noticeChipsSpaced}`}>
                    {g.items.map((it) => {
                      const taken = takenSet.has(normalize(it));
                      return (
                        <span
                          key={it}
                          className={`${s.noticeChip} ${taken ? s.noticeChipActive : ""
                            }`}
                        >
                          {it}
                        </span>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
