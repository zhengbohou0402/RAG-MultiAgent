import { useState } from "react";
import type { ReactNode } from "react";
import { App as AntApp, Button, Input, Space, Tag, Typography } from "antd";
import { ApiOutlined, FileProtectOutlined, PlayCircleOutlined, ProfileOutlined, ToolOutlined } from "@ant-design/icons";
import { api } from "../api/client";
import { useI18n } from "../i18n";

const { Text } = Typography;
const { TextArea } = Input;

type ToolAction = { label: string; name: string; icon: ReactNode };

export default function McpTools() {
  const { message } = AntApp.useApp();
  const { language } = useI18n();
  const zh = language === "zh";
  const [query, setQuery] = useState(zh
    ? "帮我查看本月账单，并给出云服务器成本优化建议"
    : "Review this month's bill and provide cloud cost optimization recommendations.");
  const [result, setResult] = useState("");
  const [loading, setLoading] = useState<string | null>(null);
  const tools: ToolAction[] = [
    { label: zh ? "账单查询" : "Billing Query", name: "billing.query", icon: <ProfileOutlined /> },
    { label: zh ? "备案清单" : "ICP Checklist", name: "icp.checklist", icon: <FileProtectOutlined /> },
    { label: zh ? "营销包" : "Marketing Package", name: "marketing.generate_package", icon: <ToolOutlined /> },
    { label: zh ? "研究计划" : "Research Plan", name: "research.plan", icon: <ApiOutlined /> },
    { label: zh ? "H5 生成" : "Generate H5", name: "h5.generate", icon: <PlayCircleOutlined /> },
  ];

  async function call(method: string, params?: Record<string, unknown>) {
    setLoading(method);
    try {
      setResult(JSON.stringify(await api.mcp.call(method, params), null, 2));
    } catch (error) {
      message.error(error instanceof Error ? error.message : (zh ? "MCP 调用失败" : "MCP call failed"));
    } finally {
      setLoading(null);
    }
  }

  async function callTool(name: string) {
    await call("tools/call", {
      name,
      arguments: {
        query,
        product_name: query,
        scenario: zh ? "云产品服务咨询" : "Cloud product consultation",
        audience: zh ? "中小企业 IT 负责人" : "SMB IT decision maker",
      },
    });
  }

  return (
    <div className="admin-shell protocol-shell">
      <div className="admin-header">
        <div>
          <Text type="secondary" className="page-kicker">MCP Tools</Text>
          <h1>{zh ? "MCP 工具中心" : "MCP Tool Center"}</h1>
          <p>{zh
            ? "模拟 2025-06-18 Streamable HTTP + JSON-RPC 工具接口，展示工具发现、调用和本地 fallback。"
            : "A 2025-06-18 Streamable HTTP + JSON-RPC demo for tool discovery, invocation, and local fallback."}</p>
        </div>
        <Space wrap>
          <Button onClick={() => call("initialize")} loading={loading === "initialize"}>initialize</Button>
          <Button onClick={() => call("tools/list")} loading={loading === "tools/list"}>tools/list</Button>
        </Space>
      </div>
      <section className="admin-panel protocol-input-panel">
        <div className="panel-title-row"><Text strong>{zh ? "调用参数" : "Invocation Parameters"}</Text><Tag color="blue">POST /mcp</Tag></div>
        <TextArea value={query} onChange={(event) => setQuery(event.target.value)} rows={4} placeholder={zh ? "输入工具调用 query" : "Enter a tool query"} />
      </section>
      <section className="protocol-tool-grid">
        {tools.map((tool) => (
          <button type="button" key={tool.name} className="protocol-tool-button" onClick={() => callTool(tool.name)} disabled={loading !== null}>
            <span>{tool.icon}</span><strong>{tool.label}</strong><small>{tool.name}</small>
          </button>
        ))}
      </section>
      <section className="admin-panel">
        <div className="panel-title-row"><Text strong>{zh ? "JSON-RPC 响应" : "JSON-RPC Response"}</Text><Text type="secondary">{loading ? "calling..." : "ready"}</Text></div>
        <pre className="json-view">{result || (zh ? "点击上方按钮发起 MCP 调用" : "Choose a tool above to invoke MCP.")}</pre>
      </section>
    </div>
  );
}
