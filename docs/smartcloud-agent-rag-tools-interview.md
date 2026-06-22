# SmartCloud Agent / RAG / Tool Protocol Interview Answers

This guide answers the 52 questions one by one for the current SmartCloud project. It follows the interview topics supplied by the user and should be used as speaking notes, not memorized word-for-word.

## How to answer honestly

- **Implemented in the current project:** Spring Boot WebFlux, LangGraph4j workflow routing, LangChain4j + DashScope, Qdrant vector retrieval, Lucene BM25 lexical retrieval, DashScope reranking, SSE chat output, JWT demo authentication, tenant-aware cache namespace, protocol-shaped MCP and A2A endpoints, local trace/metrics fallback.
- **Do not overclaim:** The MCP/A2A endpoints are runnable demos of their core contracts, not complete production protocol platforms. Redis/MySQL/Mongo can be enabled as infrastructure integrations, but the application has local fallbacks. There is no measured online QPS, corpus scale, or published offline evaluation score in this repository.
- **Recommended answer pattern:** business problem -> current SmartCloud design -> trade-off -> production hardening.

---

# Part 1: Agent

## 1. What is an Agent? How is it fundamentally different from an LLM?

**Answer:** An LLM is a probabilistic model that produces the next token from its input. An Agent is an application system around an LLM: it can observe context, decide a next action, invoke tools or retrieval, keep state, and iterate until a task is complete. The LLM is usually the reasoning component; the Agent adds execution and control flow.

**SmartCloud mapping:** `SupervisorAgent` uses an LLM to make a route decision, while `MultiAgentOrchestrator`, `AgentWorkflowGraph`, specialized agents, tools, trace service, and SSE delivery make up the Agent system. For billing, the system does not ask the LLM to invent an amount; it routes to `FinanceOrderAgent` and a controlled tool contract.

## 2. Which core components make up an Agent architecture?

**Answer:** A practical architecture contains a model/reasoner, instructions and role policy, context/memory, a planner or routing policy, tools/actions, an execution loop or workflow, a state store, guardrails, and observability/evaluation. Not every Agent needs a long autonomous loop; a customer-support Agent often benefits from deterministic workflow boundaries.

**SmartCloud mapping:** Model access is `ModelFactory`; short context is `ConversationMessageMapper`; routing is `SupervisorAgent`; control flow is `AgentWorkflowGraph`; execution is the domain agents; tools are behind `SmartCloudToolClient`; quality guard is `VerifierAgent`; traces and metrics are `SmartCloudTraceService` and `SmartCloudMetricsService`.

## 3. Explain Workflow, Agent, and Tools and their differences.

**Answer:** A workflow is an explicit, usually deterministic sequence or graph of steps. An Agent is a goal-driven decision unit that can select actions from context. A tool is a bounded capability such as querying a bill, generating an asset, or searching a knowledge base. Workflow coordinates Agents; Agents decide when and how to use tools; tools execute narrowly scoped operations.

**SmartCloud mapping:** LangGraph4j defines `supervisor -> route node -> verifier` as the workflow. `FinanceOrderAgent` is an Agent role. `billing.query` is a tool exposed through the local tool layer and MCP endpoint.

## 4. What other Agent design patterns do you know? What is the difference between Agent and Workflow?

**Answer:** Common patterns include ReAct, Plan-and-Execute, reflection/critic, router-supervisor, hierarchical manager-worker, debate, swarm/peer delegation, and event-driven agents. A workflow predefines legal transitions and is predictable, testable, and easy to observe. An Agent chooses actions dynamically; it is more flexible but less predictable and needs stronger budget, safety, and evaluation controls.

**SmartCloud mapping:** The current system deliberately uses a supervisor-router workflow, not a free-form autonomous swarm. This is a good fit for cloud support because request types are known and correctness, authorization, latency, and auditability matter.

## 5. What Agent reasoning patterns exist? What is ReAct and how is it implemented?

**Answer:** Reasoning patterns include direct response, ReAct, plan-and-execute, reflection, self-consistency, tree/graph search, and manager-worker delegation. ReAct means alternating between reasoning about the next step and taking an action: the model receives a task and tool descriptions, emits a tool call, receives the observation, then decides whether to call another tool or produce a final answer.

