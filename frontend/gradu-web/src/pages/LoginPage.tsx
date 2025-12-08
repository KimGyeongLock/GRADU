

// src/pages/LoginPage.tsx
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { axiosInstance, setAccessToken } from "../lib/axios";
import { setGuestMode } from "../lib/auth";
import "../styles/auth.css";

const HANDONG_DOMAIN = "@handong.ac.kr";

export default function LoginPage() {
  const [emailLocal, setEmailLocal] = useState("");
  const [pw, setPw] = useState("");
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);
  const nav = useNavigate();

  const onLogin = async () => {
    if (!emailLocal || !pw) {
      setErr("이메일과 비밀번호를 입력하세요.");
      return;
    }
    setErr("");
    setLoading(true);

    try {
      const fullEmail = `${emailLocal}${HANDONG_DOMAIN}`;

      const { data } = await axiosInstance.post(
        "/api/v1/auth/login",
        { email: fullEmail, password: pw },
        { withCredentials: true }
      );

      const token = data?.accessToken;
      if (!token) throw new Error("accessToken 없음");

      setGuestMode(false);

      setAccessToken(token);
      nav("/", { replace: true });
    } catch (e: any) {
      setErr(e?.response?.data?.message || "로그인 실패");
    } finally {
      setLoading(false);
    }
  };

  const onGuestLogin = async () => {
    setErr("");
    setLoading(true);
    try {
      setGuestMode(true);

      nav("/curriculum", { replace: true });
    } catch (e: any) {
      setErr("비회원 로그인을 진행할 수 없습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth">
      <div className="auth__layout">
        <img src="/gradu_text.png" alt="GRADU" className="auth__logo" />

        <section className="auth__card">
          <h1 className="auth__title">Sign In</h1>
          <p className="auth__subtitle">
            한동대학교 컴공심화 이수 관리 및 졸업 설계 서비스
          </p>

          {/* 이메일 입력 */}
          <div className="auth__field auth__field--email">
            <input
              id="emailInput"
              className="auth__input auth__input--email"
              placeholder="이메일 주소"
              value={emailLocal}
              onChange={(e) =>
                setEmailLocal(
                  e.target.value
                    .replace(/\s+/g, "") // 공백 제거
                    .replace(/@.*/, "") // 사용자가 도메인까지 쳐도 앞부분만 유지
                )
              }
              onKeyDown={(e) => e.key === "Enter" && onLogin()}
              autoComplete="email"
              autoFocus
            />

            <span
              className="auth__emailDomain"
              onClick={() => document.getElementById("emailInput")?.focus()}
            >
              {HANDONG_DOMAIN}
            </span>
          </div>

          {/* 비밀번호 입력 */}
          <div className="auth__field">
            <input
              className="auth__input"
              type="password"
              placeholder="비밀번호"
              value={pw}
              onChange={(e) => setPw(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && onLogin()}
              autoComplete="current-password"
            />
          </div>

          {err && <div className="auth__error">{err}</div>}

          <div className="auth__buttonRow">
            <button
              className="auth__button auth__button--primary"
              onClick={onLogin}
              disabled={loading || !emailLocal || !pw}
            >
              {loading ? "로그인 중..." : "로그인"}
            </button>

            <button
              className="auth__button auth__button--guest"
              type="button"
              onClick={onGuestLogin}
              disabled={loading}
            >
              비회원 로그인
            </button>
          </div>


          <div className="auth__footer auth__muted">
            <span>계정이 없나요? </span>
            <Link className="auth__link" to="/register">
              회원가입
            </Link>

            <span className="auth__separator">|</span>

            <span>비밀번호를 잊으셨나요? </span>
            <Link className="auth__link" to="/reset-password">
              재설정
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
}

