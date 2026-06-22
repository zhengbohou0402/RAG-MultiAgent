import { useCallback, useEffect, useState } from "react";
import { Button, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { DollarOutlined, ReloadOutlined, WalletOutlined } from "@ant-design/icons";
import { api } from "../api/client";
import type { BillingSummary, InvoiceRecord } from "../api/client";
import StatsCard from "../components/StatsCard";
import { useAuth } from "../context/useAuth";
import { useI18n } from "../i18n";

const { Text } = Typography;

export default function Billing() {
  const { user } = useAuth();
  const { language } = useI18n();
  const zh = language === "zh";
  const [summary, setSummary] = useState<BillingSummary | null>(null);
  const [invoices, setInvoices] = useState<InvoiceRecord[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [summaryData, invoiceData] = await Promise.all([
        api.billing.summary(),
        api.billing.invoices(),
      ]);
      setSummary(summaryData);
      setInvoices(invoiceData);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const columns: TableColumnsType<InvoiceRecord> = [
    { title: zh ? "发票号" : "Invoice No.", dataIndex: "invoice_no", key: "invoice_no" },
    { title: zh ? "金额" : "Amount", dataIndex: "amount", key: "amount" },
    {
      title: zh ? "状态" : "Status",
      dataIndex: "status",
      key: "status",
      render: (status: string) => <Tag color={status === "PAID" ? "green" : "orange"}>{status}</Tag>,
    },
    { title: zh ? "开票日期" : "Issued At", dataIndex: "issued_at", key: "issued_at" },
  ];

  return (
    <div className="admin-shell">
      <div className="admin-header">
        <div>
          <Text type="secondary" className="page-kicker">Tenant Billing</Text>
          <h1>{zh ? "租户账单中心" : "Tenant Billing Center"}</h1>
          <p>{zh
            ? "展示 MySQL 模拟账单、订单与发票数据，Finance_Order_Agent 使用同一套租户上下文查询。"
            : "MySQL-backed demo billing, order, and invoice data, queried by Finance_Order_Agent with the same tenant context."}
          </p>
        </div>
        <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>{zh ? "刷新" : "Refresh"}</Button>
      </div>

      <section className="admin-section">
        <div className="stats-grid">
          <StatsCard label={zh ? "租户" : "Tenant"} value={user?.tenantId || summary?.tenant_id || "-"} sub={user?.displayName} accent="blue" />
          <StatsCard label={zh ? "账期" : "Billing Month"} value={summary?.billing_month || "-"} sub={summary?.account_id} accent="purple" />
          <StatsCard label={zh ? "本月消费" : "Monthly Cost"} value={summary?.total_cost || "-"} sub={summary?.top_product} accent="green" />
          <StatsCard label={zh ? "待支付" : "Unpaid"} value={summary?.unpaid_amount || "-"} sub={zh ? "未支付金额" : "unpaid amount"} accent="amber" />
        </div>
      </section>

      <section className="admin-panel billing-summary-panel">
        <div className="panel-title-row">
          <Text strong><WalletOutlined /> {zh ? "费用优化建议" : "Cost Optimization"}</Text>
          <Tag color="blue">Finance_Order_Agent</Tag>
        </div>
        <p>{summary?.recommendation || (zh ? "暂无账单建议" : "No billing recommendation yet.")}</p>
      </section>

      <section className="admin-panel">
        <div className="panel-title-row">
          <Text strong><DollarOutlined /> {zh ? "发票列表" : "Invoices"}</Text>
          <Text type="secondary">{invoices.length} {zh ? "条记录" : "records"}</Text>
        </div>
        <Table rowKey="id" columns={columns} dataSource={invoices} loading={loading} pagination={false} size="middle" />
      </section>
    </div>
  );
}