**Implementation:** Put tool schemas in the model context, parse a structured tool call, validate arguments and authorization server-side, execute with timeout, append the normalized tool result as an observation, and repeat under step/time/token budgets. Never execute model text directly as SQL, shell, or URL access.

**SmartCloud mapping:** The project currently uses controlled routing plus one domain execution path rather than an open ReAct loop. Its MCP tools and domain agents provide the safe action boundary. A future deep-research route is the best place to use bounded ReAct iterations.

## 6. What are the core differences among ReAct, Plan-and-Execute, and Reflection? How do you choose?

**Answer:** ReAct interleaves reasoning and tool observations, so it adapts well to uncertain tool-driven tasks. Plan-and-Execute creates a plan first, then delegates or executes steps; it is better for long, decomposable tasks but needs re-planning when assumptions fail. Reflection adds a critic/reviewer pass to identify unsupported claims or poor plans; it improves quality but adds cost and latency.

**Selection:** Use direct or route-to-tool for simple customer support; ReAct for a small number of uncertain API/retrieval calls; plan-and-execute for research reports or complex cloud architecture comparison; reflection for high-risk output, final reports, or answers requiring citations. SmartCloud uses routing now and reserves deep research plus verifier upgrades for plan/reflection.

## 7. How do you split complex tasks? Why and how does it improve results?

**Answer:** First define the expected artifact and constraints, then split by independent sub-goals, required data sources, and acceptance criteria. Build a dependency graph, run independent retrieval/tool steps in parallel where safe, and let a coordinator synthesize evidence. Splitting reduces context overload, makes failures local and observable, allows domain-specialized prompts/tools, and supports separate evaluation of each subtask.

**SmartCloud mapping:** A cloud architecture research task can split into workload assumptions, compute, network, storage, cost, security, and recommendation. Each subtask retrieves/tool-calls within a budget; a synthesis step cites evidence and highlights assumptions. The current `DeepResearchAgent` is a lightweight route; this decomposition is the production evolution.

## 8. Explain Agent memory and how to design a memory module in practice.

**Answer:** Agent memory is information retained beyond the immediate prompt. A useful distinction is short-term working memory, episodic memory of past interactions, semantic memory of facts/knowledge, and procedural memory of reusable workflows or skills. A memory module needs a write policy, schema, retrieval policy, update/conflict policy, TTL/deletion policy, permission model, and evaluation.

**SmartCloud mapping:** Recent chat messages are short-term memory; the RAG corpus is semantic/domain memory; stored conversation/trace records are episodic records; workflow routes and prompts act as procedural knowledge. Production user preference memory should be structured, tenant-scoped, consented, and retrieved only when relevant.

## 9. How would you build long- and short-term memory? How is it stored, at what granularity, and used?

**Answer:** Short-term memory should keep the most relevant recent turns plus a rolling summary, stored per conversation and injected before routing/retrieval. Long-term memory should store atomic, attributable facts such as a stable preference, verified profile attribute, resolved incident, or user-approved summary. Store structured metadata including tenant, user, source, confidence, timestamps, expiration, and sensitivity level; retrieve by semantic relevance plus permission filters.

**SmartCloud mapping:** `ConversationMessageMapper` limits supervisor history to the most recent 20 messages. Conversation and traces have persistence paths with fallback behavior. The project should not claim it already has a mature preference-memory extractor; that is a reasonable next step.

## 10. What is Multi-Agent?

**Answer:** Multi-Agent is a system where several specialized decision/execution units collaborate on a task under a defined communication and coordination policy. Specialization can be by business domain, tool permissions, modality, or lifecycle. It is useful when one general prompt would otherwise carry too many tools, policies, and evaluation criteria.

**SmartCloud mapping:** A supervisor routes requests among direct answer, product technology, finance/order, ICP filing, operations marketing, deep research, knowledge RAG, procedure RAG, and web-refresh paths. `VerifierAgent` is an additional guard role.

## 11. Compare Single-Agent and Multi-Agent designs.

**Answer:** A single Agent is simpler, cheaper, and easier to debug when tools and policies are few. A multi-Agent design improves domain separation, least-privilege tool access, independent evaluation, and extensibility, but adds routing error, handoff latency, shared-state complexity, and more observability needs. Do not adopt multi-Agent merely because it sounds advanced.

**SmartCloud mapping:** The previous `ReactAgent` is retained as a comparison implementation. SmartCloud moved to multi-Agent because billing, ICP, product, marketing, and research need different domain contracts and risk boundaries.

