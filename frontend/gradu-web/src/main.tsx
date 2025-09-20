// src/main.tsx (또는 router 파일)
import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import AppShell from "./components/AppShell";
import CurriculumPage from "./pages/CurriculumPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import { PrivateRoute } from "./routes/PrivateRoute";
import './index.css';          // tailwind 포함
import './styles/reset.css';   // ★ 마지막에

const qc = new QueryClient();

const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },

  // 기본은 커리큘럼 + AppShell
  {
    path: "/",
    element: (
      <PrivateRoute>
        <AppShell>
          <CurriculumPage />
        </AppShell>
      </PrivateRoute>
    ),
  },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={qc}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>
);
