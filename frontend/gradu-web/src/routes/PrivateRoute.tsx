import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";

export function isAuthed() {
  return Boolean(localStorage.getItem("access_token"));
}

export function PrivateRoute({ children }: { children: ReactNode }) {
  const authed = isAuthed();
  const loc = useLocation();
  if (!authed) return <Navigate to="/login" replace state={{ from: loc }} />;
  return <>{children}</>;
}
