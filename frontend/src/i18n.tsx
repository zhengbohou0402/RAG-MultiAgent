import { createContext, useContext, useMemo, useState } from "react";
import type { ReactNode } from "react";

export type Language = "zh" | "en";

const LANG_KEY = "smartcloud_language";

const messages = {
  zh: {
    "app.boot": "正在进入 SmartCloud 控制台...",
    "nav.chat": "智能客服",
    "nav.billing": "租户账单",
    "nav.mcp": "MCP 工具",
    "nav.a2a": "A2A 演示",
    "nav.marketing": "营销素材",
    "nav.observability": "观测 Trace",
    "nav.knowledge": "知识库",
    "nav.dashboard": "运营看板",
    "nav.settings": "系统设置",
    "nav.prompts": "提示词",
    "nav.brand": "云客服平台",
    "nav.defaultUser": "演示租户",
    "nav.light": "浅色模式",
    "nav.dark": "深色模式",
    "nav.logout": "退出登录",
    "login.kicker": "SmartCloud 云客服",
    "login.title": "工业级智能服务平台",
    "login.desc": "面向云服务咨询、账单查询、备案支持、营销推广和深度研究的多 Agent 工作台。",
    "login.panelTitle": "租户登录",
    "login.panelHint": "默认演示账号可直接进入系统。",
    "login.username": "账号",
    "login.password": "密码",
    "login.tenant": "租户",
    "login.usernameRequired": "请输入账号",
    "login.passwordRequired": "请输入密码",
    "login.tenantRequired": "请输入租户 ID",
    "login.submit": "进入控制台",
    "login.success": "登录成功，已进入租户工作台",
    "login.failed": "登录失败",
    "chat.title": "SmartCloud 智能服务台",
    "chat.newCase": "新建服务会话",
    "chat.tag.mcp": "MCP 工具",
    "chat.badge": "7x24h AI 服务台",
    "chat.welcomeTitle": "SmartCloud 工业级智能服务平台",
    "chat.welcomeDesc": "一个编排器把用户请求路由给产品技术、财务订单、备案服务、营销运营和深度研究等专业 Agent。",
    "chat.metric.agents": "专业 Agent",
    "chat.metric.retrieval": "检索引擎",
    "chat.metric.workflow": "LangGraph 工作流",
    "chat.metric.streaming": "流式响应",
    "agent.product": "ECS、GPU、数据库、存储、网络",
    "agent.finance": "账单、发票、订单、续费成本",
    "agent.icp": "备案材料、状态、流程风险",
    "agent.marketing": "活动文案、海报 brief、落地页",
    "agent.research": "技术报告与方案对比",
    "composer.placeholder": "咨询产品、账单、备案、营销或深度研究问题...",
    "composer.disclaimer": "SmartCloud 可能出错，账单、备案和生产操作请以官方系统为准。",
    "suggest.ecs": "创建 ECS",
    "suggest.gpu": "GPU 活动",
    "suggest.billing": "账单查询",
    "suggest.invoice": "开具发票",
    "suggest.icp": "备案流程",
    "suggest.poster": "营销海报",
    "suggest.research": "深度研究",
    "suggest.refresh": "刷新知识库",
    "suggest.ecsPrompt": "如何为生产 Web 应用创建一台 ECS 云服务器实例？",
    "suggest.gpuPrompt": "我有大模型部署需求，需要 GPU 云资源，有没有合适的活动或产品推荐？",
    "suggest.billingPrompt": "请帮我查询当前账单、未支付金额和主要成本来源。",
    "suggest.invoicePrompt": "这个月的云服务订单应该如何开具发票？",
    "suggest.icpPrompt": "我的域名需要备案，需要准备哪些材料和步骤？",
    "suggest.posterPrompt": "云服务器 ECS 标准型 2 核 4G，帮我生成推广文案和海报 brief。",
    "suggest.researchPrompt": "我准备做 Agentic RAG 应用，请给我一份技术选型报告。",
    "suggest.refreshPrompt": "回答云产品问题前，请刷新最新网站知识。",
    "apiKey.title": "配置 DashScope API Key",
    "apiKey.desc": "请输入阿里云百炼 DashScope API Key，用于 SmartCloud 的聊天、检索和多 Agent 回复。密钥只会保存到本地 .env 文件。",
    "apiKey.key": "API Key",
    "apiKey.region": "区域",
    "apiKey.china": "中国站",
    "apiKey.intl": "国际站",
    "apiKey.baseUrl": "Base URL（国际站）",
    "apiKey.save": "保存并继续",
    "apiKey.fullSettings": "如需选择模型、验证密钥或更多配置，请打开完整设置",
    "apiKey.openSettings": "完整设置",
    "apiKey.required": "请输入 DashScope API Key。",
    "apiKey.saved": "已保存，可以开始使用。",
    "apiKey.failed": "保存失败，请检查连接后重试。",
  },
  en: {
    "app.boot": "Entering SmartCloud console...",
    "nav.chat": "AI Service",
    "nav.billing": "Billing",
    "nav.mcp": "MCP Tools",
    "nav.a2a": "A2A Demo",
    "nav.marketing": "Marketing",
    "nav.observability": "Traces",
    "nav.knowledge": "Knowledge",
    "nav.dashboard": "Dashboard",
    "nav.settings": "Settings",
    "nav.prompts": "Prompts",
    "nav.brand": "Cloud Service",
    "nav.defaultUser": "Demo tenant",
    "nav.light": "Light mode",
    "nav.dark": "Dark mode",
    "nav.logout": "Sign out",
    "login.kicker": "SmartCloud ServiceOps",
    "login.title": "Industrial Intelligent Service Platform",
    "login.desc": "A multi-agent workspace for cloud support, billing, ICP filing, marketing, and deep research.",
    "login.panelTitle": "Tenant Login",
    "login.panelHint": "Use the default demo account to enter the console.",
    "login.username": "Username",
    "login.password": "Password",
    "login.tenant": "Tenant",
    "login.usernameRequired": "Please enter username",
    "login.passwordRequired": "Please enter password",
    "login.tenantRequired": "Please enter tenant ID",
    "login.submit": "Enter Console",
    "login.success": "Signed in successfully",
    "login.failed": "Login failed",
    "chat.title": "SmartCloud ServiceOps Console",
    "chat.newCase": "New service case",
    "chat.tag.mcp": "MCP Tools",
    "chat.badge": "7x24h AI service desk",
    "chat.welcomeTitle": "SmartCloud Intelligent Service Platform",
    "chat.welcomeDesc": "One orchestrator routes requests to specialist agents for product support, finance, ICP filing, marketing operations, and deep research.",
    "chat.metric.agents": "specialist agents",
    "chat.metric.retrieval": "retrieval engines",
    "chat.metric.workflow": "LangGraph workflow",
    "chat.metric.streaming": "streaming response",
    "agent.product": "ECS, GPU, database, storage, networking",
    "agent.finance": "billing, invoices, orders, renewal costs",
    "agent.icp": "ICP filing materials, status, procedure risks",
    "agent.marketing": "campaign copy, poster brief, landing page",
    "agent.research": "technical reports and solution comparison",
    "composer.placeholder": "Ask about products, bills, ICP filing, marketing, or deep research...",
    "composer.disclaimer": "SmartCloud can make mistakes. Verify billing, filing, and production operations through official systems.",
    "suggest.ecs": "Create ECS",
    "suggest.gpu": "GPU Campaign",
    "suggest.billing": "Billing",
    "suggest.invoice": "Invoice",
    "suggest.icp": "ICP Filing",
    "suggest.poster": "Marketing Poster",
    "suggest.research": "Deep Research",
    "suggest.refresh": "Refresh KB",
    "suggest.ecsPrompt": "How do I create an ECS cloud server instance for a production web app?",
    "suggest.gpuPrompt": "I need GPU cloud resources for LLM deployment. Are there any suitable activities or product recommendations?",
    "suggest.billingPrompt": "Please help me check my current bill, unpaid amount, and main cost driver.",
    "suggest.invoicePrompt": "How can I issue an invoice for this month's cloud service order?",
    "suggest.icpPrompt": "My domain needs ICP filing. What materials and steps should I prepare?",
    "suggest.posterPrompt": "Cloud server ECS standard 2-core 4G: help me generate promotion copy and a poster brief.",
    "suggest.researchPrompt": "I am planning an Agentic RAG application. Give me a technical selection report.",
    "suggest.refreshPrompt": "Refresh the latest website knowledge before answering cloud product questions.",
    "apiKey.title": "Configure DashScope API Key",
    "apiKey.desc": "Enter your Alibaba Cloud Model Studio (DashScope) API key for SmartCloud chat, retrieval, and multi-agent responses. It is stored only in your local .env file.",
    "apiKey.key": "API key",
    "apiKey.region": "Region",
    "apiKey.china": "China",
    "apiKey.intl": "International",
    "apiKey.baseUrl": "Base URL (international)",
    "apiKey.save": "Save and continue",
    "apiKey.fullSettings": "To pick a model, verify your key, and more, open full settings",
    "apiKey.openSettings": "open full settings",
    "apiKey.required": "Please enter your DashScope API key.",
    "apiKey.saved": "Saved. You're ready to go.",
    "apiKey.failed": "Could not save settings. Check your connection and try again.",
  },
} as const;

export type MessageKey = keyof typeof messages.zh;

interface I18nContextValue {
  language: Language;
  setLanguage: (language: Language) => void;
  t: (key: MessageKey) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

function readInitialLanguage(): Language {
  const saved = localStorage.getItem(LANG_KEY);
  return saved === "en" ? "en" : "zh";
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(readInitialLanguage);

  const value = useMemo<I18nContextValue>(() => {
    const setLanguage = (next: Language) => {
      localStorage.setItem(LANG_KEY, next);
      setLanguageState(next);
      document.documentElement.setAttribute("lang", next === "zh" ? "zh-CN" : "en");
    };

    return {
      language,
      setLanguage,
      t: (key) => messages[language][key] || messages.zh[key] || key,
    };
  }, [language]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

// The hook intentionally lives next to its provider to keep language access consistent.
// eslint-disable-next-line react-refresh/only-export-components
export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used inside I18nProvider");
  }
  return context;
}
