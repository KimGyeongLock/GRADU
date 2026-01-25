import { useMemo, useRef, useState } from "react";
import s from "./RankingCard.module.css";
import type { RankingCategory, RankingData, RankingItem } from "./rankingTypes";

type Props = {
  initialCategory?: RankingCategory;
  data: RankingData;
};

type LiberalSubTab = keyof RankingData["liberal"];
type MajorSubTab = keyof RankingData["major"];

function formatCount(n: number) {
  return n.toLocaleString("ko-KR");
}

function Delta({ delta }: { delta: number }) {
  if (delta === 0) return <span className={s.deltaNeutral}>–</span>;
  if (delta > 0)
    return (
      <span className={s.deltaUp}>
        ▲ <span className={s.deltaNum}>{delta}</span>
      </span>
    );
  return (
    <span className={s.deltaDown}>
      ▼ <span className={s.deltaNum}>{Math.abs(delta)}</span>
    </span>
  );
}

function Medal({ rank }: { rank: number }) {
  if (rank === 1) return <span className={`${s.medal} ${s.medal1}`}>1</span>;
  if (rank === 2) return <span className={`${s.medal} ${s.medal2}`}>2</span>;
  if (rank === 3) return <span className={`${s.medal} ${s.medal3}`}>3</span>;
  return (
    <span className={s.rankCircle}>
      <span className={s.rankCircleNum}>{rank}</span>
    </span>
  );
}