## 12. What methods are used for Agent memory compression?

**Answer:** Common methods are sliding windows, rolling summaries, structured fact extraction, topic-based summaries, episodic clustering, semantic deduplication, importance scoring, hierarchical summaries, and retrieval-time compression/reranking. Compression must preserve source, time, uncertainty, and user consent; otherwise summaries can turn old or mistaken statements into permanent “facts.”

**SmartCloud mapping:** The current implementation primarily uses a recent-message window. A next step is to summarize older conversation turns into a source-linked conversation summary and retain only verified preferences as long-term records.

## 13. Why hand-build an Agent instead of using a mature framework in some engineering cases?

**Answer:** Hand-built orchestration can be appropriate when the flow is small, domain constraints are strict, existing service contracts must be reused, performance/observability requirements are specific, or a framework would add a second abstraction/runtime without enough value. Frameworks help when workflows become complex, stateful, resumable, human-in-the-loop, or need graph visualization/checkpointing.

**SmartCloud mapping:** The project uses LangGraph4j for explicit routing graph control, but keeps answer streaming in Reactor and tools in Spring services. That hybrid is intentional: it introduces a graph where it provides value and preserves existing WebFlux/RAG contracts.

## 14. How do you give an LLM planning ability?

**Answer:** Define the task objective, available tools, constraints, output schema, and stop conditions; ask the model to generate a structured plan; validate it; execute steps under budgets; feed observations back; and re-plan only when evidence invalidates the plan. Separate planning from execution where possible, and validate every tool action server-side.

**SmartCloud mapping:** `SupervisorAgent` performs a compact planning decision: route, standalone query, and reason. A full planning implementation for deep research would add a typed research plan with subquestions, data sources, budget, dependencies, and evidence-based completion criteria.

## 15. Explain the Agent reflection mechanism. Why use it and how is it implemented?

**Answer:** Reflection is a deliberate review step after a draft, plan, or tool result. It checks whether requirements were met, whether claims are supported, whether tools were used correctly, and whether a different plan is needed. It reduces avoidable errors but is not magic; it costs extra latency/tokens and a model can still approve a wrong answer.

**SmartCloud mapping:** `VerifierAgent` is currently a lightweight guard: direct answers pass through and non-direct empty streams produce a safe fallback. A stronger reflection stage would inspect retrieved sources/tool results, require evidence for each factual claim, detect unsupported recommendations, and request a revision or return an uncertainty message.

## 16. How do you design Multi-Agent collaboration and dynamic switching?

**Answer:** Start with explicit roles, input/output contracts, tool permissions, shared-state ownership, and a coordinator. Use intent/routing confidence, task progress, tool failure, cost/latency budget, and policy risk as switching signals. Define allowed handoff edges, retry/compensation behavior, loop limits, and a fallback route; record every handoff for evaluation.

**SmartCloud mapping:** `AgentWorkflowGraph` gives an allowlisted switch graph: supervisor -> exactly one selected domain node -> verifier -> end. `AgentDecision` contains route/query/reason, and the graph trace is emitted through `__THINK__` and persisted. A production extension can add confidence-aware clarification and bounded re-routing after tool failure.

---

# Part 2: RAG

## 1. What is RAG? Describe the complete workflow of a RAG system.

**Answer:** Retrieval-Augmented Generation supplies an LLM with relevant external evidence at query time. The offline flow is document ingestion, parsing/OCR, cleaning, chunking, embedding, metadata creation, and index storage. The online flow is authorization/context loading, query normalization or rewrite, multi-route retrieval, filtering/reranking, prompt assembly with evidence, model generation, citation/guard checks, and tracing/evaluation.

**SmartCloud mapping:** Documents are indexed into Qdrant and a Lucene lexical index. At answer time `KnowledgeResearchAgent` calls `RagService.streamAnswer(query)`. The system uses DashScope `text-embedding-v3` for embeddings and `gte-rerank-v2` for reranking, then streams the answer through WebFlux.

## 2. What problem does RAG primarily solve for LLMs?

**Answer:** RAG brings fresh, private, or domain-specific knowledge into the answer without retraining the base model. It improves auditability by showing evidence, reduces unsupported factual answers, and makes knowledge updates much faster than updating model weights. It does not eliminate hallucination by itself; retrieval and answer grounding still need evaluation.

