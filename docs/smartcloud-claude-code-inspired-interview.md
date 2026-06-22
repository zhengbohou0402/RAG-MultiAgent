# Claude Code-Inspired Interview Questions for SmartCloud

This guide is adapted from the supplied Claude Code interview-question outline. It does **not** claim SmartCloud is Claude Code or a coding Agent. Instead, it explains the shared engineering ideas and maps them honestly to the current Java multi-Agent customer-support project.

## A safe opening answer

> Claude Code is a coding-Agent harness: it combines a model with repository tools, permission control, context management, task planning, and an execution loop. My SmartCloud project is not a coding Agent. It borrows the same engineering principles for cloud-service support: bounded workflows, explicit tools, context limits, traceability, and controlled multi-Agent routing. Its focus is customer-support tasks, RAG, business tools, MCP/A2A demos, and SSE output.

---

# 1. Agent Harness and Workflow

## Q1. What is an Agent Harness? Why is Claude Code more than a model shell?

**Answer:** An Agent Harness is the engineering runtime around a model: prompt/context construction, tool registration and execution, permissions, state, retries, streaming UI, logging, and evaluation. A raw LLM only generates tokens; a harness turns those tokens into controlled work and makes the process recoverable and observable.

**SmartCloud mapping:** `ModelFactory` supplies models, `AgentWorkflowGraph` controls legal transitions, domain agents and `SmartCloudToolClient` execute capabilities, `ChatService` manages streaming/history/cache, and trace/metrics services make decisions inspectable. Together they form a small domain-specific Agent Harness.

## Q2. How is SmartCloud different from Claude Code?

**Answer:** Claude Code optimizes for repository exploration and code changes: shell/file/git/test tools and long codebase context. SmartCloud optimizes for cloud-service support: route classification, RAG, tenant-aware billing data, ICP procedures, marketing assets, and conversation SSE. Both need tools, memory boundaries, safety, and observability, but their tool permissions and evaluation criteria are different.

## Q3. Why not let one general Agent handle every SmartCloud request?

**Answer:** One general prompt would combine product knowledge, billing authorization, filing policy, marketing rules, research behavior, and all tools. That makes tool selection, testing, and least privilege harder. SmartCloud uses a supervisor to choose a specialized route, then allows only that role's bounded execution path.

**Code anchor:** `SupervisorAgent` creates `AgentDecision`; `AgentWorkflowGraph` routes it; `MultiAgentOrchestrator` dispatches the result.

## Q4. Explain the SmartCloud execution loop end to end.

**Answer:** The chat controller enters `ChatService`, which loads recent conversation history and checks an initial-turn cache. `MultiAgentOrchestrator` runs the LangGraph4j graph. The supervisor chooses a route and standalone query; the graph selects one specialist; that specialist streams an answer or uses RAG/tool data; `VerifierAgent` applies its guard; ChatService saves non-trace chunks and returns the same SSE-compatible stream to the frontend.

## Q5. Why use a graph instead of a large switch statement only?

**Answer:** A switch can dispatch today, but a graph makes states, allowed edges, route traces, conditional transitions, and future retries/review loops explicit. It is easier to test every route and explain why an execution path was legal. The graph should not own token delivery, because Reactor is better suited to streaming tokens.

**SmartCloud mapping:** LangGraph4j controls `supervisor -> selected node -> verifier -> END`; Reactor `Flux<String>` carries actual response chunks.

## Q6. When should an Agent stop using tools and return a final answer?

**Answer:** Stop when the task acceptance criteria are met, required evidence/tool data has been obtained, no tool can add material value, or a budget is reached. Every loop needs limits on steps, time, token cost, and repeated calls. On failure, return a transparent uncertainty or escalation response instead of continuing indefinitely.

**SmartCloud mapping:** Most current routes are intentionally single-pass and bounded. `WEB_REFRESH` does not crawl automatically; it falls back to indexed knowledge, avoiding uncontrolled writes or long-running web behavior.

## Q7. How should tool errors be handled in an Agent loop?

**Answer:** Normalize an error into a structured observation: tool name, safe error type, retryability, correlation ID, and user-safe message. Retry only idempotent/transient failures with bounded backoff. The Agent can choose an alternate tool or ask for clarification; the user should never see stack traces or provider secrets.

**SmartCloud mapping:** Tool/protocol calls are traced. A production hardening step is typed tool-result envelopes, timeout/retry policies, and route-specific fallback for failed billing, RAG, or A2A calls.

