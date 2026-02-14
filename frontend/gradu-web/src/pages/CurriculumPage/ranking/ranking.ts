import { axiosInstance } from "../../../lib/axios";
import type { RankingData } from "./rankingTypes";

export async function fetchCourseRanking(): Promise<RankingData> {
  const { data } = await axiosInstance.get<RankingData>("/api/v1/rankings/courses");
  return data;
}
