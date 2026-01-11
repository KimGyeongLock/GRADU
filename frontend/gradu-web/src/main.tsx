// src/main.tsx
import React from "react";
import ReactDOM from "react-dom/client";
import {
  createBrowserRouter,
  RouterProvider,
  Navigate,
  Outlet,
} from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import AppShell from "./components/AppShell";
import CurriculumPage from "./pages/CurriculumPage/CurriculumPage";
import CurriculumDetailPage from "./pages/CurriculumDetailPage/CurriculumDetailPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ResetPasswordPage from "./pages/ResetPasswordPage";
import { PrivateRoute } from "./routes/PrivateRoute";
import { PublicRoute } from "./routes/PublicRoute";
import { OverlayUIProvider } from "./ui/OverlayUIContext";

import "./index.css";
import "./styles/reset.css";

const qc = new QueryClient();

const PrivateLayout = () => (
  <PrivateRoute>
    <AppShell>
      <Outlet />
    </AppShell>
  </PrivateRoute>
);

const router = createBrowserRouter([
  {
    path: "/login",
    element: (
      <PublicRoute>
        <LoginPage />
      </PublicRoute>
    ),
  },
  {
    path: "/register",
    element: (
      <PublicRoute>
        <RegisterPage />
      </PublicRoute>
    ),
  },
  {
    path: "/reset-password",
    element: (
      <PublicRoute>
        <ResetPasswordPage />
      </PublicRoute>
    ),
  },

  // 보호 라우트
  {
    element: <PrivateLayout />,
    children: [
      { path: "/", element: <Navigate to="/curriculum" replace /> },
      { path: "/curriculum", element: <CurriculumPage /> },
      { path: "/curriculum/:category", element: <CurriculumDetailPage /> },
    ],
  },

  // 기타 → 홈으로
  { path: "*", element: <Navigate to="/" replace /> },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={qc}>
      <OverlayUIProvider>
        <RouterProvider router={router} />
      </OverlayUIProvider>
    </QueryClientProvider>
  </React.StrictMode>
);
