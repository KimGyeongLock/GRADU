import { useEffect, useRef, useState, type ReactNode } from "react";
import { useNavigate, Link } from "react-router-dom";
import { getDisplayName } from "../lib/displayName";
import { logoutApi } from "../lib/axios";
import Footer from "./Footer";

import "./AppShell.css";

export default function AppShell({ children }: { children: ReactNode }) {
  const display = getDisplayName();
  const [open, setOpen] = useState(false);
  const btnRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const nav = useNavigate();

  // 바깥 클릭 시 드롭다운 닫기
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
              onClick={() => setOpen(v => !v)}
              className="accountBtn"
            >
              <span aria-hidden className="accountIcon">👤</span>
              <span className="accountName">{display}</span>
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
