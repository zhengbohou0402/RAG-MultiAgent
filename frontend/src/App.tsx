import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { ConfigProvider, App as AntApp, theme } from "antd";
import { useState, useEffect } from "react";
import Chat from "./pages/Chat";
import Settings from "./pages/Settings";
import Manage from "./pages/Manage";
import Dashboard from "./pages/Dashboard";
import Prompts from "./pages/Prompts";
import Login from "./pages/Login";
import Billing from "./pages/Billing";
import McpTools from "./pages/McpTools";
import A2ADemo from "./pages/A2ADemo";
import MarketingAssets from "./pages/MarketingAssets";
import Observability from "./pages/Observability";
import ApiKeySetupModal from "./components/ApiKeySetupModal";
import WebNavigation from "./components/WebNavigation";
import { api } from "./api/client";
import { AuthProvider } from "./context/AuthContext";
import { useAuth } from "./context/useAuth";
import { I18nProvider, useI18n } from "./i18n";

const THEME_KEY = "smartcloud_theme";

function AppRoutes({ isDark, onToggleTheme }: { isDark: boolean; onToggleTheme: () => void }) {
  const location = useLocation();
  const { user, loading } = useAuth();
  const { t } = useI18n();
  const [dashscopeConfigured, setDashscopeConfigured] = useState<boolean | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function poll() {
      while (!cancelled) {
        try {
          await api.health();
        } catch {
          await new Promise((r) => setTimeout(r, 1000));
          continue;
        }
        try {
          const s = await api.configStatus();
          if (!cancelled) {
            // Treat the key as configured only when the API explicitly returns true.
            setDashscopeConfigured(s.dashscope_configured === true);
          }
        } catch {
          // If the backend is reachable but config status fails, still show setup guidance.
          if (!cancelled) setDashscopeConfigured(false);
        }
        await new Promise((r) => setTimeout(r, 1000));
      }
    }
    poll();
    return () => {
      cancelled = true;
    };
  }, []);

  const showApiKeyModal =
    dashscopeConfigured === false && location.pathname !== "/settings" && location.pathname !== "/login";

  if (!loading && !user && location.pathname !== "/login") {
    return <Navigate to="/login" replace />;
  }

  if (loading && location.pathname !== "/login") {
    return <div className="boot-screen">{t("app.boot")}</div>;
  }

  return (
    <>
      <ApiKeySetupModal
        open={showApiKeyModal}
        onSuccess={() => setDashscopeConfigured(true)}
      />
      <div className={`app-shell${location.pathname === "/login" ? " login-mode" : ""}`}>
        {location.pathname !== "/login" && (
          <WebNavigation isDark={isDark} onToggleTheme={onToggleTheme} />
        )}
        <main className="app-content">
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<Chat />} />
            <Route path="/billing" element={<Billing />} />
            <Route path="/mcp" element={<McpTools />} />
            <Route path="/a2a" element={<A2ADemo />} />
            <Route path="/marketing" element={<MarketingAssets />} />
            <Route path="/observability" element={<Observability />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/manage" element={<Manage />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/prompts" element={<Prompts />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </>
  );
}

function App() {
  const [isDark, setIsDark] = useState(() => {
    const saved = localStorage.getItem(THEME_KEY);
    return saved === "dark";
  });

  useEffect(() => {
    localStorage.setItem(THEME_KEY, isDark ? "dark" : "light");
    document.documentElement.setAttribute("data-theme", isDark ? "dark" : "light");
  }, [isDark]);

  return (
    <ConfigProvider
      theme={{
        algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
        token: {
          fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
          colorPrimary: "#1677ff",
          borderRadius: 8,
          colorBgContainer: isDark ? "#1a1a2e" : "#ffffff",
        },
      }}
    >
      <AntApp>
        <BrowserRouter>
          <AuthProvider>
            <I18nProvider>
              <AppRoutes isDark={isDark} onToggleTheme={() => setIsDark(!isDark)} />
            </I18nProvider>
          </AuthProvider>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  );
}

export default App;
