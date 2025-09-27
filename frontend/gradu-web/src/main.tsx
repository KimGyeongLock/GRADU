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
import CurriculumPage from "./pages/CurriculumPage";
import CurriculumDetailPage from "./pages/CurriculumDetailPage"; // ← 상세 페이지
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import { PrivateRoute } from "./routes/PrivateRoute";

import "./index.css";
import "./styles/reset.css"; // (원하는 대로) 마지막에

const qc = new QueryClient();

const PrivateLayout = () => (
  <PrivateRoute>
    <AppShell>
      <Outlet />
    </AppShell>
  </PrivateRoute>
);

const router = createBrowserRouter([
  // 공개 라우트
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },

  // 보호된 라우트(레이아웃 + 중첩)
  {
    element: <PrivateLayout />,
    children: [
      { path: "/", element: <Navigate to="/curriculum" replace /> }, // 기본 리다이렉트
      { path: "/curriculum", element: <CurriculumPage /> },
      { path: "/curriculum/:category", element: <CurriculumDetailPage /> }, // 상세
    ],
  },

  // 기타 → 홈으로
  { path: "*", element: <Navigate to="/" replace /> },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={qc}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>
);