**SmartCloud mapping:** Cloud-product documents, filing procedures, and support knowledge should be answered from the project knowledge base or tools rather than the model's parametric memory.

## 3. Compared with fine-tuning, what does RAG solve? What are their strengths and weaknesses?

**Answer:** RAG is best for frequently changing factual knowledge, private documents, traceability, and low-cost updates. Fine-tuning is best for stable behavior: output format, tone, tool-call style, domain language patterns, and repeated task execution. RAG can fail if retrieval misses; fine-tuning can become stale, is costly to retrain, and does not provide source attribution. A common production design combines them: tune behavior if needed and use RAG for facts.

**SmartCloud mapping:** The project uses prompting plus RAG/tool data rather than fine-tuning. That is appropriate for changing cloud prices, policies, invoices, and product details.

## 4. How are RAG documents stored? What granularity and chunking strategy do you use?

**Answer:** Store original document identity, extracted text, chunks, embeddings, metadata, source location, timestamps, and index version. Chunk by semantic structure first: headings, paragraphs, lists, tables, code blocks, and page boundaries. Use token/character limits only as a backstop. The right chunk size depends on document type, embedding model, and retrieval task, so it must be evaluated rather than chosen by folklore.

**SmartCloud mapping:** Current configuration uses `chunk-size: 800` and `chunk-overlap: 120`. Chunks are indexed in Qdrant and Lucene; manifest/index metadata supports inspection and version-aware cache namespaces.

## 5. How do you avoid losing semantic meaning at chunk boundaries?

**Answer:** Split on document structure rather than fixed length where possible, use overlaps, preserve headings and parent-section metadata, attach neighboring chunk context at retrieval time, and avoid splitting atomic structures such as a table row, procedure step sequence, or code block. Test boundary questions specifically in the evaluation set.

**SmartCloud mapping:** The 120-character overlap is the current baseline. For ICP procedures or product pricing tables, structural chunking and parent/child retrieval would be a stronger next step than only increasing overlap.

## 6. What is an embedding in RAG? How do you select and evaluate an embedding model?

**Answer:** An embedding maps text into a dense vector where semantically similar content is close under a similarity metric. Select a model based on language coverage, domain fit, query/document length, vector dimension and storage cost, latency, privacy/deployment constraints, and most importantly retrieval performance on a labeled dataset. Evaluate Recall@K, MRR, nDCG, hard negatives, latency, and cost.

**SmartCloud mapping:** The configured embedding model is DashScope `text-embedding-v3`. It should be evaluated with Chinese cloud-support queries, product aliases, abbreviated terms, and procedure questions before making claims about quality.

## 7. Which embedding algorithms or model families do you know?

**Answer:** Traditional approaches include one-hot, TF-IDF, Word2Vec, GloVe, and Doc2Vec. Modern retrieval usually uses transformer bi-encoders or contrastive sentence embeddings, multilingual embeddings, late-interaction models such as ColBERT, and sparse learned representations. Dense embeddings are strong at semantic matching; sparse/BM25-like representations preserve exact keywords, IDs, and rare terms.

**SmartCloud mapping:** SmartCloud uses dense vector retrieval plus Lucene BM25-style lexical retrieval, which is a practical dense-sparse hybrid baseline for cloud product IDs and natural-language support questions.

## 8. What is a vector database? Have you compared vector databases?

**Answer:** A vector database stores embeddings and metadata and performs approximate nearest-neighbor search, often with filtering, persistence, replication, and hybrid retrieval. Compare index types and recall/latency trade-offs, metadata filtering, hybrid capabilities, operational model, SDK support, multi-tenancy, cost, backup, and observability. The right choice depends on data scale and operational requirements.

**SmartCloud mapping:** The project uses Qdrant because it fits the existing Java/LangChain4j integration and supports vector retrieval plus metadata-oriented operations. Lucene complements it for lexical search. I would not claim a broad benchmark unless one was actually run.

## 9. Explain the vector database you use. What scale and performance have you seen? Any bottlenecks?

**Answer:** In this project I use local Qdrant for dense retrieval and Lucene for lexical retrieval. It is a local/interview-demo deployment, so I describe architecture and measured results only when I have a recorded benchmark. Typical bottlenecks to investigate are embedding API latency, poor filtering/selectivity, excessive top-K, disk/memory pressure, large metadata payloads, reranker latency, and index rebuilds.

