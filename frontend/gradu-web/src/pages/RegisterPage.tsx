// src/pages/RegisterPage.tsx
import { useEffect, useMemo, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { axiosInstance } from "../lib/axios";

const HANDONG_DOMAIN = "handong.ac.kr";
const OTP_LEN = 6;
const RESEND_SEC = 60;

export default function RegisterPage() {
  const nav = useNavigate();

  const [studentId, setStudentId] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [pw2, setPw2] = useState("");

  // 이메일 앞부분(로컬파트) 입력
  const [emailLocal, setEmailLocal] = useState("");
  const emailFull = useMemo(
    () => (emailLocal ? `${emailLocal}@${HANDONG_DOMAIN}` : ""),
    [emailLocal]
  );

  // OTP
  const [otp, setOtp] = useState("");
  const [otpSentAt, setOtpSentAt] = useState<number | null>(null);
  const [showOtp, setShowOtp] = useState(false);

  // UI 상태
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 쿨다운용 틱
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

  // OTP 전송(검증은 회원가입에서만 수행/소비)
  async function sendOtp() {
    setErr(null);
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

  // 회원가입(백엔드에서 OTP 검증 + 소비 + 회원생성 일괄 처리)
  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);

    if (!studentId || !name || !password) {
      setErr("학번, 이름, 비밀번호를 모두 입력하세요.");
      return;
    }
    if (password !== pw2) {
      setErr("비밀번호가 일치하지 않습니다.");
      return;
    }
    if (otp.length !== OTP_LEN) {
      setErr("인증코드 6자리를 정확히 입력하세요.");
      return;
    }

    try {
      setLoading(true);
      await axiosInstance.post("/api/v1/auth/register", {
        studentId,
        name,
        password,
        code: otp, 
        email: emailFull
      });
      alert("회원가입이 완료되었습니다. 로그인 해주세요.");
      nav("/login", { replace: true });
    } catch (e: any) {
      setErr(e?.response?.data?.message || "회원가입에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth">
      <form className="auth__card" onSubmit={onSubmit}>
        <h1 className="auth__title">Create Account</h1>
        <p className="auth__subtitle">회원가입</p>

        {err && <div className="auth__error">{err}</div>}

        {/* 학번 */}
        <label className="auth__field" style={{ gridTemplateColumns: "1fr" }}>
          <input
            className="auth__input"
            placeholder="학번"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value.trim())}
            autoComplete="off"
          />
        </label>

        {/* 이름 */}
        <label className="auth__field" style={{ gridTemplateColumns: "1fr" }}>
          <input
            className="auth__input"
            placeholder="이름"
            value={name}
            onChange={(e) => setName(e.target.value.trim())}
            autoComplete="off"
          />
        </label>

        {/* 학교 이메일 + '인증하기' (전송 전용) */}
        <div className="auth__emailRow">
          <label
            className="auth__field"
            style={{ gridTemplateColumns: "1fr", flex: 1, margin: 0 }}
          >
            <div style={{ display: "flex", width: "100%", gap: 8 }}>
              <input
                className="auth__input"
                placeholder="22500001"
                value={emailLocal}
                onChange={(e) => {
                  setEmailLocal(e.target.value.trim());
                  setOtp(""); // 이메일이 바뀌면 코드 초기화
                  setShowOtp(false);
                  setOtpSentAt(null);
                }}
                autoComplete="off"
                style={{ flex: 1 }}
              />
              <div
                style={{
                  alignSelf: "center",
                  fontSize: 14,
                  color: "var(--muted)",
                  whiteSpace: "nowrap",
                }}
              >
                @handong.ac.kr
              </div>
            </div>
          </label>

          <button
            type="button"
            onClick={sendOtp}
            disabled={sending || remain > 0}
            className="auth__emailBtn"
            aria-label="이메일 인증 요청"
            title="이메일로 인증코드를 받습니다"
          >
            {remain > 0 ? `인증하기 (${remain}s)` : "인증하기"}
          </button>
        </div>

        {/* 인증코드 입력(전송 성공 후 노출) */}
        {showOtp && (
          <div>
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
          </div>
        )}

        {/* 비밀번호 */}
        <label className="auth__field" style={{ gridTemplateColumns: "1fr" }}>
          <input
            type="password"
            className="auth__input"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        {/* 비밀번호 확인 */}
        <label className="auth__field" style={{ gridTemplateColumns: "1fr" }}>
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
          <Link className="auth__link" to="/login">
            로그인
          </Link>
        </div>
      </form>
    </div>
  );
}
