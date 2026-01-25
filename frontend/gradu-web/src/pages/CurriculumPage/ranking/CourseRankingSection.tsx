import { useQuery } from "@tanstack/react-query";
import RankingCard from "./RankingCard";
import type { RankingData } from "./rankingTypes";
import { fetchCourseRanking } from "./ranking";

export default function CourseRankingSection() {
  const { data, isLoading, isError, error } = useQuery<RankingData>({
    queryKey: ["course-ranking"],
    queryFn: () => fetchCourseRanking(),
    refetchOnWindowFocus: false,
    staleTime: 30_000, // 30초 정도는 캐시로 버팀(임시)
  });

  if (isLoading) return <div style={{ padding: 12 }}>랭킹 불러오는 중…</div>;
  if (isError) {
    return (
      <div style={{ padding: 12 }}>
        랭킹 로딩 실패: {(error as Error)?.message ?? "unknown error"}
      </div>
    );
  }
  if (!data) return null;

  return <RankingCard data={data} initialCategory="major" />;
}