## Q8. What is Harness Engineering and how does it relate to Agent products?

**Answer:** Harness Engineering treats the runtime around the model as a first-class product. Model quality matters, but reliable value comes from how context is selected, tools are constrained, outcomes are validated, failures are recovered, and behavior is measured. It shifts discussion from “which model” to “how does the whole system reliably complete work.”

**SmartCloud mapping:** The project deliberately combines graph orchestration, RAG, tools, cache, auth context, traces, and SSE rather than relying on one giant prompt.

## Q9. What would you add before calling SmartCloud industrial-grade?

**Answer:** I would add a labeled evaluation suite, source/claim verification, robust RBAC at every tool/data boundary, durable outbox/Saga compensation, production Redis/MySQL/Mongo deployment, rate limits and quotas, timeout/retry/circuit-breaker policy, secret management, and load testing. The current version is an interview-ready local demo with clear extension seams, not a claim of completed high-availability production operations.

## Q10. How would you design a Claude Code-like coding Agent separately from SmartCloud?

**Answer:** It needs repository discovery, `rg`/glob/file-read tools, AST/symbol navigation, isolated command execution, diff/apply-patch, test runners, git-aware rollback, permissions, plan/review modes, and task checkpoints. RAG can assist documentation search, but exact code navigation should primarily use paths, symbols, references, and grep-like tools.

**SmartCloud relationship:** This is a different product. The transferable parts are bounded tools, plan-before-act, context budgets, traces, and review gates.

---

# 2. Context, Memory, and Retrieval

## Q11. Why does an Agent exhaust context faster than ordinary chat?

**Answer:** An Agent accumulates system instructions, user messages, tool schemas, tool inputs/outputs, retrieved documents, intermediate plans, errors, and progress state. Each tool loop expands the transcript, so the context budget is consumed much faster than in simple question-answer chat.

**SmartCloud mapping:** A support request can include recent messages, the routing prompt, retrieved RAG chunks, tool outputs, and streamed response metadata. This is why context must be intentionally bounded.

## Q12. How does SmartCloud manage short-term context today?

**Answer:** `ConversationMessageMapper` converts stored messages to LangChain4j messages and only reuses the most recent 20 messages for supervisor context. The supervisor then rewrites a follow-up into a standalone query. This controls cost and prevents stale dialogue from overwhelming retrieval.

**Boundary:** It is a sliding-window baseline, not a complete compact/summarization system yet.

## Q13. Why is a bigger context window not a complete memory solution?

**Answer:** Bigger context increases cost and latency, allows irrelevant or contradictory history to distract the model, and still has finite capacity. It also suffers from “lost in the middle”: important facts can become less salient among long unrelated content. Good memory requires selection, compression, provenance, freshness, and permissions.

## Q14. How would you evolve SmartCloud from a window to layered memory?

**Answer:** Keep a recent-turn window for working memory; summarize older resolved discussion with links to source turns; extract only verified, user-approved durable preferences into structured long-term memory; keep domain knowledge in RAG; retrieve memory by relevance plus tenant/user permissions. Every record should include source, confidence, timestamp, TTL, and deletion policy.

## Q15. What is context compaction? How would you implement it?

**Answer:** Compaction reduces the token footprint while preserving task-critical state. Use different tiers: truncate low-value raw turns, summarize completed episodes, persist large tool outputs externally and keep a short reference/preview in context, and retrieve detailed data only when needed. Keep the original audit record unchanged; generate a read-time projection for the model.

**SmartCloud mapping:** For long deep-research tasks, store raw research artifacts in Mongo/object storage, keep a plan/evidence summary in graph state, and rehydrate only the relevant evidence on the next step.

## Q16. Why should large tool results stay outside the prompt?

**Answer:** Large raw results burn tokens and hide the important values. Store the full result with an ID, include a compact schema-aware summary in context, and let the Agent fetch a filtered slice when necessary. This preserves auditability without making every later decision slower or noisier.

**SmartCloud mapping:** Full traces/tool data belong in trace persistence; the user stream should contain only useful final content plus a concise route trace.

## Q17. Why is codebase retrieval usually grep/symbol-first rather than RAG-first?

**Answer:** Code questions often depend on exact identifiers, imports, call sites, file paths, and current syntax. Grep/symbol tools provide precise, fresh, navigable results; vector retrieval can blur identifiers, split dependencies across chunks, and become stale after edits. RAG is useful for semantic documentation, architecture notes, and historical incidents, not as the only code-navigation primitive.

