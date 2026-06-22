import { App as AntApp, Button, Form, Input, Space, Tag, Typography } from "antd";
import { CloudServerOutlined, LoginOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Navigate, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/useAuth";
import { useI18n } from "../i18n";

const { Text } = Typography;

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const { message } = AntApp.useApp();
  const [submitting, setSubmitting] = useState(false);
  const { t } = useI18n();

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function onFinish(values: { username: string; password: string; tenant_id: string }) {
    setSubmitting(true);
    try {
      await login(values.username, values.password, values.tenant_id);
      message.success(t("login.success"));
      navigate("/", { replace: true });
    } catch (error) {
      message.error(error instanceof Error ? error.message : t("login.failed"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-shell">
      <section className="login-hero">
        <div className="login-brand-mark">SC</div>
        <Text className="page-kicker">{t("login.kicker")}</Text>
        <h1>{t("login.title")}</h1>
        <p>{t("login.desc")}</p>
        <div className="login-tags">
          <Tag color="blue">LangGraph4j</Tag>
          <Tag color="green">Agentic RAG</Tag>
          <Tag color="orange">MCP / A2A</Tag>
        </div>
      </section>

      <section className="login-panel">
        <Space direction="vertical" size={4} className="login-panel-head">
          <SafetyCertificateOutlined />
          <h2>{t("login.panelTitle")}</h2>
          <Text type="secondary">{t("login.panelHint")}</Text>
        </Space>

        <Form
          layout="vertical"
          initialValues={{ username: "demo-admin", password: "demo123456", tenant_id: "tenant-demo" }}
          onFinish={onFinish}
        >
          <Form.Item
            label={t("login.username")}
            name="username"
            rules={[{ required: true, message: t("login.usernameRequired") }]}
          >
            <Input size="large" placeholder="demo-admin" />
          </Form.Item>
          <Form.Item
            label={t("login.password")}
            name="password"
            rules={[{ required: true, message: t("login.passwordRequired") }]}
          >
            <Input.Password size="large" placeholder="demo123456" />
          </Form.Item>
          <Form.Item
            label={t("login.tenant")}
            name="tenant_id"
            rules={[{ required: true, message: t("login.tenantRequired") }]}
          >
            <Input size="large" placeholder="tenant-demo" prefix={<CloudServerOutlined />} />
          </Form.Item>
          <Button
            block
            size="large"
            type="primary"
            htmlType="submit"
            loading={submitting}
            icon={<LoginOutlined />}
          >
            {t("login.submit")}
          </Button>
        </Form>
      </section>
    </div>
  );
}
