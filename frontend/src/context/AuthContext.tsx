import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api, AUTH_TOKEN_KEY } from "../api/client";
import type { SmartCloudUser } from "../api/client";
import { AuthContext } from "./auth-core";
import type { AuthContextValue } from "./auth-core";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(AUTH_TOKEN_KEY));
  const [user, setUser] = useState<SmartCloudUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function loadUser() {
      if (!token) {
        setUser(null);
        setLoading(false);
        return;
      }
      try {
        const current = await api.auth.me();
        if (!cancelled) {
          setUser(current);
        }
      } catch {
        localStorage.removeItem(AUTH_TOKEN_KEY);
        if (!cancelled) {
          setToken(null);
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    void loadUser();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    loading,
    login: async (username: string, password: string, tenantId?: string) => {
      const response = await api.auth.login({ username, password, tenant_id: tenantId });
      localStorage.setItem(AUTH_TOKEN_KEY, response.token);
      setToken(response.token);
      setUser(response.user);
    },
    logout: () => {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      setToken(null);
      setUser(null);
    },
  }), [loading, token, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
