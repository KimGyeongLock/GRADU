export type RankingItem = {
  rank: number;
  courseName: string;
  takenCount: number;
  delta: number;
};

export type MajorRanking = {
  y1s1: RankingItem[];
  y1s2: RankingItem[];
  y2s1: RankingItem[];
  y2s2: RankingItem[];
  y3s1: RankingItem[];
  y3s2: RankingItem[];
  y4s1: RankingItem[];
  y4s2: RankingItem[];
};

export type LiberalRanking = {
  faithWorldview: RankingItem[];
  generalEdu: RankingItem[];
  bsm: RankingItem[];
  freeElective: RankingItem[];
};

export type RankingData = {
  major: MajorRanking;
  liberal: LiberalRanking;
};

export type RankingCategory = "major" | "liberal";