**Good interview wording:** “I do not fabricate a production corpus or QPS number. For production I would load-test realistic documents and queries, track p50/p95 retrieval and rerank latency, Recall@K, memory, and index build time.”

## 10. When you give a RAG input to the model, what is the system workflow?

**Answer:** Authenticate and load relevant context, normalize/rewrite the query, retrieve dense and lexical candidates under metadata filters, merge and rerank them, select evidence within a context budget, build a prompt that tells the model to answer only from evidence and express uncertainty, stream the answer, then record citations/traces/metrics. Failed or empty retrieval should lead to a clear fallback, not invented facts.

**SmartCloud mapping:** The supervisor first rewrites follow-up questions into a standalone query; `KnowledgeResearchAgent` delegates to `RagService`; `VerifierAgent` handles the current empty-output guard; ChatService keeps only non-trace chunks for history/cache.

## 11. What is the difference between vector retrieval and keyword retrieval?

**Answer:** Vector retrieval matches semantic similarity and handles paraphrases well, but can miss exact identifiers and may return superficially related text. Keyword retrieval such as BM25 excels at exact terms, product models, error codes, names, and rare words, but struggles with paraphrase. Hybrid retrieval combines them, then reranks the merged candidates.

**SmartCloud mapping:** Qdrant dense retrieval and Lucene BM25-style retrieval are deliberately both present. This matters for queries mixing natural language with terms such as ECS, GPU model names, invoice numbers, and ICP terminology.

## 12. How do you do Query Rewrite and why?

**Answer:** Query rewrite converts vague, conversational, or incomplete input into an explicit retrieval query. It resolves pronouns from history, expands abbreviations/synonyms, preserves constraints, removes irrelevant social language, and may create multiple query variants. The goal is higher retrieval recall, not a prettier question; rewrites must not add facts the user did not provide.

**SmartCloud mapping:** `SupervisorAgent` returns a standalone `query` using current message plus recent history. `QueryPreprocessor` also expands cloud-related terminology. Keep the original query in traces for debugging rewrite drift.

## 13. What is multi-channel recall and how do you implement it?

**Answer:** Multi-channel recall retrieves candidates from different signals, for example dense vectors, BM25, metadata filters, FAQ exact match, SQL/tool data, graph relations, and web sources. Normalize scores or use rank fusion such as RRF, deduplicate by document identity, then rerank the combined candidate set. Each channel should be measured separately to understand contribution.

**SmartCloud mapping:** Current channels are Qdrant dense and Lucene lexical. Billing and product information should additionally use tool calls because user-specific structured data should not be served from a general RAG corpus.

## 14. Which RAG retrieval optimization strategies do you know?

**Answer:** Improve parsing/cleaning, structural chunking, metadata quality, query rewrite, hybrid recall, rank fusion, reranking, dynamic top-K, context compression, domain synonyms, parent-child retrieval, freshness filters, caching, and permission filtering. Optimize with a labeled failure taxonomy; blindly raising top-K often increases noise and token cost.

**SmartCloud mapping:** The project already has hybrid retrieval, DashScope reranking, query preprocessing, cache support, and document/index metadata. The next high-value step is a golden query set and retrieval metrics before changing parameters.

## 15. What advanced RAG patterns do you know?

**Answer:** Advanced patterns include hybrid RAG, parent-child RAG, multi-query RAG, HyDE, corrective RAG, self-RAG, adaptive RAG, agentic RAG, RAPTOR/hierarchical summaries, GraphRAG, multimodal RAG, and long-context retrieval. Use them only when the failure mode justifies their complexity.

**SmartCloud mapping:** SmartCloud is a hybrid/agent-routed RAG baseline. Its Agentic aspect is that the supervisor decides whether to use knowledge RAG, a specialized tool agent, or a research route rather than retrieving for every question.

## 16. When would you use a graph database to enhance vector retrieval?

**Answer:** Use a graph when relationships are first-class and multi-hop: product dependencies, organization ownership, incident topology, compliance rules, version compatibility, or entity lineage. Vector search finds semantically relevant entry points; graph traversal enforces relation/path constraints and exposes why two facts connect. It adds ingestion and consistency cost, so it is not needed for flat FAQ search.

