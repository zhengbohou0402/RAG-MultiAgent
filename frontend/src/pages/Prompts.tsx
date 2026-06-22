import { useEffect, useState } from "react";
import { Button, Typography, Space, Input, message, Card } from "antd";
import { SaveOutlined } from "@ant-design/icons";
import { api } from "../api/client";

const { Text } = Typography;
const { TextArea } = Input;
const DEFAULT_PROMPT =
  "You are SmartCloud ServiceOps Assistant, designed to help enterprise cloud users with product support, billing, ICP filing, marketing operations, and architecture research. Always be practical, concise, and grounded in retrieved knowledge or tool results.";

export default function Prompts() {
  const [promptText, setPromptText] = useState(DEFAULT_PROMPT);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void api.prompt.get()
      .then((result) => setPromptText(result.system_prompt))
      .catch((error) => message.error(`Could not load system prompt: ${error}`))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    if (!promptText.trim()) {
      message.error("System prompt must not be empty.");
      return;
    }
    setSaving(true);
    try {
      await api.prompt.save(promptText);
      message.success("System prompt saved. New conversations will use it immediately.");
    } catch (error) {
      message.error(`Could not save system prompt: ${error}`);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-shell settings-shell">
      <div className="admin-header settings-header">
        <div>
          <Text type="secondary" className="page-kicker">SmartCloud Platform</Text>
          <h1>System Prompts</h1>
          <p>Configure the underlying persona and instructions for the SmartCloud assistant.</p>
        </div>
      </div>

      <div className="settings-layout">
        <section className="admin-panel settings-card">
          <Space orientation="vertical" size="large" className="settings-stack">
            <section>
              <div className="panel-title-row">
                <Text strong>Global System Prompt</Text>
              </div>
              <Text type="secondary" className="field-help">
                This prompt will be injected at the beginning of every conversation to steer the model's behavior.
              </Text>
              <TextArea
                rows={8}
                value={promptText}
                onChange={(e) => setPromptText(e.target.value)}
                disabled={loading}
                placeholder="Enter system prompt here..."
                style={{ borderRadius: "12px", padding: "16px", fontSize: "14px" }}
              />
            </section>
            <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={saving || loading} block>
              Save Configuration
            </Button>
          </Space>
        </section>

        <aside className="admin-panel settings-side-panel">
          <Text strong>Suggestions</Text>
          <div className="info-grid settings-info-grid" style={{ marginTop: "16px" }}>
            <Card size="small" className="manage-card" style={{ cursor: "pointer", marginBottom: "8px" }} onClick={() => setPromptText("You are SmartCloud's enterprise technical support specialist. Provide structured, production-safe cloud guidance based on retrieved documents and tool results. Do not hallucinate product policies, prices, or internal status.")}>
              <Text strong style={{ fontSize: "12px" }}>Enterprise Support</Text>
              <p style={{ margin: 0, fontSize: "11px", color: "#888" }}>Strict, operational, production-safe tone.</p>
            </Card>
            <Card size="small" className="manage-card" style={{ cursor: "pointer" }} onClick={() => setPromptText("You are SmartCloud's customer success consultant. Use a friendly business tone, recommend practical next steps, and keep every claim grounded in knowledge-base or tool evidence.")}>
              <Text strong style={{ fontSize: "12px", color: "#0f766e" }}>Customer Success</Text>
              <p style={{ margin: 0, fontSize: "11px", color: "#888" }}>Warm business tone with clear next steps.</p>
            </Card>
          </div>
        </aside>
      </div>
    </div>
  );
}
