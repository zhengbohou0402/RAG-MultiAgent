import { useCallback, useEffect, useRef } from "react";
import { Button, Layout, Tag } from "antd";
import {
  CloudServerOutlined,
  AuditOutlined,
  FileProtectOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  SafetyCertificateOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import ChatMessage from "../components/ChatMessage";
import Composer from "../components/Composer";
import { useChat } from "../hooks/useChat";
import { useI18n } from "../i18n";

const { Content, Header } = Layout;

export default function Chat() {
  const {
    messages,
    streaming,
    send,
    clearMessages,
  } = useChat();
  const { t } = useI18n();
  const chatRef = useRef<HTMLDivElement>(null);
  const agents = [
    { icon: <CloudServerOutlined />, name: "Product_Tech_Agent", desc: t("agent.product") },
    { icon: <AuditOutlined />, name: "Finance_Order_Agent", desc: t("agent.finance") },
    { icon: <FileProtectOutlined />, name: "ICP_Service_Agent", desc: t("agent.icp") },
    { icon: <LineChartOutlined />, name: "Ops_Marketing_Agent", desc: t("agent.marketing") },
    { icon: <RadarChartOutlined />, name: "Deep_Research_Agent", desc: t("agent.research") },
  ];

  const handleNewChat = useCallback(() => {
    clearMessages();
  }, [clearMessages]);

  useEffect(() => {
    if (chatRef.current) {
      chatRef.current.scrollTop = chatRef.current.scrollHeight;
    }
  }, [messages]);

  useEffect(() => {
    const handleNew = () => {
      handleNewChat();
    };
    const handleClearAll = () => {
      clearMessages();
    };
    window.addEventListener("new-chat", handleNew);
    window.addEventListener("clear-chat", handleClearAll);
    return () => {
      window.removeEventListener("new-chat", handleNew);
      window.removeEventListener("clear-chat", handleClearAll);
    };
  }, [clearMessages, handleNewChat]);

  return (
    <Layout className="chat-main" style={{ height: "100%" }}>
      <Header className="chat-header">
        <div className="chat-header-stack">
          <span className="chat-header-title">{t("chat.title")}</span>
        </div>
        <div className="chat-header-actions">
          <div className="chat-header-tags">
            <Tag color="blue">LangGraph4j</Tag>
            <Tag color="cyan">Agentic RAG</Tag>
            <Tag color="green">{t("chat.tag.mcp")}</Tag>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleNewChat} disabled={streaming}>
            {t("chat.newCase")}
          </Button>
        </div>
      </Header>
      <Content className="chat-content" ref={chatRef as React.RefObject<HTMLDivElement>}>
        {messages.length === 0 ? (
          <div className="smartcloud-welcome">
            <div className="welcome-copy">
              <Tag icon={<SafetyCertificateOutlined />} color="processing">{t("chat.badge")}</Tag>
              <h1>{t("chat.welcomeTitle")}</h1>
              <p>{t("chat.welcomeDesc")}</p>
            </div>
            <div className="agent-grid">
              {agents.map((agent) => (
                <div className="agent-card" key={agent.name}>
                  <div className="agent-card-icon">{agent.icon}</div>
                  <strong>{agent.name}</strong>
                  <span>{agent.desc}</span>
                </div>
              ))}
            </div>
            <div className="smartcloud-metrics">
              <div><strong>5</strong><span>{t("chat.metric.agents")}</span></div>
              <div><strong>2</strong><span>{t("chat.metric.retrieval")}</span></div>
              <div><strong>1</strong><span>{t("chat.metric.workflow")}</span></div>
              <div><strong>SSE</strong><span>{t("chat.metric.streaming")}</span></div>
            </div>
          </div>
        ) : (
          messages.map((msg, i) => <ChatMessage key={i} message={msg} />)
        )}
      </Content>
      <div className="chat-footer">
        <Composer onSend={send} disabled={streaming} />
      </div>
    </Layout>
  );
}
