// src/routes/PrivateRoute.tsx
import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { isGuestMode } from "../lib/auth";

export function isAuthed() {
  const hasToken = Boolean(localStorage.getItem("access_token"));
  const guest = isGuestMode();
  return hasToken || guest;
}

export function PrivateRoute({ children }: { children: ReactNode }) {
  const authed = isAuthed();
  const loc = useLocation();
  if (!authed) return <Navigate to="/login" replace state={{ from: loc }} />;
  return <>{children}</>;
}
