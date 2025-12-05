// src/routes/PublicRoute.tsx
import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { isAuthed } from "./PrivateRoute";

export function PublicRoute({ children }: { children: ReactNode }) {
  const authed = isAuthed();
  const loc = useLocation();

  if (authed) {
    return <Navigate to="/" replace state={{ from: loc }} />;
  }

  return <>{children}</>;
}