**SmartCloud mapping:** This project uses RAG for support knowledge, not for locating Java symbols. If a code-assistant feature is later added, it should combine `rg`, AST/symbol lookup, and targeted file reads.

## Q18. How do cache and memory differ in SmartCloud?

**Answer:** Memory preserves relevant state or knowledge for future reasoning; cache accelerates reuse of a previously computed result. Memory can change an Agent decision, while cache should only reuse a safe response under compatible context. Cache keys need tenant, model, region, index version, normalized query, and TTL boundaries.

**SmartCloud mapping:** `SemanticCacheService` uses a tenant-aware namespace and can use Redis L1 with a local semantic fallback. Recent conversation and RAG corpus serve memory-like roles, not just speed optimization.

---

# 3. Tools, Skills, Hooks, MCP, and Safety

## Q19. What is the difference between a Tool, a Skill, and an Agent?

**Answer:** A tool is a bounded operation with typed input/output. A skill is reusable procedure/policy describing how to solve a recurring task, often using several tools. An Agent selects and coordinates skills/tools toward a goal. Keeping these layers separate prevents a “tool” from becoming an untestable mini-application hidden behind a vague prompt.

**SmartCloud mapping:** `billing.query` is a tool; an ICP filing process can become a skill; `IcpServiceAgent` is the role that applies the relevant procedure and tool set.

## Q20. How would you define an effective Skill for SmartCloud?

**Answer:** A skill should include its scope, prerequisites, allowed tools, input/output schema, decision rules, examples, stop/escalation conditions, safety constraints, and evaluation cases. Version it like code, keep it concise, and load it only when routing selects that domain.

**Example:** An “ICP filing” skill would include eligibility checks, required material checklist, explicit uncertainty wording, and a prohibition on claiming approval status without an authoritative tool result.

## Q21. What is a Hook and how is it different from a tool?

**Answer:** A hook is lifecycle automation triggered before or after an action, while a tool is a capability the Agent intentionally selects. Hooks enforce invariants and automation such as formatting after edits, blocking sensitive-file writes, or emitting telemetry. Tools perform task-level work such as running a search or querying a bill.

**SmartCloud mapping:** The current project does not implement an Agent hook runtime. In a production pipeline, hooks could block tool calls with missing tenant context, redact PII in traces, validate output schemas, and record audit events.

## Q22. How do you make tool calls safe?

**Answer:** Use allowlisted tools and typed schemas; validate arguments server-side; enforce tenant/user authorization at the data boundary; set timeouts, rate limits, and idempotency rules; redact sensitive logs; avoid model-generated SQL or shell execution; require user confirmation for irreversible actions; and audit all calls. Treat tool descriptions and retrieved text as prompt-injection targets.

**SmartCloud mapping:** Domain routes limit capabilities; MCP tool names are explicitly enumerated in `McpController`; user context is passed through the Agent chain. More rigorous RBAC and policy enforcement remain a production priority.

## Q23. Explain MCP in one minute and describe what SmartCloud actually implements.

**Answer:** MCP is a standard client-server protocol that lets AI hosts discover and invoke external tools, resources, and prompts through a common contract. SmartCloud implements the core tool portion with a JSON-RPC-style `/mcp` endpoint: `initialize`, `tools/list`, and `tools/call`, plus tool definitions for billing, ICP, marketing, research planning, and H5 generation.

**Boundary:** It is a locally runnable protocol demo. It should not be described as a complete, fully interoperable MCP platform with every session, authorization, and transport edge case implemented.

## Q24. Why use MCP rather than hard-code all integrations into an Agent?

**Answer:** MCP decouples capability providers from Agent hosts, enabling discovery, reuse, independent deployment, and language/framework freedom. Hard-coded internal tools are simpler and lower-latency for a small service. The choice depends on whether the capability must be reused outside one application and independently owned.

**SmartCloud mapping:** Internal Java tool services make sense for local demo reliability; the MCP facade provides a reusable integration boundary for future external Agent hosts.

## Q25. What is A2A and how does SmartCloud use it?

**Answer:** A2A is agent-to-agent delegation: an agent advertises its capabilities and accepts a task from another agent, returning task status and artifacts. SmartCloud exposes an Agent Card at `/.well-known/agent-card.json`, a request endpoint at `/message:send`, and an SSE stream at `/message:stream`. Marketing obtains product context through the A2A-shaped path before generating content.

