import { Button, Segmented } from "antd";
import {
  ApartmentOutlined,
  ApiOutlined,
  BulbOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  LogoutOutlined,
  MessageOutlined,
  NodeIndexOutlined,
  PictureOutlined,
  SettingOutlined,
  SlidersOutlined,
  WalletOutlined,
} from "@ant-design/icons";
import { NavLink } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { useI18n } from "../i18n";

interface Props {
  isDark: boolean;
  onToggleTheme: () => void;
}

const NAV_ITEMS = [
  { to: "/", labelKey: "nav.chat", icon: <MessageOutlined /> },
  { to: "/billing", labelKey: "nav.billing", icon: <WalletOutlined /> },
  { to: "/mcp", labelKey: "nav.mcp", icon: <ApiOutlined /> },
  { to: "/a2a", labelKey: "nav.a2a", icon: <ApartmentOutlined /> },
  { to: "/marketing", labelKey: "nav.marketing", icon: <PictureOutlined /> },
  { to: "/observability", labelKey: "nav.observability", icon: <NodeIndexOutlined /> },
  { to: "/manage", labelKey: "nav.knowledge", icon: <DatabaseOutlined /> },
  { to: "/dashboard", labelKey: "nav.dashboard", icon: <DashboardOutlined /> },
  { to: "/settings", labelKey: "nav.settings", icon: <SettingOutlined /> },
  { to: "/prompts", labelKey: "nav.prompts", icon: <SlidersOutlined /> },
] as const;

export default function WebNavigation({ isDark, onToggleTheme }: Props) {
  const { user, logout } = useAuth();
  const { language, setLanguage, t } = useI18n();

  return (
    <aside className="web-nav" aria-label="Primary navigation">
      <div className="web-nav-brand">
        <div className="web-nav-logo">SC</div>
        <div className="web-nav-brand-text">
          <strong>{t("nav.brand")}</strong>
          <span>SmartCloud ServiceOps</span>
        </div>
      </div>

      <nav className="web-nav-items">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            title={t(item.labelKey)}
            className={({ isActive }) => `web-nav-item${isActive ? " active" : ""}`}
          >
            <span className="web-nav-icon">{item.icon}</span>
            <span className="web-nav-item-text">{t(item.labelKey)}</span>
          </NavLink>
        ))}
      </nav>

      <div className="web-nav-foot">
        <div className="web-nav-user">
          <strong>{user?.displayName || t("nav.defaultUser")}</strong>
          <span>{user?.tenantId || "tenant-demo"}</span>
        </div>
        <Segmented
          className="language-switch"
          block
          size="small"
          value={language}
          onChange={(value) => setLanguage(value as "zh" | "en")}
          options={[
            { label: "中文", value: "zh" },
            { label: "EN", value: "en" },
          ]}
        />
        <Button block icon={<BulbOutlined />} onClick={onToggleTheme}>
          {isDark ? t("nav.light") : t("nav.dark")}
        </Button>
        <Button block icon={<LogoutOutlined />} onClick={logout}>
          {t("nav.logout")}
        </Button>
      </div>
    </aside>
  );
}
