import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { axiosInstance /*, setAccessToken, setProfileName, setStudentId */ } from "../lib/axios";

export default function RegisterPage() {
  const nav = useNavigate();
  const [studentId, setStudentId] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [pw2, setPw2] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(null);

    if (!studentId || !name || !password) {
      return setErr("학번, 이름, 비밀번호를 모두 입력하세요.");
    }
    if (password !== pw2) {
      return setErr("비밀번호가 일치하지 않습니다.");
    }

    try {
      setLoading(true);

      // ★ 백엔드가 받는 필드명과 맞춰주세요
      const res = await axiosInstance.post("/api/v1/auth/register", {
        studentId,
        name,
        password,
      });

      // --- 만약 서버가 가입 직후 토큰을 준다면 자동 로그인 처리도 가능 ---
      // const { accessToken, name: n, studentId: sid } = res.data ?? {};
      // if (accessToken) setAccessToken(accessToken);
      // if (n) setProfileName(String(n));
      // if (sid) setStudentId(String(sid));
      // nav("/curriculum", { replace: true }); return;

      alert("회원가입이 완료되었습니다. 로그인 해주세요.");
      nav("/login", { replace: true });
    } catch (e: any) {
      setErr(e?.response?.data?.message || "회원가입에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <form onSubmit={onSubmit} className="w-[360px] bg-white rounded-2xl shadow p-6 space-y-4">
        <h1 className="text-2xl font-semibold text-gray-900">회원가입</h1>

        {err && <p className="text-sm text-red-600">{err}</p>}

        <div className="space-y-1">
          <label className="text-sm text-gray-700">학번</label>
          <input
            className="w-full border rounded-lg p-2 bg-white text-gray-900"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            autoFocus
          />
        </div>

        <div className="space-y-1">
          <label className="text-sm text-gray-700">이름</label>
          <input
            className="w-full border rounded-lg p-2 bg-white text-gray-900"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        <div className="space-y-1">
          <label className="text-sm text-gray-700">비밀번호</label>
          <input
            type="password"
            className="w-full border rounded-lg p-2 bg-white text-gray-900"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        <div className="space-y-1">
          <label className="text-sm text-gray-700">비밀번호 확인</label>
          <input
            type="password"
            className="w-full border rounded-lg p-2 bg-white text-gray-900"
            value={pw2}
            onChange={(e) => setPw2(e.target.value)}
          />
        </div>

        <button
          disabled={loading}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white rounded-lg py-2 disabled:opacity-60"
        >
          {loading ? "가입 중..." : "회원가입"}
        </button>

        <div className="text-sm text-gray-600 text-center">
          이미 계정이 있나요?{" "}
          <Link to="/login" className="text-blue-600 hover:underline">로그인</Link>
        </div>
      </form>
    </div>
  );
}
