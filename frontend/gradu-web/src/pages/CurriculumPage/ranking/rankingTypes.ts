export type RankingCategory = "major" | "liberal";

/**
 * rank: 1~10
 * takenCount: 수강 인원(집계값)
 * delta: 순위 변동(+, -, 0)
 */
export type RankingItem = {
  rank: number;
  courseName: string;
  takenCount: number;
  delta: number;
};

export type RankingData = {
  major: RankingItem[];
  liberal: RankingItem[];
};
