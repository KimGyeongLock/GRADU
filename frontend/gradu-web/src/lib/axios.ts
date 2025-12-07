// src/lib/axios.ts
import axios from "axios";

export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
});

// Access Token key
const ACCESS_KEY = "access_token";

// ======================================================
//  JWT payload decode (base64url → json)
// ======================================================
function parseJwt(token: string): any {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

// ======================================================
//  Access Token 저장 / 삭제
//  → 저장 시 토큰에서 userId 자동 추출
// ======================================================
let cachedUserId: string | null = null;

export function setAccessToken(token: string | null) {
  if (token) {
    localStorage.setItem(ACCESS_KEY, token);
    axiosInstance.defaults.headers.common["Authorization"] = `Bearer ${token}`;

    const claims = parseJwt(token);
    const uid = claims?.sub || claims?.userId || claims?.id;

    cachedUserId = uid ? String(uid) : null;
  } else {
    localStorage.removeItem(ACCESS_KEY);
    delete axiosInstance.defaults.headers.common["Authorization"];
    cachedUserId = null;
  }
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_KEY);
}

// ======================================================
//  studentId 대신 → 토큰 기반 userId 조회
// ======================================================
export function getStudentId(): string | null {
  if (cachedUserId) return cachedUserId;

  const token = getAccessToken();
  if (!token) return null;

  const claims = parseJwt(token);
  const uid = claims?.sub || claims?.userId || claims?.id;
  cachedUserId = uid ? String(uid) : null;

  return cachedUserId;
}

// 앱 시작 시 토큰이 있으면 자동 적용
const bootToken = getAccessToken();
if (bootToken) setAccessToken(bootToken);

// ======================================================
//  로그아웃 API (백엔드 refresh 쿠키 삭제 + 로컬 토큰 삭제)
// ======================================================
export async function logoutApi() {
  try {
    await axiosInstance.post("/api/v1/auth/logout", {});
  } finally {
    setAccessToken(null);
  }
}

// ======================================================
//  401/403 → reissue 자동 처리 인터셉터
// ======================================================
let isRefreshing = false;
let queue: Array<{
  resolve: (v: any) => void;
  reject: (e: any) => void;
}> = [];

axiosInstance.interceptors.response.use(
  (res) => res,
  async (error) => {
    const { config, response } = error || {};
    const status = response?.status;

    if ((status === 401 || status === 403) && !config.__isRetryRequest) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => queue.push({ resolve, reject }));
      }

      isRefreshing = true;
      config.__isRetryRequest = true;

      try {
        const r = await axiosInstance.post("/api/v1/auth/reissue", {});
        const token = r?.data?.accessToken;

        if (token) setAccessToken(token);

        queue.forEach((p) => p.resolve(axiosInstance(config)));
        queue = [];
        return axiosInstance(config);
      } catch (e) {
        queue.forEach((p) => p.reject(e));
        queue = [];
        setAccessToken(null);
        throw e;
      } finally {
        isRefreshing = false;
      }
    }

    throw error;
  }
);

// ======================================================
//  전체 인증 정보 초기화 (logout 시 사용 가능)
// ======================================================
export function clearAuth() {
  setAccessToken(null);
}