**SmartCloud mapping:** A graph could model cloud products -> regions -> compatible services -> pricing rules -> ICP constraints. The current project does not implement a graph database, so describe it as a future enhancement, not an existing feature.

## 17. How do you reduce hallucinations in a RAG system?

**Answer:** Improve retrieval recall and precision, require cited evidence, separate facts from recommendations, instruct the model to abstain when evidence is insufficient, validate structured claims against tools, apply output guards, and use evaluator samples. Also defend against prompt injection in retrieved documents by treating them as untrusted data.

**SmartCloud mapping:** The architecture routes billing to tools and general knowledge to RAG, which reduces pressure on one generic model. `VerifierAgent` currently guards empty streams only; source-level grounding and claim verification are explicitly future work.

## 18. How do you quantify RAG quality?

**Answer:** Evaluate retrieval and generation separately. Retrieval uses Recall@K, MRR, nDCG, source coverage, and filter correctness. Generation uses correctness, faithfulness, completeness, citation quality, safety, latency, and cost. Build a labeled golden set from real user intents, include hard negatives and time-sensitive cases, then monitor production traces with sampled human review.

**SmartCloud mapping:** The project has traces and metrics hooks but no published offline scorecard yet. The honest next step is to add route-specific golden sets for product, ICP, knowledge, and procedure queries.

## 19. How does a RAG knowledge base support dynamic, continuous updates?

**Answer:** Use a versioned ingestion pipeline: detect changed documents, parse and chunk incrementally, embed only affected chunks, upsert new chunks, delete stale ones, update lexical/vector indexes atomically or with a staged alias, invalidate cache by index version, and retain rollback/audit metadata. Freshness should be explicit in retrieval filters and answer citations.

**SmartCloud mapping:** Document manifests and index state exist, and cache namespace includes an index version. The project also has crawl/indexing paths. Production should add robust change detection, idempotency, scheduled refresh policies, and alerting for failed ingestion.

## 20. What is the hardest part of real RAG delivery?

**Answer:** The difficult part is not calling an embedding API; it is defining trustworthy source data, preserving document structure, building a representative evaluation set, handling permissions and freshness, and diagnosing whether an error came from retrieval, prompting, model behavior, or user ambiguity. Operationally, data ownership and measurable feedback loops are usually harder than the vector database itself.

**SmartCloud mapping:** The project shows the technical path. To make it production-grade, I would prioritize source governance, evaluation datasets, citation verification, and tenant/authorization filters over adding more agent labels.

---

# Part 3: Function Calling, MCP, Skill, A2A, and Transport

## 1. What is Function Calling and what is its principle?

**Answer:** Function Calling lets a model return a structured request to invoke an application-defined function rather than only natural language. The application sends tool names, descriptions, and JSON schemas; the model chooses a tool and arguments; the host validates and executes it; the result is returned to the model as a tool message; then the model creates the user-facing answer. The model proposes an action, but the application remains the authority that executes it.

**SmartCloud mapping:** The project uses controlled Java service/tool contracts for billing, ICP, marketing, research planning, and H5 generation. Its MCP `tools/call` endpoint demonstrates a standardized external tool invocation shape. The LLM does not directly execute arbitrary code or SQL.

## 2. How does an LLM learn to call external tools?

**Answer:** At inference time, the model is given tool schemas and examples in context, then predicts structured tokens that match a function name and arguments. During training, it learns from supervised examples of user intent -> tool schema -> valid call -> tool observation -> final answer, often reinforced with execution feedback. Tool quality also depends heavily on clear names, descriptions, schemas, and error feedback.

**SmartCloud mapping:** `SupervisorAgent` is currently trained/prompted for route JSON, not free-form function-call selection. Domain execution is deliberately constrained after routing, which is safer for a cloud-support demo.

## 3. How is Function Calling ability trained in large models?

**Answer:** It is generally built through instruction tuning on structured tool-use traces: choose the correct tool, fill valid arguments, recover from tool errors, decide whether another call is needed, and produce a grounded final response. Training data may use human-written traces, synthetic tool environments, execution-verified examples, and preference/reward optimization. Generalization requires varied schemas and negative examples, not memorizing one API.

**Interview nuance:** The exact recipe is model-provider-specific and usually proprietary. Explain the common learning signal instead of asserting private training details for a particular model.

## 4. What is MCP? Explain its core content.

