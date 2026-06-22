# SmartCloud Multi-Agent Interview Question Bank

This question bank is tailored to the current SmartCloud implementation. It is inspired by the RAG, Agent, evaluation, memory, and Agentic RAG interview sections in [AgentGuide](https://github.com/adongwanai/AgentGuide/tree/main/docs/04-interview).

Use the answers as anchors, not scripts. Be precise about the distinction between a working demo and a production-ready capability.

## 1. One-minute project introduction

> SmartCloud is a cloud-service customer-support demo built with Spring Boot WebFlux, LangChain4j, LangGraph4j, Qdrant/Lucene RAG, and a React console. A supervisor first turns the user request plus recent conversation into a route, then a LangGraph4j workflow selects one specialized agent. Product, billing, ICP filing, marketing, deep research, and knowledge-RAG requests are separated. The control plane is the graph; token output stays as Reactor `Flux<String>` and is returned through the existing SSE chat API. The project also demonstrates tenant-aware caching, JWT demo login, MCP JSON-RPC tools, A2A Agent Card/message APIs, local trace persistence, and Prometheus metrics.

## 2. Architecture and multi-agent questions

### Q1. Why did you move from a single ReactAgent to multi-agent orchestration?

**Answer anchors:** A single prompt/tool loop mixes routing, domain expertise, tool selection, and response generation. It becomes difficult to test, observe, and extend. SmartCloud separates those responsibilities: `SupervisorAgent` decides, `AgentWorkflowGraph` controls the path, specialized agents execute, and `VerifierAgent` guards the result.

**Follow-up:** What is the cost? More route latency and another model call. Mitigate with small routing prompts, rule fallback, routing metrics, and bypassing the graph for deterministic cache hits.

### Q2. Walk me through one request end-to-end.

**Answer anchors:** `ChatService` loads recent conversation history, checks the tenant-scoped L1 cache for a new conversation, then calls `MultiAgentOrchestrator`. The graph runs `supervisor -> selected route node -> verifier`. The orchestrator starts the selected agent's `Flux`, emits a `__THINK__` route trace, applies the verifier, and ChatService persists non-trace output. The frontend consumes the same SSE protocol as before.

### Q3. What exactly does LangGraph4j do here, and what does Reactor do?

**Answer anchors:** LangGraph4j controls state transitions and conditional routing through `AgentWorkflowGraph`. Reactor handles asynchronous token streaming from the selected agent with `Flux<String>`. The graph determines *which* agent should run; WebFlux transports *how* its answer is streamed. This prevents forcing token-level streaming into the graph state machine.

### Q4. What state is carried in the workflow graph?

**Answer anchors:** `AgentWorkflowState` contains the user message, recent history, supervisor decision, selected-agent name, graph-node trace, and guard message. The state is intentionally execution metadata, not the full answer stream, because long token streams do not belong in graph state.

### Q5. How do you route requests, and how do you handle route-model failure?

**Answer anchors:** The supervisor asks the configured LangChain4j chat model to return compact JSON with `route`, standalone `query`, and `reason`. JSON is parsed into `AgentDecision`. On errors, fallback rules recognize casual messages, cloud products, billing, ICP, marketing, deep research, procedures, and website refresh. Unknown cases fall back conservatively to knowledge RAG.

**Follow-up:** How would you measure route quality? Build a labeled route dataset, calculate route accuracy and confusion matrix, log the chosen route/reason/latency, and sample disagreements between fallback and LLM decisions.

### Q6. Why does the supervisor rewrite follow-up questions into standalone queries?

**Answer anchors:** Retrieval quality depends on explicit entities and intent. A follow-up such as “that one can be renewed?” needs the previous product or invoice context. The supervisor gets the recent conversation and emits a complete search query before downstream RAG/tool calls.

### Q7. Why are there both specialized routes and `KNOWLEDGE_RAG` / `PROCEDURE_RAG`?

**Answer anchors:** Specialized agents represent high-value, tool-oriented business domains. General knowledge and legacy procedural queries still benefit from the existing RAG pipeline. Keeping those routes preserves compatibility while allowing the project to grow without a big-bang replacement.

### Q8. How many agents are in the current project?

**Answer anchors:** There are nine route targets: `DIRECT`, `PRODUCT_TECH`, `FINANCE_ORDER`, `ICP_SERVICE`, `OPS_MARKETING`, `DEEP_RESEARCH`, `KNOWLEDGE_RAG`, `PROCEDURE_RAG`, and `WEB_REFRESH`. In addition, `SupervisorAgent` performs planning/routing and `VerifierAgent` performs a lightweight quality gate. Do not call every route a fully autonomous LLM agent: several are currently deterministic tool-backed service agents, by design.

### Q9. What does the verifier protect against today, and what is missing?

**Answer anchors:** It passes direct answers through and returns a safe fallback when a non-direct stream is empty. It does not yet verify citations, factual grounding, policy compliance, or tool-result consistency. A production evolution would collect the full answer, validate sources/tool evidence, optionally use a judge model, then stream or revise the result.

## 3. RAG and Agentic RAG questions

### Q10. Explain the RAG chain in your project.

**Answer anchors:** The route agent calls `KnowledgeResearchAgent`, which delegates to `RagService.streamAnswer(query)`. RAG remains responsible for retrieval and answer generation; agents do not directly manipulate `VectorStoreService`. That preserves a single retrieval boundary and limits coupling.

### Q11. Why should an agent not directly access the vector store?

**Answer anchors:** It would duplicate retrieval policies, source formatting, reranking, and observability across agents. `RagService` is the contract boundary. It makes retrieval improvements, permission filtering, and evaluations centrally enforceable.

### Q12. How would you improve retrieval quality when answers are wrong?

**Answer anchors:** Diagnose in order: query rewrite quality, document parsing/chunking, embedding model, top-K and similarity threshold, hybrid retrieval, reranking, metadata/tenant filters, and answer grounding. Evaluate retrieval recall separately from generation faithfulness; do not only inspect the final answer.

### Q13. What is Agentic RAG in this project rather than ordinary RAG?

**Answer anchors:** Ordinary RAG always retrieves then answers. Here, the supervisor first decides whether knowledge retrieval is appropriate, and different routes may call deterministic business tools, A2A, MCP, research planning, or RAG. The current deep-research route is a controlled demo; a production version would add iterative planning, multi-source retrieval, evidence collection, and stopping criteria.

### Q14. How do you avoid hallucination in billing and cloud-product answers?

**Answer anchors:** Route them away from a generic model into domain agents backed by tool contracts. Billing uses tenant-aware tool data, while product/RAG answers should be constrained to retrieved sources. The current verifier only guards empty output, so source citation and claim verification are explicitly listed as the next production step.

### Q15. How would you evaluate a RAG system?

**Answer anchors:** Split it into retrieval and generation. Retrieval: Recall@K, MRR, nDCG, and source coverage. Generation: correctness, faithfulness/grounding, completeness, citation precision, safety, latency, and cost. Use a stable golden set plus production traces and human review of difficult cases.

## 4. Memory, context, and cache questions

### Q16. What memory design do you have now?

**Answer anchors:** Short-term memory is recent conversation history, capped by `ConversationMessageMapper` to the latest 20 messages for the supervisor context. Conversation messages and traces can be persisted; the RAG knowledge base is semantic/domain memory. This is not yet a full user-preference memory system.

### Q17. Why do you only reuse recent messages instead of the whole chat history?

**Answer anchors:** It controls token cost, latency, and distraction from irrelevant old turns. For long conversations, summarize older turns and retrieve relevant historical facts rather than appending everything. Important user preferences should be promoted into a separate structured long-term memory with TTL and consent.

### Q18. Describe your cache layers and tenant isolation.

**Answer anchors:** Redis is intended as L1 for high-frequency answers, while vector retrieval remains L2 for long-tail knowledge. Cache keys include a namespace derived from tenant, region, model, and knowledge-index version, then the normalized query. That prevents answers from one tenant or index version leaking into another.

**Follow-up:** The current implementation also retains an embedding-based local file cache as a fallback/demo layer. State that candidly and say production would use Redis client abstractions, distributed metrics, eviction policy, and sensitive-data exclusions.

### Q19. When should you not cache an agent answer?

**Answer anchors:** Do not cache user-specific billing, mutable status, authorization-sensitive data, unsafe or failed answers, and conversations with meaningful history unless the key includes all relevant context. Cache only validated, stable, tenant-safe results with a suitable TTL.

## 5. MCP, A2A, and tool-use questions

### Q20. What MCP capability have you implemented?

**Answer anchors:** The `/mcp` endpoint supports JSON-RPC `initialize`, `tools/list`, and `tools/call`, plus a streamable-HTTP readiness response. Tools include billing query, ICP checklist, marketing package, research plan, and H5 generation. Calls are traced and metricized.

### Q21. Is this a full production MCP server?

**Answer anchors:** It is a protocol-shaped local demo, not a complete production MCP platform. It demonstrates the core tool discovery/invocation contract. Production work would add session lifecycle, origin and authorization controls, schema validation, tool timeouts/retries, auditing, and official SDK interoperability tests.

### Q22. What problem does A2A solve that MCP does not?

**Answer anchors:** MCP exposes tools/resources/prompts from a server to an agent. A2A is agent-to-agent delegation: discovering an agent's capabilities through an Agent Card, sending it a task, and receiving task artifacts/status. In SmartCloud, `OpsMarketingAgent` requests product context from `ProductTechAgent` via the A2A demo before generating marketing material.

### Q23. How does your A2A demo work?

**Answer anchors:** `/.well-known/agent-card.json` exposes capability metadata. `/message:send` returns a JSON-RPC task result; `/message:stream` returns SSE progress/completion events. The current product agent is local/deterministic so the demo stays repeatable. A remote service can replace it behind the same client contract.

### Q24. How do you make LLM tool calling safe?

**Answer anchors:** Do not allow arbitrary tool names or arbitrary SQL/URLs. Use an allowlist, JSON schema validation, tenant authorization at the tool boundary, timeouts, idempotency for writes, sanitised error messages, rate limits, audit traces, and human approval for irreversible actions. Treat retrieved text as untrusted to resist prompt injection.

## 6. Persistence, security, and observability questions

### Q25. How do you persist chat data, and why mention Saga?

**Answer anchors:** The intended model is MySQL for searchable conversation/case indexes and MongoDB for flexible full messages, traces, and tool-call payloads. Because it spans stores without a distributed transaction, write the index first, write detailed data second, and compensate/delete the index if the second step fails. The demo includes fallback behavior when infrastructure is unavailable; production needs a durable outbox/compensation record and retry worker.

### Q26. How is tenant identity passed through the agent chain?

**Answer anchors:** Authentication resolves a `SmartCloudUserContext`, then ChatService passes it to the orchestrator and `AgentContext`. Tool calls, cache namespace generation, trace records, and billing lookup can use tenant/user IDs. Authorization must still be enforced at each data/tool boundary, not only at the initial controller.

### Q27. How did you implement observability?

**Answer anchors:** Every route records the chosen route, selected agent, reason, graph nodes, latency, and tool metadata through `SmartCloudTraceService`; traces have memory fallback and optional Mongo persistence. `SmartCloudMetricsService` emits route/cache/protocol metrics, and Actuator provides Prometheus scraping. The React Trace page is a local LangSmith/Phoenix-style demo, not a replacement for a hosted tracing product.

### Q28. What would you alert on in production?

**Answer anchors:** Route error rate, empty-answer/fallback rate, cache hit rate, RAG latency and no-document rate, tool/MCP/A2A failure and timeout rate, token/cost usage, queue saturation, authentication failures, and data-persistence compensation backlog. Tag all metrics by route and tenant tier, but avoid high-cardinality raw user IDs.

### Q29. Why use WebFlux for this project?

**Answer anchors:** The chat API streams model tokens over SSE and may have many concurrent I/O-bound requests. WebFlux and Reactor fit that model. Blocking model or graph work is moved to `boundedElastic`; otherwise blocking calls could stall the event loop.

## 7. System-design pressure questions

### Q30. The supervisor is wrong 8% of the time. What do you do?

**Answer anchors:** Collect labeled route traces, identify the confusion pairs, improve prompt/examples and fallback rules, add confidence scores, allow a low-confidence clarification question, and use a second-stage router only for ambiguous cases. Keep an escape hatch to general RAG and log user correction signals.

### Q31. How would you handle 10x traffic tomorrow?

**Answer anchors:** Keep SSE connections stateless at the API layer, externalize conversation/cache/trace storage, use bounded concurrency and timeouts around models/tools, introduce queueing and backpressure for expensive research, separate fast routes from slow routes, rate-limit by tenant, and scale model/retrieval services independently. Do not claim the demo is already horizontally production-scaled.

### Q32. A marketing agent receives malicious text from a retrieved document. What is the threat?

**Answer anchors:** Prompt injection. Retrieved text can attempt to change system instructions or trigger tools. Keep trusted instructions separate, mark retrieval as untrusted data, constrain tool permissions, validate all tool arguments server-side, and require explicit user intent before actions that create assets or spend money.

### Q33. Design a real deep-research agent for cloud architecture selection.

**Answer anchors:** Plan the question into subproblems, gather authoritative sources with date and provenance, retrieve per subproblem, deduplicate and rank evidence, synthesize a comparison with trade-offs, cite each recommendation, run a critic for unsupported claims, and stop under a budget/time limit. Cache only stable research artifacts with source-version metadata.

### Q34. How would you decide whether to add more agents?

**Answer anchors:** Add an agent only when it has a distinct domain policy, tool set, retrieval corpus, evaluation set, or lifecycle. If it only changes wording, keep it as a prompt or skill. More agents increase routing cost, failure modes, and observability burden.

## 8. Honest project-boundary questions

### Q35. Which parts are demo-grade today and which parts would you productionize first?

**Answer anchors:** The runnable core is graph routing, streaming, specialized route contracts, RAG delegation, local MCP/A2A demonstrations, metrics, and trace fallback. Production priorities are real Redis/MySQL/Mongo deployment, robust reactive clients rather than hand-built protocol adapters, durable Saga/outbox, source-level verification, evaluation datasets, remote A2A interoperability, secrets management, and policy/security hardening.

### Q36. Why did you choose Java/Spring instead of a Python Agent framework?

**Answer anchors:** The project extends a Java Spring Boot RAG application and keeps existing API, service, and WebFlux contracts. LangChain4j integrates the model/RAG layer, while LangGraph4j provides explicit graph control flow without introducing a second runtime. The choice reduces operational complexity for a Java backend team; it is not a claim that Java is universally better for agents.

## 9. Six questions you should rehearse aloud first

1. Draw the `ChatService -> MultiAgentOrchestrator -> LangGraph4j workflow -> specialized agent -> verifier -> SSE` path without looking at code.
2. Explain why graph control flow and `Flux<String>` streaming are intentionally separate.
3. Explain exactly what is real in the MCP/A2A demo and what is a production next step.
4. Explain RAG evaluation as retrieval quality plus generation faithfulness.
5. Explain cache-key tenant isolation and when answers must not be cached.
6. Give an honest answer about the current verifier's limitations and your planned source-grounding upgrade.

## 10. Mock-interview rule

When an interviewer asks about a feature, always answer in this order:

1. State the user/business problem.
2. State the current implementation and name the relevant component.
3. State the trade-off.
4. State the production hardening you would add next.

That sequence is much stronger than claiming every demo integration is already industrial-grade.
