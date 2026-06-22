import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { NodeIndexOutlined, ReloadOutlined } from "@ant-design/icons";
import { api } from "../api/client";
import type { TraceRecord } from "../api/client";
import StatsCard from "../components/StatsCard";
import { useI18n } from "../i18n";

const { Text } = Typography;

export default function Observability() {
  const { language } = useI18n();
  const zh = language === "zh";
  const [traces, setTraces] = useState<TraceRecord[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setTraces(await api.traces.list());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const routeCounts = useMemo(() => traces.reduce<Record<string, number>>((acc, trace) => {
    acc[trace.route] = (acc[trace.route] || 0) + 1;
    return acc;
  }, {}), [traces]);
  const avgLatency = traces.length
    ? Math.round(traces.reduce((sum, trace) => sum + trace.latency_ms, 0) / traces.length)
    : 0;
  const columns: TableColumnsType<TraceRecord> = [
    { title: zh ? "时间" : "Time", dataIndex: "created_at", key: "created_at", width: 170, render: (value: number) => new Date(value * 1000).toLocaleString() },
    { title: "Route", dataIndex: "route", key: "route", render: (route: string) => <Tag color="blue">{route}</Tag> },
    { title: "Agent", dataIndex: "selected_agent", key: "selected_agent" },
    { title: zh ? "路由原因" : "Reason", dataIndex: "reason", key: "reason" },
    { title: zh ? "图路径" : "Graph Path", dataIndex: "nodes", key: "nodes", render: (nodes: string[]) => nodes.join(" -> ") },
    { title: zh ? "耗时" : "Latency", dataIndex: "latency_ms", key: "latency_ms", width: 90, render: (value: number) => `${value} ms` },
  ];

  return (
    <div className="admin-shell">
      <div className="admin-header">
        <div>
          <Text type="secondary" className="page-kicker">Observability</Text>
          <h1>{zh ? "Agent Trace 观测" : "Agent Trace Observability"}</h1>
          <p>{zh
            ? "本地记录 Supervisor 路由、LangGraph 节点、工具调用和耗时，可映射到 LangSmith / Phoenix 的面试讲法。"
            : "Local records of Supervisor routing, LangGraph nodes, tool calls, and latency, ready to explain as a LangSmith / Phoenix-style interview story."}</p>
        </div>
        <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>{zh ? "刷新" : "Refresh"}</Button>
      </div>
      <section className="admin-section">
        <div className="stats-grid">
          <StatsCard label={zh ? "Trace 数量" : "Trace Count"} value={traces.length} sub="memory + optional Mongo" accent="blue" />
          <StatsCard label={zh ? "平均路由耗时" : "Avg. Route Latency"} value={`${avgLatency} ms`} sub="workflow latency" accent="green" />
          <StatsCard label={zh ? "路由类型" : "Route Types"} value={Object.keys(routeCounts).length} sub="agent route diversity" accent="purple" />
          <StatsCard label="Tool Calls" value={traces.reduce((sum, trace) => sum + trace.tool_calls.length, 0)} sub="MCP / A2A events" accent="amber" />
        </div>
      </section>
      <section className="admin-panel">
        <div className="panel-title-row">
          <Text strong><NodeIndexOutlined /> {zh ? "路由分布" : "Route Distribution"}</Text>
          <Text type="secondary">{Object.entries(routeCounts).map(([route, count]) => `${route}:${count}`).join("  ") || "-"}</Text>
        </div>
        <Table rowKey="id" columns={columns} dataSource={traces} loading={loading} size="middle" pagination={{ pageSize: 8 }} />
      </section>
    </div>
  );
}
