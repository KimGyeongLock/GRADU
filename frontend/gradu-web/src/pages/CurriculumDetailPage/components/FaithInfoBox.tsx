import { useState } from "react";
import s from "../CurriculumDetail.module.css";

type Props = {
    takenSet: Set<string>;
    normalize: (v: string) => string;
};

const REQUIRED = [
    {
        group: "기독교신앙의기초1",
        note: "(2학점 이상)",
        items: ["성경의 이해", "성경과 삶", "성경과 영적 성장"],
    },
    {
        group: "기독교신앙의기초2",
        note: "(2학점 이상)",
        items: ["기독교의 이해", "기독교와 비교종교", "기독교와 포스트모더니즘"],
    },
    {
        group: "세계관1",
        note: "(2학점 이상)  ※ 기독교세계관 권장",
        items: ["창조와 진화", "그리스도인과 선교", "기독교세계관"],
    },
] as const;

export function FaithInfoBox({ takenSet, normalize }: Props) {
    const [open, setOpen] = useState(false);

    return (
        <div className={s.noticeBox}>
            <div className={s.noticeToggle} onClick={() => setOpen((v) => !v)}>
                <span className={s.noticeTitle}>
                    신앙및세계관 이수 안내
                </span>
                <span className={s.noticeArrow}>{open ? "▲" : "▼"}</span>
            </div>

            {open && (
                <div className={s.noticeContent}>
                    {/* 필수 */}
                    <div className={s.noticeSection}>
                        <div className={s.noticeRow}>
                            <div className={s.noticeSectionTitle}>필수</div>
                            <p className={s.noticeTextInline}>
                                아래 과목군은 <b>반드시 이수</b>해야 합니다.
                            </p>
                        </div>

                        <div className={s.faithGroups}>
                            {REQUIRED.map((g) => {
                                const takenGroup = g.items.some((it) => takenSet.has(normalize(it)));

                                return (
                                    <div
                                        key={g.group}
                                        className={`${s.faithGroup} ${takenGroup ? s.faithGroupActive : ""}`}
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

                                        <div className={s.noticeChips}>
                                            {g.items.map((it) => {
                                                const takenItem = takenSet.has(normalize(it));
                                                return (
                                                    <span
                                                        key={it}
                                                        className={`${s.noticeChip} ${takenItem ? s.noticeChipActive : ""
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
                </div>
            )}
        </div>
    );
}
