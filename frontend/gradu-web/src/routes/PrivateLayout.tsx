import { Outlet } from "react-router-dom";
import AppShell from "../components/AppShell";
import { PrivateRoute } from "./PrivateRoute";

export default function PrivateLayout() {
  return (
    <PrivateRoute>
      <AppShell>
        <Outlet />
      </AppShell>
    </PrivateRoute>
  );
}
