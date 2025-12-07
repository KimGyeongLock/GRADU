// src/components/AppShell.tsx
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useNavigate, Link } from "react-router-dom";
import { logoutApi } from "../lib/axios";
import Footer from "./Footer";

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
            <button
              ref={btnRef}
              onClick={() => setOpen((v) => !v)}
              className="accountBtn"
            >
              <span className="accountIcon" aria-hidden>
                ⚙️
              </span>
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
