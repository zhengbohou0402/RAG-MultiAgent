import { useCallback, useEffect, useState } from "react";
import { App as AntApp, Button, Input, Space, Tag, Typography } from "antd";
import { ApartmentOutlined, ReloadOutlined, SendOutlined } from "@ant-design/icons";
import { api } from "../api/client";
import type { AgentCard, McpResponse } from "../api/client";
import { useI18n } from "../i18n";

const { Text } = Typography;
const { TextArea } = Input;

export default function A2ADemo() {
  const { message } = AntApp.useApp();
  const { language } = useI18n();
  const zh = language === "zh";
  const [card, setCard] = useState<AgentCard | null>(null);
  const [query, setQuery] = useState(zh
    ? "有没有 GPU 相关活动？我有大模型部署需求。"
    : "Are there GPU-related campaigns for my LLM deployment needs?");
  const [result, setResult] = useState<McpResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const loadCard = useCallback(async () => {
    try {
      setCard(await api.a2a.card());
    } catch (error) {
      message.error(error instanceof Error ? error.message : (zh ? "Agent Card 加载失败" : "Agent Card failed to load"));
    }
  }, [message, zh]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadCard(), 0);
    return () => window.clearTimeout(timer);
  }, [loadCard]);

  async function send() {
    setLoading(true);
    try {
      setResult(await api.a2a.send(query));
    } catch (error) {
      message.error(error instanceof Error ? error.message : (zh ? "A2A 调用失败" : "A2A call failed"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="admin-shell protocol-shell">
      <div className="admin-header">
        <div>
          <Text type="secondary" className="page-kicker">Agent2Agent Demo</Text>
          <h1>{zh ? "A2A 产品 Agent 演示" : "A2A Product Agent Demo"}</h1>
          <p>{zh
            ? "Ops_Marketing_Agent 可通过 A2A 调用 ProductTechAgent 获取产品信息，再生成推广内容。"
            : "Ops_Marketing_Agent calls ProductTechAgent through A2A for product context before generating promotion content."}</p>
        </div>
        <Button icon={<ReloadOutlined />} onClick={loadCard}>{zh ? "刷新 Agent Card" : "Refresh Agent Card"}</Button>
      </div>
      <section className="admin-panel">
        <div className="panel-title-row"><Text strong><ApartmentOutlined /> Agent Card</Text><Tag color="green">{card?.capabilities?.streaming ? "streaming" : "ready"}</Tag></div>
        <div className="agent-card-preview">
          <div><Text type="secondary">Agent</Text><strong>{card?.name || "-"}</strong></div>
          <div><Text type="secondary">Endpoint</Text><strong>{card?.url || "-"}</strong></div>
          <div><Text type="secondary">Version</Text><strong>{card?.version || "-"}</strong></div>
        </div>
      </section>
      <section className="admin-panel protocol-input-panel">
        <div className="panel-title-row"><Text strong>{zh ? "message/send 请求" : "message/send Request"}</Text><Tag color="blue">POST /message:send</Tag></div>
        <TextArea rows={4} value={query} onChange={(event) => setQuery(event.target.value)} />
        <Space className="panel-actions"><Button type="primary" icon={<SendOutlined />} onClick={send} loading={loading}>{zh ? "发送给产品 Agent" : "Send to Product Agent"}</Button></Space>
      </section>
      <section className="admin-panel">
        <div className="panel-title-row"><Text strong>{zh ? "A2A 响应" : "A2A Response"}</Text><Text type="secondary">{result ? "completed" : "waiting"}</Text></div>
        <pre className="json-view">{result ? JSON.stringify(result, null, 2) : (zh ? "暂无调用结果" : "No call result yet.")}</pre>
      </section>
    </div>
  );
}