**Answer:** MCP, Model Context Protocol, is an open protocol for connecting AI applications to external context and capabilities through a standardized client-server contract. Its core concepts include initialization/capability negotiation, tools, resources, prompts, structured JSON-RPC messages, transports, and lifecycle/security considerations. It aims to decouple an Agent host from one-off tool integrations.

**SmartCloud mapping:** SmartCloud exposes `/mcp` with JSON-RPC `initialize`, `tools/list`, and `tools/call`; the listed tools include `billing.query`, `icp.checklist`, `marketing.generate_package`, `research.plan`, and `h5.generate`.

## 5. What components does MCP consist of?

**Answer:** The main roles are MCP Host, MCP Client, and MCP Server. The host is the AI application that manages user interaction and model calls; a client maintains a connection to a server; the server exposes tools, resources, and prompts. The protocol also includes initialization/capability negotiation, JSON-RPC request/response/notification semantics, a transport, and authorization/session policies.

**SmartCloud mapping:** In the current demo, the Spring Boot application acts as the MCP server. The React console can demonstrate calls, while a future external Agent host or `SmartCloudToolClient` adapter can act as an MCP client.

## 6. What is the difference between MCP and Function Calling? Have you run MCP in practice?

**Answer:** Function Calling is normally a model-provider or host-level structured action format: the host presents functions and executes the selected one. MCP is an interoperability protocol that standardizes how a host discovers and invokes externally hosted tools/resources/prompts. They are complementary: an Agent may discover tools via MCP and present adapted schemas to an LLM through Function Calling.

**SmartCloud mapping:** Yes, the project has a runnable MCP-style endpoint. It supports core JSON-RPC methods and traces calls. Be precise: it is a local protocol demo, not evidence of exhaustive compatibility with all MCP clients or production-grade session/auth behavior.

## 7. When would you use Function Calling and when MCP?

**Answer:** Use direct Function Calling when tools are few, internal, tightly coupled to one application, and you want minimum latency/complexity. Use MCP when tools should be independently deployed, discovered, reused by multiple Agent hosts, or owned by other teams/languages. In either case, authorization, validation, timeout, rate limit, and audit belong on the server side.

**SmartCloud mapping:** Finance and product operations can begin as internal Java tool services. The MCP facade makes those capabilities reusable by another Agent host or external workflow without exposing internal implementation details.

## 8. Why do some reasoning models not support MCP?

**Answer:** MCP is not a property of the model weights alone; it requires host/client support to discover tools, serialize requests, execute actions, and feed observations back. Some reasoning-model APIs may not expose tool-call output, may use a different response protocol, may prioritize hidden reasoning traces over interleaved actions, or may lack stable tool-use guarantees. An adapter host can sometimes bridge the gap, but it cannot guarantee the model will choose tools well.

**Interview nuance:** Say “may not support MCP in a given product integration,” not “a reasoning model is fundamentally incompatible with MCP.”

## 9. What is a Skill?

**Answer:** A Skill is reusable procedural capability: instructions, policy, examples, tools, and sometimes code/assets that teach an Agent how to complete a class of tasks. It answers “how should the Agent perform this workflow?” rather than simply exposing one callable operation. Skills can be static prompt packages or versioned executable workflow modules.

**SmartCloud mapping:** A future “ICP filing skill” could bundle eligibility checks, required materials, a step sequence, allowed tools, and escalation conditions. Today, much of that procedure is represented by route prompts and `IcpServiceAgent`, not a fully separate skill registry.

## 10. What is the difference between MCP and Agent Skill?

**Answer:** MCP defines how a client interoperates with an external capability provider. A Skill defines the reusable know-how or procedure for completing a task. MCP may expose a tool that a Skill uses; a Skill may call several MCP tools and RAG sources. One is a protocol/interface layer, the other is a task-level capability package.

**SmartCloud mapping:** `/mcp` exposes tools such as `icp.checklist`; the ICP domain workflow and prompt/routing rules are the beginnings of an ICP skill.

## 11. Compare Function Calling, Skill, and MCP.

| Concept | Main question answered | Typical scope | SmartCloud example |
| --- | --- | --- | --- |
| Function Calling | Which bounded operation should the model request now? | One model-host action | Structured billing query arguments |
| Skill | How should the Agent complete this recurring task? | Procedure, instructions, tools, policy | ICP filing service procedure |
| MCP | How can an AI host discover and call external capabilities? | Cross-process/protocol integration | `/mcp` tool discovery and invocation |