function HatIcon({ active }: { active: boolean }) {
  return (
    <svg
      className={s.tabIcon}
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      style={{ opacity: active ? 1 : 0.7 }}
    >
      <path
        d="M12 3 2 8l10 5 10-5-10-5Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path
        d="M6 10v5c0 1.7 2.7 3 6 3s6-1.3 6-3v-5"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function BookIcon({ active }: { active: boolean }) {
  return (
    <svg
      className={s.tabIcon}
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      style={{ opacity: active ? 1 : 0.7 }}
    >
      <path
        d="M4.5 5.5c1.5-1 3.5-1 5 0l1 .7 1-.7c1.5-1 3.5-1 5 0V20c-1.5-1-3.5-1-5 0l-1 .7-1-.7c-1.5-1-3.5-1-5 0V5.5Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path
        d="M10.5 6.2V20.7"
        stroke="currentColor"
        strokeWidth="1.2"
        strokeLinejoin="round"
        opacity="0.5"
      />
    </svg>
  );
}

const LIBERAL_TABS: { key: LiberalSubTab; label: string }[] = [
  { key: "faithWorldview", label: "신앙·세계관" },
  { key: "generalEdu", label: "전문교양" },
  { key: "bsm", label: "BSM" },
  { key: "freeElective", label: "자유선택" },
];

const MAJOR_TABS: { key: MajorSubTab; label: string }[] = [
  { key: "y1s2", label: "1-2" },
  { key: "y2s1", label: "2-1" },
  { key: "y2s2", label: "2-2" },
  { key: "y3s1", label: "3-1" },
  { key: "y3s2", label: "3-2" },
  { key: "y4s1", label: "4-1" },
  { key: "y4s2", label: "4-2" },
];

export default function RankingCard({ initialCategory = "liberal", data }: Props) {
  const [cat, setCat] = useState<RankingCategory>(initialCategory);

  const [libTab, setLibTab] = useState<LiberalSubTab>("faithWorldview");
  const [majorTab, setMajorTab] = useState<MajorSubTab>("y1s2");

  const onChangeCat = (next: RankingCategory) => {
    setCat(next);
    if (next === "liberal") setLibTab("faithWorldview");
    if (next === "major") setMajorTab("y1s2");
  };

  const items: RankingItem[] = useMemo(() => {
    if (cat === "major") return (data.major?.[majorTab] ?? []).slice(0, 10);
    return (data.liberal?.[libTab] ?? []).slice(0, 10);
  }, [cat, data, libTab, majorTab]);

  const majorTabsRef = useRef<HTMLDivElement | null>(null);
  const drag = useRef({ isDown: false, startX: 0, startLeft: 0 });

  const onMajorMouseDown = (e: React.MouseEvent) => {
    const el = majorTabsRef.current;
    if (!el) return;
    drag.current.isDown = true;
    drag.current.startX = e.clientX;
    drag.current.startLeft = el.scrollLeft;
    el.classList.add(s.dragging);
  };

  const onMajorMouseMove = (e: React.MouseEvent) => {
    const el = majorTabsRef.current;
    if (!el || !drag.current.isDown) return;
    const dx = e.clientX - drag.current.startX;
    el.scrollLeft = drag.current.startLeft - dx;
  };

  const endDrag = () => {
    const el = majorTabsRef.current;
    drag.current.isDown = false;
    el?.classList.remove(s.dragging);
  };


  return (
    <section className={s.root} aria-label="과목 랭킹 리스트">
      {/* 상위 탭 */}
      <div className={s.tabs} role="tablist" aria-label="랭킹 탭">
        <button
          type="button"
          className={`${s.tab} ${cat === "major" ? s.tabActive : ""}`}
          onClick={() => onChangeCat("major")}
          role="tab"
          aria-selected={cat === "major"}
        >
          <HatIcon active={cat === "major"} />
          <span>전공</span>
        </button>

        <button
          type="button"
          className={`${s.tab} ${cat === "liberal" ? s.tabActive : ""}`}
          onClick={() => onChangeCat("liberal")}
          role="tab"
          aria-selected={cat === "liberal"}
        >
          <BookIcon active={cat === "liberal"} />
          <span>교양</span>
        </button>
      </div>

      {/* 전공 하위 탭 */}
      {cat === "major" && (
        <div
          ref={majorTabsRef}
          className={`${s.subTabs} ${s.majorSubTabs}`}
          role="tablist"
          aria-label="전공 하위 탭"
          onMouseDown={onMajorMouseDown}
          onMouseMove={onMajorMouseMove}
          onMouseUp={endDrag}
          onMouseLeave={endDrag}
        >
          {MAJOR_TABS.map((t) => (
            <button
              key={t.key}
              type="button"
              className={`${s.tab} ${s.subTab} ${majorTab === t.key ? s.tabActive : ""}`}
              onClick={() => setMajorTab(t.key)}
              role="tab"
              aria-selected={majorTab === t.key}
            >
              <span>{t.label}</span>
            </button>
          ))}
        </div>
      )}

      {/* 교양 하위 탭 */}
      {cat === "liberal" && (
        <div className={s.subTabs} role="tablist" aria-label="교양 하위 탭">
          {LIBERAL_TABS.map((t) => (
            <button
              key={t.key}
              type="button"
              className={`${s.tab} ${s.subTab} ${libTab === t.key ? s.tabActive : ""}`}
              onClick={() => setLibTab(t.key)}
              role="tab"
              aria-selected={libTab === t.key}
            >
              <span>{t.label}</span>
            </button>
          ))}
        </div>
      )}

      {/* 랭킹 리스트 */}
      <div className={s.list} role="list">
        {items.map((it) => {
          const isTop3 = it.rank <= 3;
          return (
            <div
              key={`${cat}-${cat === "major" ? majorTab : libTab}-${it.rank}-${it.courseName}`}
              className={`${s.row} ${isTop3 ? s.rowTop3 : ""}`}
              role="listitem"
            >
              <div className={s.left}>
                <Medal rank={it.rank} />
              </div>

              <div className={s.mid}>
                <div className={s.nameLine}>
                  <div className={s.courseName}>{it.courseName}</div>
                  <div className={s.delta}>
                    <Delta delta={it.delta} />
                  </div>
                </div>

                <div className={s.count}>
                  <span className={s.countNum}>{formatCount(it.takenCount)}</span>
                  <span className={s.countUnit}>명 수강</span>
                </div>
              </div>

              <div className={s.right}>
                {isTop3 ? <span className={s.topBadge} aria-hidden="true">⟡</span> : null}
              </div>
            </div>
          );
        })}
      </div>

      <div className={s.footer}>
        <span className={s.bulb} aria-hidden="true">💡</span>
        <span className={s.footerText}>랭킹은 사용자들이 입력한 과목 데이터를 기반으로 집계됩니다.</span>
      </div>
    </section>
  );
}
