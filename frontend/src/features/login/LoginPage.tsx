import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../app/auth/useAuth";
import { apiFetch, ApiError } from "../../app/api/http";

type LoginRequest = {
  username: string;
  password: string;
};

type LoginResponseData = {
  accessToken: string;
  userId: string;
  roles: string[];
};

export default function LoginPage() {
  const nav = useNavigate();
  const { login } = useAuth();

   /* Demo defaults: in dev profile, users are seeded into the DB (see backend application-dev.yml). */
  const [username, setUsername] = useState("user1");
  const [password, setPassword] = useState("user1-pass");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);

    try {
      // apiFetch unwraps { success, data } and returns data directly
      const data = await apiFetch<LoginResponseData>("/auth/login", {
        method: "POST",
        body: { username, password } satisfies LoginRequest,
      });

      if (!data?.accessToken) {
          setError("Unexpected login response format");
          return;
      }

      const { accessToken, roles, userId } = data as any;
      // roles is array
      const rolesArr: string[] = Array.isArray(roles) ? roles : (typeof roles === "string" && roles.length > 0) ? [roles] : [];

/*       console.log('typeof loginWithToken', typeof loginWithToken, loginWithToken);
      if (typeof loginWithToken !== 'function') {
        throw new Error(`loginWithToken is not a function (typeof=${typeof loginWithToken})`);
      } */
      login({ accessToken, roles: rolesArr, userId });

      const isSupport = rolesArr.some((r) => r === "SUPPORT" || r === "ROLE_SUPPORT");
      nav(isSupport ? "/support/disputes" : "/transactions");
    } catch (err: any) {
      const ae = err as ApiError;
      setError(ae?.detail ?? ae?.title ?? err?.message ?? "Login failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-wrap">
      <div className="card login-card">
        <h2 style={{ marginBottom: 6 }}>CAPITEC</h2>
        <div className="page-subtitle">Transaction Dispute Portal</div>

        {error && <div className="error-box">{error}</div>}

        <form onSubmit={onSubmit}>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: "block", marginBottom: 6, color: "#334155" }}>Username</label>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
            />
          </div>

          <div style={{ marginBottom: 14 }}>
            <label style={{ display: "block", marginBottom: 6, color: "#334155" }}>Password</label>
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              autoComplete="current-password"
            />
          </div>

          <button type="submit" disabled={busy} className="btn btn-primary" style={{ width: "100%" }}>
            {busy ? "Logging in..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  )
}
