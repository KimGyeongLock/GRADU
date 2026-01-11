import { createContext, useContext, useMemo, useState, type ReactNode } from "react";

type OverlayUI = {
  isRankingOpen: boolean;
  openRanking: () => void;
  closeRanking: () => void;
  toggleRanking: () => void;
};

const Ctx = createContext<OverlayUI | null>(null);

export function OverlayUIProvider({ children }: { children: ReactNode }) {
  const [isRankingOpen, setIsRankingOpen] = useState(false);

  const value = useMemo<OverlayUI>(
    () => ({
      isRankingOpen,
      openRanking: () => setIsRankingOpen(true),
      closeRanking: () => setIsRankingOpen(false),
      toggleRanking: () => setIsRankingOpen((v) => !v),
    }),
    [isRankingOpen]
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useOverlayUI() {
  const v = useContext(Ctx);
  if (!v) throw new Error("useOverlayUI must be used within OverlayUIProvider");
  return v;
}
