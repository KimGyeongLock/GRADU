// src/pages/RegisterPage.tsx
import { useEffect, useMemo, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { axiosInstance } from "../lib/axios";

const HANDONG_DOMAIN = "handong.ac.kr";
const OTP_LEN = 6;
const RESEND_SEC = 300;

export default function RegisterPage() {
  const nav = useNavigate();

  const [emailLocal, setEmailLocal] = useState("");
  const emailFull = useMemo(
    () => (emailLocal ? `${emailLocal}@${HANDONG_DOMAIN}` : ""),
    [emailLocal]
  );

  const [password, setPassword] = useState("");
  const [pw2, setPw2] = useState("");

  // OTP
  const [otp, setOtp] = useState("");
  const [otpSentAt, setOtpSentAt] = useState<number | null>(null);
  const [showOtp, setShowOtp] = useState(false);

  // UI 상태
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [tick, setTick] = useState(0);
  useEffect(() => {
    if (!otpSentAt) return;
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, [otpSentAt]);

  const remain = useMemo(() => {
    if (!otpSentAt) return 0;
    const left = RESEND_SEC - Math.floor((Date.now() - otpSentAt) / 1000);
    return left > 0 ? left : 0;
  }, [otpSentAt, tick]);

  // ✓ OTP 전송
  async function sendOtp() {
    setErr(null);

    if (!emailFull) {
      setErr("학교 이메일을 입력하세요.");
      return;
    }

    setSending(true);
    try {
      await axiosInstance.post("/api/v1/auth/email/otp/send", { email: emailFull });
      setOtp("");
      setOtpSentAt(Date.now());
      setShowOtp(true);
    } catch (e: any) {
      setErr(e?.response?.data?.message || "인증코드 발송에 실패했습니다.");
    } finally {
      setSending(false);
    }
  }

  // ✓ 회원가입
  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);

    if (!emailFull) {
      setErr("학교 이메일을 입력하세요.");
      return;
    }
    if (password !== pw2) {
      setErr("비밀번호가 일치하지 않습니다.");
      return;
    }
    if (otp.length !== OTP_LEN) {
      setErr("인증코드 6자리를 입력하세요.");
      return;
    }

    try {
      setLoading(true);
      await axiosInstance.post("/api/v1/auth/register", {
        email: emailFull,
        password,
        code: otp,
      });

      alert("회원가입 완료! 로그인해주세요.");
      nav("/login", { replace: true });
    } catch (e: any) {
      const data = e?.response?.data;
      const fieldErr =
        data?.errors && typeof data.errors === "object"
          ? Object.values<string>(data.errors)[0]
          : null;

      setErr(fieldErr || data?.message || "회원가입 실패");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth">
      <form className="auth__card" onSubmit={onSubmit}>
        <h1 className="auth__title">Create Account</h1>
        <p className="auth__subtitle">회원가입</p>

        {err && <div className="auth__error">{err}</div>}

        {/* 학교 이메일 */}
        <div className="auth__emailRow">
          <label
            className="auth__field auth__emailField"
            style={{ gridTemplateColumns: "1fr" }}
          >
            <div style={{ display: "flex", width: "100%", gap: 8 }}>
              <input
                className="auth__input"
                placeholder="22500001"
                value={emailLocal}
                onChange={(e) => {
                  setEmailLocal(e.target.value.trim());
                  setOtp("");
                  setShowOtp(false);
                  setOtpSentAt(null);
                }}
                autoComplete="off"
                style={{ flex: 1 }}
              />
              <div className="auth__emailDomain">
                @handong.ac.kr
              </div>
            </div>
          </label>

          <button
            type="button"
            onClick={sendOtp}
            disabled={sending || remain > 0}
            className="auth__emailBtn"
          >
            {remain > 0 ? `인증하기 (${remain}s)` : "인증하기"}
          </button>
        </div>


        {/* OTP 입력 */}
        {showOtp && (
          <label className="auth__field" style={{ gridTemplateColumns: "1fr" }}>
            <input
              className="auth__input"
              placeholder="인증코드 6자리"
              value={otp}
              onChange={(e) =>
                setOtp(e.target.value.replace(/\D/g, "").slice(0, OTP_LEN))
              }
              inputMode="numeric"
              maxLength={OTP_LEN}
            />
          </label>
        )}

        {/* 비밀번호 */}
        <label className="auth__field">
          <input
            type="password"
            className="auth__input"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        {/* 비밀번호 확인 */}
        <label className="auth__field">
          <input
            type="password"
            className="auth__input"
            placeholder="비밀번호 확인"
            value={pw2}
            onChange={(e) => setPw2(e.target.value)}
          />
        </label>

        <button
          className="auth__button"
          disabled={loading || otp.length !== OTP_LEN}
        >
          {loading ? "가입 중..." : "회원가입"}
        </button>

        <div className="auth__footer">
          <span className="auth__muted">이미 계정이 있나요? </span>
          <Link className="auth__link" to="/login">로그인</Link>
        </div>
      </form>
    </main>
  );
}