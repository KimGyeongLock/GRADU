// src/pages/ResetPasswordPage.tsx
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { axiosInstance } from "../lib/axios";
import "../styles/auth.css";

const HANDONG_DOMAIN = "handong.ac.kr";
const OTP_LEN = 6;
const RESEND_SEC = 300; // 5분 쿨다운

export default function ResetPasswordPage() {
  const nav = useNavigate();

  const [studentId, setStudentId] = useState("");
  const [emailLocal, setEmailLocal] = useState("");
  const emailFull = useMemo(
    () => (emailLocal ? `${emailLocal}@${HANDONG_DOMAIN}` : ""),
    [emailLocal]
  );

  const [otp, setOtp] = useState("");
  const [otpSentAt, setOtpSentAt] = useState<number | null>(null);
  const [showOtp, setShowOtp] = useState(false);

  const [newPw, setNewPw] = useState("");
  const [newPw2, setNewPw2] = useState("");

  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 쿨다운 타이머
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

  // 1) OTP 전송
  async function sendOtp() {
    setErr(null);

    if (!studentId || !emailLocal) {
      setErr("학번과 학교 이메일을 먼저 입력하세요.");
      return;
    }

    setSending(true);
    try {
      await axiosInstance.post("/api/v1/auth/email/otp/send", {
        email: emailFull,
      });
      setOtp("");
      setOtpSentAt(Date.now());
      setShowOtp(true);
    } catch (e: any) {
      setErr(e?.response?.data?.message || "인증코드 발송에 실패했습니다.");
    } finally {
      setSending(false);
    }
  }

  // 2) 비밀번호 재설정
  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);

    if (!studentId || !emailLocal) {
      setErr("학번과 학교 이메일을 입력하세요.");
      return;
    }
    if (otp.length !== OTP_LEN) {
      setErr("인증코드 6자리를 정확히 입력하세요.");
      return;
    }
    if (!newPw) {
      setErr("새 비밀번호를 입력하세요.");
      return;
    }
    if (newPw !== newPw2) {
      setErr("비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      setLoading(true);
      await axiosInstance.post("/api/v1/auth/password/reset", {
        studentId,
        email: emailFull,
        code: otp,
        newPassword: newPw,
      });
      alert("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
      nav("/login", { replace: true });
    } catch (e: any) {
      const data = e?.response?.data;
      const firstFieldError =
        data?.errors && typeof data.errors === "object"
          ? (Object.values<string>(data.errors)[0] as string)
          : null;
      const fallbackMessage = data?.message || "비밀번호 재설정에 실패했습니다.";
      setErr(firstFieldError || fallbackMessage);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth">
      <div className="auth__layout">
        <img src="/gradu_text.png" alt="GRADU" className="auth__logo" />

        <form className="auth__card" onSubmit={onSubmit}>
          <h1 className="auth__title">Reset Password</h1>
          <p className="auth__subtitle">비밀번호 재설정</p>

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

          {/* 학교 이메일 + OTP 버튼 */}
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
                    setOtp("");
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
            >
              {remain > 0 ? `인증코드 (${remain}s)` : "코드 발송"}
            </button>
          </div>

          {/* 인증코드 입력 */}
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

          {/* 새 비밀번호 */}
          <label className="auth__field" style={{ gridTemplateColumns: "1fr" }}>
            <input
              type="password"
              className="auth__input"
              placeholder="새 비밀번호"
              value={newPw}
              onChange={(e) => setNewPw(e.target.value)}
            />
          </label>

          {/* 새 비밀번호 확인 */}
          <label className="auth__field" style={{ gridTemplateColumns: "1fr" }}>
            <input
              type="password"
              className="auth__input"
              placeholder="새 비밀번호 확인"
              value={newPw2}
              onChange={(e) => setNewPw2(e.target.value)}
            />
          </label>

          <button
            className="auth__button"
            disabled={
              loading ||
              !studentId ||
              !emailLocal ||
              otp.length !== OTP_LEN ||
              !newPw ||
              !newPw2
            }
          >
            {loading ? "변경 중..." : "비밀번호 변경"}
          </button>

          <div className="auth__footer">
            <span className="auth__muted">비밀번호가 기억나셨나요? </span>
            <Link className="auth__link" to="/login">
              로그인
            </Link>
          </div>
        </form>
      </div>
    </main>
  );
}