## Q26. MCP versus A2A: when do you use each?

**Answer:** MCP is best for a host/agent to call a tool or retrieve a resource from a capability server. A2A is best when the remote side is itself an autonomous/specialized Agent that owns planning, state, status, and artifacts. They can compose: an A2A agent can internally use MCP tools.

**SmartCloud mapping:** `billing.query` is naturally MCP/tool-shaped; asking a product specialist to return a recommendation artifact is naturally A2A-shaped.

## Q27. How would you protect a system from prompt injection through RAG or MCP tools?

**Answer:** Treat retrieved documents, tool results, and remote agent text as untrusted data, not instruction. Separate trusted system policy from untrusted content; do not concatenate raw content into privileged prompts; validate tool arguments and authorization in code; minimize tool permissions; require confirmation for side effects; and log/review suspicious tool-selection patterns.

## Q28. What is an LLM gateway, and do you have one?

**Answer:** An LLM gateway centralizes provider routing, authentication, quotas, retries, caching, budgets, safety policy, and observability. SmartCloud currently centralizes model setup through `ModelFactory` and configuration for DashScope, but it does not deploy a separate gateway product. The next step would be an internal/provider gateway with route-level cost, latency, fallback, and tenant quota controls.

---

# 4. Multi-Agent, Isolation, and Coordination

## Q29. What is a SubAgent? Is every SmartCloud route a SubAgent?

**Answer:** A SubAgent is usually a separately scoped Agent instance with its own context, instructions, tools, and result contract, created by a parent/coordinator. Not every route is necessarily an independent SubAgent. In SmartCloud, the route nodes are specialized execution roles in one graph; some are LLM-backed, some are deterministic tool-backed. Calling them all fully autonomous independent SubAgents would be inaccurate.

## Q30. Why isolate SubAgent context instead of sharing the full parent transcript?

**Answer:** Isolation reduces irrelevant context, keeps role instructions focused, protects sensitive data, limits prompt injection spread, and lowers token cost. The parent should send a minimal task packet and receive a typed result/summary, not expose every internal thought or unrelated history.

**SmartCloud mapping:** `AgentDecision` provides a clean handoff contract: route, standalone query, and reason. User/tenant context should be passed only when that agent/tool needs it.

## Q31. How should Agents communicate: shared memory or result messages?

**Answer:** Use result messages as the default: typed artifact, source references, status, uncertainty, and next-action recommendation. Use shared storage only for durable artifacts with explicit ownership and concurrency control. Fully shared free-form context creates hidden coupling, conflicts, and prompt contamination.

**SmartCloud mapping:** A2A result artifacts and trace records are better communication boundaries than a global mutable conversation blob.

## Q32. How do you avoid conflicts when Agents run concurrently?

**Answer:** Give every agent clear ownership of its output, use immutable task inputs and correlation IDs, define idempotency for side-effect tools, use locks/version checks for shared records, and aggregate only typed results. The coordinator controls dependencies and cancellation; independent read-only retrieval can run in parallel, but writes need stronger ordering.

## Q33. How do you decide whether a request needs one Agent or multiple Agents?

**Answer:** Start with one if the task has one domain, one tool set, and one acceptance criterion. Split only when subtasks have different expertise, separate permissions/data sources, parallelizable evidence collection, or independently testable quality criteria. Every new Agent adds route cost, coordination complexity, and an evaluation burden.

**SmartCloud mapping:** Billing should remain a bounded finance route. A long architecture report may justify product, cost, compliance, and research sub-agents coordinated by a plan.

## Q34. How does dynamic route switching work in SmartCloud and how would you improve it?

**Answer:** The supervisor returns a route, standalone query, and reason; LangGraph4j follows an allowlisted conditional edge; the selected agent runs; then the verifier completes the path. Improvements are confidence scoring, clarification on ambiguous requests, route-specific failure handling, a bounded fallback/re-route edge, and offline route confusion-matrix evaluation.

## Q35. Why is a coordinator/supervisor useful in multi-Agent systems?

**Answer:** The coordinator owns task decomposition, agent selection, budget, dependency ordering, result aggregation, and stop/fallback decisions. Without it, peer agents can duplicate work, conflict, or loop. The coordinator should be deterministic where possible and preserve a trace of every handoff.

