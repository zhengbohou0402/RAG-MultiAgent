import { createContext } from "react";
import type { SmartCloudUser } from "../api/client";

export interface AuthContextValue {
  user: SmartCloudUser | null;
  token: string | null;
  loading: boolean;
  login: (username: string, password: string, tenantId?: string) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
