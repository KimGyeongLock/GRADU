import { useMemo, useState } from "react";
import s from "./RankingCard.module.css";
import { mockRanking } from "./mockRanking";
import type { RankingCategory, RankingItem } from "./rankingTypes";

type Props = {
  initialCategory?: RankingCategory;
};

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
  // Top3 강조 + 나머지는 숫자 원
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

export default function RankingCard({ initialCategory = "liberal" }: Props) {
  const [cat, setCat] = useState<RankingCategory>(initialCategory);

  const items: RankingItem[] = useMemo(() => {
    const list = cat === "major" ? mockRanking.major : mockRanking.liberal;
    return list.slice(0, 10);
  }, [cat]);

  return (
    <section className={s.root} aria-label="과목 랭킹 리스트">
      {/* ✅ 탭만 (상위 헤더는 바깥에서 이미 있음) */}
      <div className={s.tabs} role="tablist" aria-label="랭킹 탭">
        <button
          type="button"
          className={`${s.tab} ${cat === "major" ? s.tabActive : ""}`}
          onClick={() => setCat("major")}
          role="tab"
          aria-selected={cat === "major"}
        >
          <HatIcon active={cat === "major"} />
          <span>전공</span>
        </button>

        <button
          type="button"
          className={`${s.tab} ${cat === "liberal" ? s.tabActive : ""}`}
          onClick={() => setCat("liberal")}
          role="tab"
          aria-selected={cat === "liberal"}
        >
          <BookIcon active={cat === "liberal"} />
          <span>교양</span>
        </button>
      </div>

      <div className={s.list} role="list">
        {items.map((it) => {
          const isTop3 = it.rank <= 3;
          return (
            <div
              key={`${cat}-${it.rank}-${it.courseName}`}
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
                {/* Top3만 뱃지 아이콘 느낌 */}
                {isTop3 ? <span className={s.topBadge} aria-hidden="true">⟡</span> : null}
              </div>
            </div>
          );
        })}
      </div>

      <div className={s.footer}>
        <span className={s.bulb} aria-hidden="true">💡</span>
        <span className={s.footerText}>
          랭킹은 사용자들이 입력한 과목 데이터를 기반으로 집계됩니다.
        </span>
      </div>
    </section>
  );
}