**Short answer:** Function Calling is an action format, Skill is reusable know-how, and MCP is an interoperability protocol.

## 12. What is A2A? How is it different from MCP?

**Answer:** A2A, Agent-to-Agent, is a protocol pattern for one agent to discover another agent's capabilities and delegate a task, receiving status and artifacts. MCP is primarily about an AI application accessing tools/resources/prompts from a server. A2A is agent delegation; MCP is capability/tool integration. In real systems they can coexist: an Agent can use MCP tools internally and expose a higher-level capability through A2A.

**SmartCloud mapping:** The project exposes `/.well-known/agent-card.json`, `/message:send`, and `/message:stream`. `OpsMarketingAgent` requests product context from the ProductTech side before generating a marketing package.

## 13. Which transports does MCP commonly use?

**Answer:** MCP has commonly used local stdio transport for child-process/local integrations and HTTP-based transports for remote servers, including Streamable HTTP in newer specifications. Older ecosystems also used HTTP plus SSE patterns. The important point is that transport carries protocol messages; JSON-RPC semantics, initialization, sessions, authentication, and cancellation must still be designed correctly.

**SmartCloud mapping:** `/mcp` accepts JSON-RPC-style POST requests and provides an SSE readiness response. Describe it as a Streamable-HTTP-oriented demo rather than claiming complete session semantics.

## 14. Compare WebSocket and SSE, including limitations.

**Answer:** SSE is server-to-client, unidirectional streaming over HTTP; it is simple for token streaming, works well with HTTP infrastructure, supports reconnection via event IDs, but client-to-server actions need normal HTTP requests and some proxies impose connection limits/timeouts. WebSocket is a persistent full-duplex connection, suitable for frequent bidirectional events, but needs connection lifecycle, heartbeat, backpressure, load-balancer affinity, and often more operational care.

**SmartCloud mapping:** Chat tokens and A2A progress use SSE because the user mainly receives streamed output after sending an HTTP request. WebSocket would be considered for live co-editing, interactive interruption/control signals, or high-frequency bidirectional agent collaboration.

## 15. Why use WebRTC? What is the key difference from WebSocket in AI dialogue streaming?

**Answer:** WebRTC is designed for low-latency real-time media and peer-to-peer data, including audio/video, jitter handling, NAT traversal, and adaptive media transport. WebSocket is client-server message transport and is excellent for text/event signaling but does not provide real-time media features. For ordinary text LLM token streaming, SSE or WebSocket is usually simpler; WebRTC is justified for real-time voice/video assistants where media latency and bidirectional audio matter.

**SmartCloud mapping:** SmartCloud is text/SSE-oriented and does not need WebRTC today. A future voice cloud-support assistant could use WebRTC for audio while keeping backend tool/RAG calls over ordinary service protocols.

## 16. Have you used an LLM gateway? What does a gateway solve?

**Answer:** An LLM gateway centralizes model-provider access: credentials, model routing/fallback, request normalization, quotas, rate limits, retries, caching, budget controls, safety filters, observability, and audit. It prevents every product service from embedding provider-specific SDK logic and makes multi-provider migration safer. It is not a substitute for application-level authorization or RAG/tool guardrails.

**SmartCloud mapping:** The current project centralizes DashScope model construction through `ModelFactory` and configuration, but it does not yet deploy a separate LLM gateway product. A production evolution could place LiteLLM, an API gateway with an LLM policy plugin, or an internal model gateway in front of DashScope/other providers and emit per-route cost/latency metrics.

---

# Final rehearsal checklist

1. Use the exact component names when describing SmartCloud: `ChatService`, `MultiAgentOrchestrator`, `AgentWorkflowGraph`, `SupervisorAgent`, specialized agents, `RagService`, `VerifierAgent`.
2. Mention the actual models only when asked: `qwen-turbo`, `text-embedding-v3`, and `gte-rerank-v2` are configured defaults.
3. Mention actual RAG baseline: Qdrant dense retrieval + Lucene BM25 lexical retrieval + reranking; configured chunk size is 800 with overlap 120.
4. Say “MCP/A2A runnable demo with core contracts” rather than “complete industrial protocol platform.”
5. Never invent a corpus size, QPS, latency, cost reduction, or evaluation score. Explain how you would measure it instead.