**SmartCloud mapping:** `SupervisorAgent` makes the semantic decision; `AgentWorkflowGraph` is the deterministic coordinator that constrains legal execution paths.

## Q36. How would you design a code-review SubAgent, and what is transferable to SmartCloud?

**Answer:** A review SubAgent should receive a scoped diff, repository rules, threat/test checklist, and strict output schema: severity, file/line, evidence, risk, and test gap. It should not modify code during review. The transferable idea is independent verification with a structured output contract.

**SmartCloud mapping:** A future verifier can receive answer text plus retrieved sources/tool outputs and return claim-level grounding gaps, citation coverage, policy risk, and a revise/abstain decision.

---

# 5. Integrated Scenario Questions

## Q37. Design the full workflow for a new SmartCloud capability: “recommend a cloud deployment plan.”

**Answer:** Clarify workload, traffic, data sensitivity, region, budget, and availability targets. Route to deep research/product technology. Create a bounded plan with compute, storage, network, security, cost, and compliance subtasks. Retrieve product documentation and call authorized pricing/catalog tools. Synthesize trade-offs with assumptions and evidence. Verify that each concrete product/cost claim has a source/tool result, stream the answer, persist the trace, and offer human escalation for ambiguous requirements.

## Q38. A long Agent task starts giving inconsistent answers. How do you diagnose it?

**Answer:** First inspect trace: which route, model, prompt version, history window, retrieved chunks, tool results, and failures were present? Then separate causes: ambiguous requirements, stale/irrelevant context, retrieval miss, tool-data inconsistency, or model generation error. Reproduce with the same correlation ID and input snapshot. Fix the earliest failing layer rather than only adding prompt text.

## Q39. How would you handle sensitive configuration and tenant data in an Agent system?

**Answer:** Keep secrets outside prompts and source control, use secret managers/environment injection, redact logs/traces, apply least privilege and tenant-scoped authorization to every tool/data query, define retention/deletion policy, and prohibit agents from exposing raw credentials or cross-tenant results. Safety must be enforced in code and infrastructure, not trusted to prompts.

**SmartCloud mapping:** JWT resolves `SmartCloudUserContext`; cache namespace and tool calls can use tenant/user IDs. Production needs full RBAC and tests proving cross-tenant isolation.

## Q40. What are the most valuable Claude Code ideas to borrow for SmartCloud?

**Answer:** Five ideas transfer directly: plan before expensive action; keep contexts scoped and compact; use precise tools and typed results; isolate specialized workers and aggregate summaries; make execution observable and reversible where possible. I would not copy code-search-specific behavior into a customer-support system; the domain decides the toolset and evaluation method.

## Q41. If an interviewer asks “what is missing compared with Claude Code?”, how do you answer?

**Answer:** Claude Code has mature repository tools, granular permission UX, long-task context compaction, task resumption, code-aware retrieval, and a rich autonomous coding loop. SmartCloud intentionally has a narrower domain: it focuses on cloud support/RAG/business tools. Its next maturity steps are evaluation suites, stronger verifier grounding, durable workflow state, production protocol/security hardening, and richer memory management.

## Q42. Give a two-minute project story using these Agent-engineering concepts.

**Answer:** “I evolved a single React-style RAG Agent into a Java multi-Agent support harness. A supervisor uses the recent conversation to produce a structured route and standalone query. LangGraph4j constrains the route graph, and the selected specialist either calls RAG, a business-tool boundary, or an A2A-shaped product service. Reactor/WebFlux remains responsible for token-level SSE streaming, so graph control flow does not block user output. I added tenant-aware cache namespaces, JWT demo context, MCP-style tool discovery/invocation, traces, and metrics. The important engineering trade-off is that it is a controlled, observable demo rather than an unconstrained autonomous swarm; next I would add source-level verification, durable state, and evaluation data before claiming production readiness.”

---

# Final interview reminders

1. Do not say SmartCloud “uses Claude Code.” Say it borrows general Agent-engineering ideas.
2. Do not call every graph route an autonomous LLM SubAgent. Several routes are controlled tool-backed roles.
3. Do not claim automatic context compaction, production A2A interoperability, or complete MCP compliance; describe them as roadmap items.
4. Anchor answers in real classes: `ChatService`, `MultiAgentOrchestrator`, `AgentWorkflowGraph`, `SupervisorAgent`, `RagService`, `VerifierAgent`, `SmartCloudTraceService`.
5. When asked about a missing capability, explain its production design rather than bluffing that it is implemented.
