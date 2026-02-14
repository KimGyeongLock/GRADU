import { createContext, useContext, useMemo, useState, useCallback, type ReactNode } from "react";

type OverlayUI = {
  isRankingOpen: boolean;
  openRanking: () => void;
  closeRanking: () => void;
  toggleRanking: () => void;
};

const Ctx = createContext<OverlayUI | null>(null);

export function OverlayUIProvider({ children }: { children: ReactNode }) {
  const [isRankingOpen, setIsRankingOpen] = useState(false);

  const openRanking = useCallback(() => setIsRankingOpen(true), []);
  const closeRanking = useCallback(() => setIsRankingOpen(false), []);
  const toggleRanking = useCallback(() => setIsRankingOpen((v) => !v), []);

  const value = useMemo<OverlayUI>(
    () => ({
      isRankingOpen,
      openRanking,
      closeRanking,
      toggleRanking,
    }),
    [isRankingOpen, openRanking, closeRanking, toggleRanking]
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useOverlayUI() {
  const v = useContext(Ctx);
  if (!v) throw new Error("useOverlayUI must be used within OverlayUIProvider");
  return v;
}
