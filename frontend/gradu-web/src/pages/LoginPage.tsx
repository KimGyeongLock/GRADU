import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import {
  axiosInstance,
  setAccessToken,
  setProfileName,   // ★ 추가
  setStudentId,     // ★ 추가 (토큰에 sid가 없을 때 대비)
} from "../lib/axios";

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

      // 토큰은 바디 또는 Authorization 헤더로 올 수 있음
      const headerAuth =
        headers?.authorization ||
        headers?.Authorization ||
        headers?.AUTHORIZATION;
      const tokenFromHeader =
        typeof headerAuth === "string"
          ? headerAuth.replace(/^Bearer\s+/i, "")
          : "";
      const token = data?.accessToken || data?.token || tokenFromHeader;
      if (!token) throw new Error("accessToken 없음");

      // ★ 토큰 저장(내부에서 name/sid 클레임도 파싱해서 저장 시도)
      setAccessToken(token);

      // ★ 응답 바디에 name / studentId 가 있으면 함께 저장(안오면 무시)
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
    <main className="mx-auto max-w-sm p-6 space-y-3">
      <h1 className="text-2xl font-semibold">로그인</h1>

      <input
        className="border p-2 w-full bg-white text-gray-900"
        placeholder="학번 (studentId)"
        value={studentId}
        onChange={(e) => setStudentIdInput(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && onLogin()}
      />

      <input
        className="border p-2 w-full bg-white text-gray-900"
        type="password"
        placeholder="비밀번호"
        value={pw}
        onChange={(e) => setPw(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && onLogin()}
      />

      <button
        className="border px-4 py-2 bg-blue-600 text-white disabled:opacity-60"
        onClick={onLogin}
        disabled={loading || !studentId || !pw}
      >
        {loading ? "로그인 중..." : "로그인"}
      </button>

      {err && <p className="text-red-600 text-sm">{err}</p>}

      <p className="text-sm">
        계정이 없나요? <Link className="underline" to="/register">회원가입</Link>
      </p>
    </main>
  );
}
