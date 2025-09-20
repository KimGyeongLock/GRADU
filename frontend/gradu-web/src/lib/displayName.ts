// src/lib/displayName.ts
export function getDisplayName() {
  const sid = localStorage.getItem("student_id") || "";
  let name = localStorage.getItem("name") || "";

  // 토큰 클레임에서 name 추출 시도
  if (!name) {
    try {
      const t = localStorage.getItem("access_token");
      if (t) {
        const p = JSON.parse(
          decodeURIComponent(
            atob(t.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))
              .split("")
              .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
              .join("")
          )
        );
        name = p?.name || p?.nickname || "";
      }
    } catch {}
  }

  // "이름(학번)님" 또는 "학번님"
  return name && sid ? `${name}(${sid})님` : sid ? `${sid}님` : "";
}
