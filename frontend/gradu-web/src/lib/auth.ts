// src/lib/auth.ts
export const GUEST_KEY = "guest_mode";

// 게스트 모드 On/Off
export function setGuestMode(on: boolean) {
  if (on) {
    sessionStorage.setItem(GUEST_KEY, "1");
  } else {
    sessionStorage.removeItem(GUEST_KEY);
  }
}

// 현재 게스트 모드인지 확인
export function isGuestMode() {
  return sessionStorage.getItem(GUEST_KEY) === "1";
}
