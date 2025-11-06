// src/lib/axios.ts
import axios from "axios";

export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
});

const ACCESS_KEY = "access_token";
const SID_KEY = "student_id";
const NAME_KEY = "name";

// base64url → JSON
function parseJwt(token: string): any {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export const getStudentId = () => localStorage.getItem(SID_KEY);
export const setStudentId = (sid: string | null) => {
  if (sid) localStorage.setItem(SID_KEY, sid);
  else localStorage.removeItem(SID_KEY);
};

export const getProfileName = () => localStorage.getItem(NAME_KEY);
export const setProfileName = (name: string | null) => {
  if (name) localStorage.setItem(NAME_KEY, name);
  else localStorage.removeItem(NAME_KEY);
};

export const setAccessToken = (t: string | null) => {
  if (t) {
    localStorage.setItem(ACCESS_KEY, t);
    axiosInstance.defaults.headers.common["Authorization"] = `Bearer ${t}`;

    // 토큰에서 studentId / name 자동 추출
    const claims = parseJwt(t);
    const sid =
      claims?.studentId || claims?.sid || claims?.sub || claims?.userId;
    const name =
      claims?.name || claims?.nickname || claims?.userName || claims?.username;

    if (sid) setStudentId(String(sid));
    if (name) setProfileName(String(name));
  } else {
    localStorage.removeItem(ACCESS_KEY);
    delete axiosInstance.defaults.headers.common["Authorization"];
    setStudentId(null);
    setProfileName(null);
  }
};

// 🔸 로그아웃 API + 로컬 정리
export async function logoutApi() {
  try {
    await axiosInstance.post("/api/v1/auth/logout", {}); // Authorization + refresh 쿠키 자동 포함
  } catch {
    // 실패해도 로컬 비우고 진행
  } finally {
    setAccessToken(null);
    setStudentId(null);
    localStorage.removeItem("name");
  }
}

// 앱 시작 시 헤더 반영
const bootToken = localStorage.getItem(ACCESS_KEY);
if (bootToken) setAccessToken(bootToken);

// 401/403 1회 재발급(reissue)
let isRefreshing = false;
let queue: Array<{ resolve: (v: any) => void; reject: (e: any) => void }> = [];

axiosInstance.interceptors.response.use(
  (res) => res,
  async (error) => {
    const { config, response } = error || {};
    const status = response?.status;
    if ((status === 401 || status === 403) && !config.__isRetryRequest) {
      if (isRefreshing) return new Promise((resolve, reject) => queue.push({ resolve, reject }));
      isRefreshing = true;
      (config as any).__isRetryRequest = true;
      try {
        const r = await axiosInstance.post("/api/v1/auth/reissue", {});
        const token = r?.data?.accessToken;
        const name = r?.data?.name; // 응답에 name이 오면 저장
        if (token) setAccessToken(token);
        if (name) setProfileName(String(name));
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
