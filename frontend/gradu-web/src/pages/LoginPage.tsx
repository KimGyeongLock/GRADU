import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import {
  axiosInstance,
  setAccessToken,
  setProfileName,
  setStudentId,
} from "../lib/axios";
import "../styles/auth.css";

export default function LoginPage() {
  const [studentId, setStudentIdInput] = useState("");
  const [pw, setPw] = useState("");
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);
  const nav = useNavigate();

  const onLogin = async () => {
    if (!studentId || !pw) {
      setErr("학번과 비밀번호를 입력하세요.");
      return;
    }
    setErr("");
    setLoading(true);
    try {
      const { data, headers } = await axiosInstance.post(
        "/api/v1/auth/login",
        { studentId, password: pw },
        { withCredentials: true }
      );

      const headerAuth =
        (headers as any)?.authorization ||
        (headers as any)?.Authorization ||
        (headers as any)?.AUTHORIZATION;
      const tokenFromHeader =
        typeof headerAuth === "string" ? headerAuth.replace(/^Bearer\s+/i, "") : "";
      const token = data?.accessToken || data?.token || tokenFromHeader;
      if (!token) throw new Error("accessToken 없음");

      setAccessToken(token);
      if (data?.name) setProfileName(String(data.name));
      if (data?.studentId) setStudentId(String(data.studentId));

      nav("/", { replace: true });
    } catch (e: any) {
      setErr(e?.response?.data?.message || "로그인 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth">
      <div className="auth__layout">
        {/* 왼쪽: 로고 이미지 */}
        <img
          src="/gradu_text.png"
          alt="GRADU"
          className="auth__logo"
        />
        {/* 오른쪽: 기존 카드 그대로 */}
        <section className="auth__card" aria-label="로그인">
          <h1 className="auth__title">Sign In</h1>
          <p className="auth__subtitle">한동대학교 컴공심화 졸업 요건 진단 서비스</p>

          <div className="auth__field">
            <i className="bx bx-id-card auth__icon" aria-hidden />
            <input
              className="auth__input"
              placeholder="학번 (Student ID)"
              value={studentId}
              onChange={(e) => setStudentIdInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && onLogin()}
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="auth__field">
            <i className="bx bx-lock-alt auth__icon" aria-hidden />
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

          <button
            className="auth__button"
            onClick={onLogin}
            disabled={loading || !studentId || !pw}
          >
            {loading ? "로그인 중..." : "로그인"}
          </button>

          <div className="auth__footer auth__muted">
            계정이 없나요?{" "}
            <Link className="auth__link" to="/register">
              회원가입
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
}
