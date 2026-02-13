// src/components/AppShell.tsx
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
import { logoutApi } from "../lib/axios";
import Footer from "./Footer";
import { useOverlayUI } from "../ui/OverlayUIContext";

import "./AppShell.css";

export default function AppShell({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const btnRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const nav = useNavigate();

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      const t = e.target as Node;
      if (!btnRef.current?.contains(t) && !menuRef.current?.contains(t)) {
        setOpen(false);
      }
    };
    document.addEventListener("click", onClick);
    return () => document.removeEventListener("click", onClick);
  }, []);

  const onLogout = async () => {
    try {
      await logoutApi();
    } finally {
      nav("/login", { replace: true });
    }
  };

  const loc = useLocation();
  const { isRankingOpen, toggleRanking, closeRanking } = useOverlayUI();

  // 커리큘럼 페이지에서만 보이게 (경로는 너 프로젝트에 맞춰 수정)
  const showRankingBtn = loc.pathname === "/" || loc.pathname.startsWith("/curriculum");

  // 페이지 이동 시 랭킹 자동 닫기(선택)
  useEffect(() => {
    closeRanking();
  }, [loc.pathname, closeRanking]);

  return (
    <div className="appShell">
      <header className="appHeader">
        <div className="appHeaderInner">
          <h1 className="appTitle">
            <Link to="/" className="appTitleLink" aria-label="홈으로 이동">
              GRADU
            </Link>
          </h1>

          <div className="account">
            {showRankingBtn && (
              <button
                type="button"
                onClick={toggleRanking}
                className={`rankingBtn ${isRankingOpen ? "rankingBtnActive" : ""}`}
                aria-pressed={isRankingOpen}
              >
                <span className="rankingIcon" aria-hidden>🏅</span>
                <span className="rankingBtnText">과목 랭킹</span>
              </button>
            )}

            <button
              ref={btnRef}
              onClick={() => setOpen((v) => !v)}
              className="accountBtn"
            >
              <span className="accountIcon" aria-hidden>⚙️</span>
              <span className="accountName">설정</span>
            </button>

            {open && (
              <div ref={menuRef} className="accountMenu">
                <button onClick={onLogout} className="accountMenuItem">
                  로그아웃
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="appMain">
        <div className="appContent">{children}</div>
      </main>
      <Footer />
    </div>
  );
}
