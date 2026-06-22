import { invoke } from "@tauri-apps/api/core";

let _baseUrl: string | null = null;
export const AUTH_TOKEN_KEY = "smartcloud_auth_token";

async function getBaseUrl(): Promise<string> {
  if (_baseUrl) return _baseUrl;

  try {
    // Desktop mode: ask the Tauri host which backend port it started.
    _baseUrl = await invoke<string>("get_server_url");
  } catch {
    const location = window.location;
    const isViteDevServer = ["5173", "5174"].includes(location.port);
    _baseUrl = isViteDevServer ? "http://127.0.0.1:8000" : location.origin;
  }
  return _baseUrl;
}

export async function resolveBackendUrl(path: string): Promise<string> {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  const base = await getBaseUrl();
  return `${base}${path}`;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const base = await getBaseUrl();
  const headers = new Headers(options?.headers);
  headers.set("Content-Type", "application/json");
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(`${base}${path}`, {
    ...options,
    headers,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error((body as { detail?: string }).detail || res.statusText);
  }
  return res.json();
}

export interface Conversation {
  id: string;
  title: string;
  updated_at: number;
  messages?: Message[];
}

export interface Message {
  role: "user" | "assistant";
  content: string;
  sources?: Source[];
  thinking?: string[];
}

export interface Source {
  file: string;
  chunk_index: number;
  excerpt: string;
  source_type?: string;
}

export interface Document {
  name: string;
  size: number;
  modified: number;
  indexed?: boolean;
  stale?: boolean;
  chunks?: number;
  source_type?: string | null;
  source_trust_label?: string | null;
  indexed_at?: string | null;
  covered_by?: string | null;
  index_note?: string | null;
}

export interface TrainingStatus {
  running: boolean;
  pending: boolean;
  last_result: string | null;
  last_error: string | null;
}

export interface KnowledgeStats {
  document_count: number;
  manifest_records: number;
  total_chunks: number;
  last_indexed: string | null;
  cache_entries: number;
  index_version: number;
  index_updated_at: string | null;
  index_last_error: string | null;
  source_type_counts: Record<string, number>;
  dense_points: number;
  lexical_documents: number;
  manifest_chunks: number;
  qdrant_collection: string;
  lexical_generation: string | null;
  index_generation: string | null;
  index_consistent: boolean;
}

export interface CacheStats {
  hit_count: number;
  miss_count: number;
  hit_rate: number;
  size: number;
  valid: number;
  threshold: number;
}

export interface SchedulerStatus {
  enabled: boolean;
  manual_available: boolean;
  interval_hours: number;
  max_pages: number;
  last_run: string | null;
  next_run: string | null;
  running: boolean;
  mode: "scheduled" | "manual" | null;
  phase: "idle" | "crawling" | "indexing";
  last_success: string | null;
  last_attempt: string | null;
  last_error: string | null;
  last_output_file: string | null;
  pages_crawled: number;
  pages_visited: number;
  pages_skipped: number;
  pages_failed: number;
  crawler_engine: string | null;
  current_url: string | null;
  browser_enabled: boolean;
  browser_auto_download: boolean;
  static_fallback: boolean;
  static_tls_fallback: boolean;
  thread_alive: boolean;
}

export interface Settings {
  dashscope_api_key: string;
  dashscope_base_url: string;
  chat_model_name: string;
}

export interface ModelsResponse {
  models: string[];
  key_valid: boolean | null;
  key_error: string;
  region: string;
}

export interface SmartCloudUser {
  tenantId: string;
  userId: string;
  username: string;
  displayName: string;
  role: string;
}

export interface LoginResponse {
  token: string;
  user: SmartCloudUser;
  expires_at: number;
}

export interface BillingSummary {
  account_id: string;
  tenant_id: string;
  billing_month: string;
  total_cost: string;
  unpaid_amount: string;
  top_product: string;
  recommendation: string;
}

export interface InvoiceRecord {
  id: string;
  invoice_no: string;
  amount: string;
  status: string;
  issued_at: string;
}

export interface TraceRecord {
  id: string;
  conversation_id: string;
  tenant_id: string;
  user_id: string;
  route: string;
  selected_agent: string;
  reason: string;
  nodes: string[];
  tool_calls: Array<Record<string, unknown>>;
  created_at: number;
  latency_ms: number;
}

export interface MarketingAsset {
  id: string;
  product_name: string;
  headline: string;
  landing_page_url: string;
  poster_url: string;
  campaign_copy: string;
}

export interface McpResponse {
  jsonrpc: "2.0";
  id: string;
  result?: unknown;
  error?: { code: number; message: string };
}

export interface AgentCard {
  name: string;
  description: string;
  url: string;
  version: string;
  capabilities: Record<string, unknown>;
  skills: Array<Record<string, unknown>>;
}

export interface DocumentChunk {
  chunk_id: string | null;
  chunk_index: number;
  text: string;
  source_type: string | null;
  source_trust_label: string | null;
}

export async function streamChat(
  message: string,
  conversationId: string | null,
  signal?: AbortSignal
): Promise<{ reader: ReadableStreamDefaultReader<Uint8Array>; conversationId: string }> {
  const base = await getBaseUrl();
  const headers = new Headers({ "Content-Type": "application/json" });
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(`${base}/api/chat`, {
    method: "POST",
    headers,
    body: JSON.stringify({ message, conversation_id: conversationId }),
    signal,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error((body as { detail?: string }).detail || res.statusText);
  }
  const cid = res.headers.get("X-Conversation-Id") || "";
  return { reader: res.body!.getReader(), conversationId: cid };
}

export const api = {
  health: () => request<{ status: string }>("/api/health"),
  configStatus: () => request<{ dashscope_configured: boolean }>("/api/config/status"),
  chat: streamChat,

  auth: {
    login: (data: { username: string; password: string; tenant_id?: string }) =>
      request<LoginResponse>("/api/auth/login", { method: "POST", body: JSON.stringify(data) }),
    me: () => request<SmartCloudUser>("/api/auth/me"),
    demo: () => request<{ username: string; password: string; tenant_id: string }>("/api/auth/demo"),
  },

  billing: {
    summary: () => request<BillingSummary>("/api/billing/summary"),
    invoices: () => request<InvoiceRecord[]>("/api/billing/invoices"),
  },

  traces: {
    list: () => request<TraceRecord[]>("/api/traces"),
    byConversation: (conversationId: string) => request<TraceRecord[]>(`/api/traces/${conversationId}`),
  },

  marketing: {
    generate: (data: { product_name: string; scenario?: string; audience?: string }) =>
      request<MarketingAsset>("/api/marketing/generate", { method: "POST", body: JSON.stringify(data) }),
  },

  mcp: {
    call: (method: string, params?: Record<string, unknown>) =>
      request<McpResponse>("/mcp", {
        method: "POST",
        body: JSON.stringify({
          jsonrpc: "2.0",
          id: `web-${Date.now()}`,
          method,
          params: params ?? {},
        }),
      }),
  },

  a2a: {
    card: () => request<AgentCard>("/.well-known/agent-card.json"),
    send: (query: string) =>
      request<McpResponse>("/message:send", {
        method: "POST",
        body: JSON.stringify({
          jsonrpc: "2.0",
          id: `a2a-${Date.now()}`,
          method: "message/send",
          params: {
            message: {
              role: "user",
              parts: [{ kind: "text", text: query }],
            },
          },
        }),
      }),
  },

  // Conversations
  conversations: {
    list: () => request<{ items: Conversation[] }>("/api/conversations"),
    create: () => request<Conversation>("/api/conversations", { method: "POST" }),
    get: (id: string) => request<Conversation>(`/api/conversations/${id}`),
    delete: (id: string) => request<{ ok: boolean }>(`/api/conversations/${id}`, { method: "DELETE" }),
    deleteAll: () => request<{ ok: boolean }>("/api/conversations", { method: "DELETE" }),
  },

  // Settings
  settings: {
    get: () => request<Settings>("/api/settings"),
    save: (data: { dashscope_api_key: string; dashscope_base_url: string; chat_model_name: string }) =>
      request<{ ok: boolean }>("/api/settings", { method: "POST", body: JSON.stringify(data) }),
  },
  prompt: {
    get: () => request<{ system_prompt: string }>("/api/prompt"),
    save: (systemPrompt: string) =>
      request<{ ok: boolean }>("/api/prompt", {
        method: "POST",
        body: JSON.stringify({ system_prompt: systemPrompt }),
      }),
  },
  models: (region?: string) =>
    request<ModelsResponse>(`/api/models${region ? `?list_region=${region}` : ""}`),

  // Documents
  documents: {
    list: () => request<{ documents: Document[] }>("/api/documents"),
    upload: async (files: FileList) => {
      const base = await getBaseUrl();
      const fd = new FormData();
      for (const f of Array.from(files)) fd.append("files", f);
      const response = await fetch(`${base}/api/upload`, { method: "POST", body: fd });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error((body as { detail?: string }).detail || response.statusText);
      }
      return response.json() as Promise<{
        saved: string[];
        errors: Array<{ name: string; error: string }>;
        training_started: boolean;
      }>;
    },
    delete: (filename: string) =>
      request<{ ok: boolean }>(`/api/documents/${encodeURIComponent(filename)}`, { method: "DELETE" }),
    chunks: (filename: string) =>
      request<{ filename: string; chunks: DocumentChunk[]; count: number }>(
        `/api/document-chunks?filename=${encodeURIComponent(filename)}`
      ),
  },

  // Training
  training: {
    status: () => request<TrainingStatus>("/api/training/status"),
    start: () => request<{ started: boolean; message: string }>("/api/training/start", { method: "POST" }),
  },

  // Stats
  knowledge: {
    stats: () => request<KnowledgeStats>("/api/knowledge/stats"),
    update: (reindex = true) => request<{ started: boolean; message: string; max_pages: number; reindex: boolean }>(
      `/api/knowledge/update?reindex=${reindex ? "true" : "false"}`,
      { method: "POST" }
    ),
  },
  cache: { stats: () => request<CacheStats>("/api/cache/stats") },
  scheduler: { status: () => request<SchedulerStatus>("/api/scheduler/status") },
};
