# AGENTS.md — AI Agent 研发协作约束

> 本文件约束 Agent 在本项目中的全部写码行为。任何任务启动前，先读本文件、`CLAUDE.md`、`CONTEXT.md`、`docs/agents/`、相关 ADR、现有规格和 Issue，判断当前阶段后再行动。

## 项目路径与后端验证环境（全局）

- 后端项目根目录唯一标准是：`D:\AAA_myProjects\spring项目\智能云图库项目\PIXORA\picture-backend`。
- 外层目录 `D:\AAA_myProjects\spring项目\智能云图库项目\picture-backend` 不是当前 PIXORA 后端，禁止将其作为构建或代码检查目录。
- 后端 POM 要求 Java 17。执行 Maven 前确认 `JAVA_HOME` 指向 JDK 17，并记录 `mvn -version`。
- 当前机器的 Maven 3.9.16 不在 PATH。若 `mvn` 无法识别，应使用已安装的 `mvn.cmd` 绝对路径，或先定位 `C:\Users\chen\.m2\wrapper\dists` 下的 Maven，不要把“找不到 mvn”当成项目构建失败。
- 后端全量编译的标准验证命令是：

  ```powershell
  $env:JAVA_HOME = 'C:\Users\chen\.jdks\ms-17.0.17'
  $mvn = 'C:\Users\chen\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd'
  Set-Location 'D:\AAA_myProjects\spring项目\智能云图库项目\PIXORA\picture-backend'
  & $mvn -version
  & $mvn -DskipTests clean compile
  ```

- 优先以用户本机/IDE 使用的 Maven 用户目录验证。Codex 沙箱可能使用 `C:\Users\CodexSandboxOffline\.m2` 和受限文件系统；若仅在该环境出现 `无法关闭编译器资源`、并点名依赖 JAR，而用户本机同目录的 `clean compile` 成功，应将其记录为测试环境差异，不得据此修改业务代码或依赖版本。
- 验证报告必须同时写明：绝对工作目录、Java 版本、Maven 版本、是否执行了 `clean`，以及完整构建结果。

## 总控原则（零号约束）

### 一、先判断阶段，再行动
- 始终先判断当前任务处于以下哪个阶段：**需求澄清 / 规格编写 / 任务拆分 / 实现 / 诊断 / 审查**。
- 不要用立即修改代码或立即回答的方式代替阶段判断。
- 阶段判断不确定时，使用 `ask-matt` Skill 辅助判断，并向用户说明推荐流程、推荐顺序、每步产物、哪些决策必须由用户确认。

### 二、人工控制边界（硬约束）
以下内容**必须由人工确认**，Agent 不得自行决定：
- 业务范围（做不做、做什么、不做什么）
- 数据含义（字段语义、枚举值、业务规则）
- 权限设计（谁能读、谁能写、谁能操作）
- 外部发布（发版、打标签、创建 PR、推送代码、发布文档）
- 不可逆操作（删除数据、迁移 Schema、修改生产配置）

### 三、分阶段交付确认
- 每个阶段完成后，**先展示产物和未决问题，等待用户确认**，再进入下一阶段。
- 不自动进入用户未显式调用的下一个 Skill 或下一个阶段。
- 对每一阶段的产出，明确标注：
  - 已验证事实
  - 基于代码的推断
  - 尚未验证的建议

### 四、证据驱动
- 所有实现必须有与风险相称的验证证据（测试、日志、复现步骤、对照实验）。
- 不根据代码阅读直接宣称根因或结论；有假设必须可被证伪并逐条验证。
- 无法建立反馈回路时，明确说明缺少什么证据，**不要猜测**。

### 五、不扩范围
- 不顺手重构无关代码。
- 不创建版本、标签、分支（除非明确要求）。
- 不发布任何外部内容（文档、PR、推送）。
- 不将清理、重构或未来扩展混入当前工单。

---

## 阶段工作流约束

### 阶段一：需求澄清（调用 `grill-with-docs`）

**触发条件**：需求模糊、描述不完整、存在术语歧义、用户说"我想做 X"但细节不清。

**执行约束**：
1. 先读取项目领域文档、相关代码和 ADR。
2. 沿决策树持续追问，**每次优先处理会改变范围、数据模型、权限、用户体验或验收标准的问题**。
3. 对可以从仓库确认的事实自行调查，**不要反问用户**。
4. 对每个问题给出一个推荐选项，并说明主要取舍。
5. 主动发现术语歧义，使用 `domain-modeling` 统一领域语言。
6. 将稳定术语更新到 `CONTEXT.md`；将难以逆转的架构决策记录为 ADR。
7. **不写实现代码**。

**阶段产物（必须输出）**：
```
问题定义
目标用户
核心场景
范围内
范围外
业务规则
异常与边界场景
验收标准
已确认决策
未决问题
```

**门禁**：只有关键决策树已经闭合后，才建议进入阶段二（`to-spec`）。

---

### 阶段二：规格编写（调用 `to-spec`）

**触发条件**：需求已经讨论清楚，需要形成可执行的技术规格。

**执行约束**：
1. **不重新进行完整访谈**；只指出阻止规格成立的缺失信息。
2. 检查相关代码、现有测试、领域词汇和 ADR。
3. 明确当前行为与目标行为的差异。
4. 使用 `codebase-design` 识别合理接缝，优先复用现有接缝。
5. 测试应尽量放在最高且稳定的行为边界，避免测试实现细节。
6. 每条验收标准必须**可观察、可判断通过或失败**。

**规格必须包含**：
- 功能范围 / 非目标
- 用户流程
- 数据与状态变化
- 接口或模块边界
- 权限与安全约束
- 失败处理
- 兼容性要求
- 可观察性要求

**阶段产物（必须标注）**：
- 已确认事实
- 设计决定
- 风险
- 仍需人工确认的事项

**门禁**：先向用户展示规格草案。**未经确认，不发布到 Issue Tracker**。

---

### 阶段三：拆分工单（调用 `to-tickets`）

**触发条件**：规格已经确认，需要拆分为可独立验证的工单。

**执行约束**：
1. 使用 **tracer bullet** 方式拆分，每个工单尽量贯穿所需的数据、后端、前端和测试。
2. **禁止按数据库、后端、前端、测试进行纯横向拆分**。
3. 每个工单必须包含：
   - 用户或系统价值
   - 范围
   - 明确非目标
   - 前置依赖
   - 涉及的主要模块
   - 验收标准
   - 测试接缝
   - 验证命令或完成定义
4. 标明工单之间的阻塞关系。
5. 标明 **AFK**（信息充分，Agent 可独立执行）和 **HITL**（需要人工业务决策或验收）。
6. 优先生成最小可运行、可验证的第一条纵向切片。
7. **不要把清理、重构或未来扩展混入当前工单**。

**门禁**：先展示工单结构和依赖图。经用户确认后再写入 Tracker。

---

### 阶段四：按工单实现（调用 `implement`）

**触发条件**：用户显式调用 `implement` 并指定工单编号或路径。

**开始前必须做**：
1. 读取完整工单、评论、规格、领域文档和相关 ADR。
2. 用一句话复述本次目标。
3. 列出范围内、范围外、测试接缝和完成标准。
4. 检查工作区现有改动，保护与本工单无关的用户修改。
5. 如果发现业务含义、权限、字段、接口契约或结果含义需要改变，**停止并请求确认**。

**实现约束（TDD）**：
1. 使用 `tdd`，按**红 → 绿 → 重构**执行。
2. 先写一个能因**正确原因**失败的测试，并运行确认失败。
3. 实现使测试通过的最少改动。
4. 一次只完成一个纵向切片。
5. 不进行无关重构，不增加未要求的配置能力。
6. 代码风格与现有项目保持一致。
7. 每完成一个切片，都运行相关测试和静态检查。
8. 如果测试失败，**不要通过删除测试、降低断言或扩大 Mock 来掩盖问题**。

**完成后必须做**：
1. 使用 `code-review` 对固定基线以来的变更执行双轴审查：
   - **Standards**：是否符合仓库规范
   - **Spec**：是否忠实实现规格
2. 修复明确且不改变用户意图的问题。
3. 输出：
   - 已完成内容
   - 修改文件
   - 测试和验证结果
   - 尚未完成内容
   - 风险与限制
   - 是否满足工单完成定义
4. **未经授权，不提交、不推送、不创建 PR、不发布、不打标签**。

---

### 阶段五：困难 Bug 诊断（调用 `diagnosing-bugs`）

**触发条件**：用户报告 Bug、错误现象、日志异常，需要定位根因。

**目标**：确定根因。**除非用户明确要求修复，否则先不要修改产品代码**。

**严格流程（必须按顺序执行）**：
1. 收集当前环境、版本、配置、日志和最近变更。
2. 建立快速、确定、可由 Agent 重复运行的反馈回路。
3. 实际复现问题，并记录完整错误。
4. 缩小复现范围，寻找最小失败案例。
5. 提出按可能性排序、可以被证伪的假设。
6. 为每个假设说明：支持证据 / 反对证据 / 可观察预测 / 验证方法。
7. 使用最小必要埋点或对照实验逐一把假设验证。
8. **不要根据代码阅读直接宣称根因**。
9. 确认根因后，指出正确的回归测试接缝和最小修复方案。

**门禁**：输出以下九项，缺一不可：
- 是否成功复现
- 根因
- 证据链
- 影响范围
- 建议修复
- 回归测试方案
- 仍未确认的风险

**额外约束**：如果无法建立反馈回路，明确说明缺少什么证据，**不要猜测**。

---

### 阶段六：架构体检（调用 `improve-codebase-architecture`）

**触发条件**：用户要求检查某个模块或范围的架构质量。

**重点寻找**：
1. 浅模块和泄漏抽象
2. 同一业务知识散落在多个位置
3. 调用者必须了解过多内部实现
4. 模块边界与领域边界不一致
5. 需要跨大量文件才能完成的小改动
6. 难以从稳定接口测试的代码
7. 循环依赖、反向依赖或入口文件失控
8. AI 生成代码造成的重复与结构熵

**执行约束**：
1. **只报告有代码证据的问题**。
2. 每个候选问题说明：
   - 具体证据
   - 当前修改成本
   - 建议边界
   - 预期收益
   - 迁移风险
3. 使用 `codebase-design` 分析是否能形成更深的模块。
4. 按**收益、风险和实施成本**排序。
5. **不要直接重构**。
6. 先让用户选择一个候选项，再通过追问明确目标。
7. 选定后使用 `to-spec` 和 `to-tickets` 形成可执行改造计划。

---

### 阶段七：交付前终审（调用 `code-review`）

**触发条件**：实现完成、准备交付之前。

**审查维度**：
1. 使用 `code-review`，以用户指定的基线（如 `main`）为固定点。
2. 分别审查：
   - **Standards**：仓库规范、代码异味、错误处理、可维护性
   - **Spec**：规格、Issue、验收标准、范围和 ―非目标
3. 额外检查：
   - 是否存在未验证路径
   - 是否误改无关文件
   - 是否遗留调试代码
   - 是否存在敏感信息
   - 是否破坏兼容性
   - 是否缺少数据库迁移或回滚考虑
   - 文档是否与实现同步

**输出约束**：
- 每条问题必须包含：证据、影响、最小修复建议。
- 区分**阻塞问题**与**非阻塞建议**。
- 没有发现问题时，也要说明实际检查了什么。

**最终结论必须是以下之一（三选一，不允许模糊）**：
- ✅ 可以交付
- ⚠️ 修复阻塞项后可以交付
- ❌ 当前不建议交付

---

## 底层可调用 Skill 索引

| Skill | 调用方式 | 核心用途 | 所属阶段 |
|-------|----------|----------|----------|
| `ask-matt` | 用户显式调用 | 判断当前阶段和推荐 Skill 顺序 | 全部 |
| `grill-with-docs` | 用户显式调用 | 沿决策树深度追问，澄清模糊需求 | 阶段一 |
| `to-spec` | 用户显式调用 | 将讨论整理为可执行技术规格 | 阶段二 |
| `to-tickets` | 用户显式调用 | 将规格拆为纵向可验证工单 | 阶段三 |
| `implement` | 用户显式调用 | TDD 驱动实现单个工单 | 阶段四 |
| `diagnosing-bugs` | 用户显式调用 | 系统性根因分析，不修改产品代码 | 阶段五 |
| `improve-codebase-architecture` | 用户显式调用 | 架构健康体检，报告问题并排序 | 阶段六 |
| `code-review` | 自动/用户调用 | 双轴审查（Standards + Spec） | 阶段四/七 |
| `research` | 自动调用 | 调研外部信息、API 文档、最佳实践 | 全部 |
| `prototype` | 自动调用 | 快速验证设计决策和交互方案 | 阶段一/二 |
| `domain-modeling` | 自动调用 | 统一领域语言，消除术语歧义 | 阶段一 |
| `codebase-design` | 自动调用 | 识别合理模块接缝，分析架构深度 | 阶段二/六 |
| `tdd` | 自动调用 | 红-绿-重构测试驱动开发 | 阶段四 |

---

## 快速决策树

```
收到任务
  └─ 先判断阶段
      ├─ 需求模糊？          → 阶段一：grill-with-docs
      ├─ 需求已清晰？        → 阶段二：to-spec
      ├─ 规格已确认？        → 阶段三：to-tickets
      ├─ 要写代码？          → 阶段四：implement（先 TDD）
      ├─ 有 Bug？            → 阶段五：diagnosing-bugs（先不复现不改代码）
      ├─ 要改架构？          → 阶段六：improve-codebase-architecture（只报告不重构）
      └─ 要交付？            → 阶段七：code-review（三选一结论）
```

---

## 禁止行为（Hard Fail）

出现以下任何一条，**立即停止并请求用户确认**：

1. 在未判断阶段的情况下直接修改代码。
2. 在未确认业务范围的情况下开始实现。
3. 删除测试、降低断言强度或扩大 Mock 来让失败测试通过。
4. 未经用户授权提交、推送、创建 PR、打标签或发布内容。
5. 声称根因却无复现步骤和证据链。
6. 在没有代码证据的情况下报告架构问题。
7. 在决策树未闭合时提前进入下一阶段。
8. 将无关重构、清理或未来扩展混入当前工单。

<!-- sol-luna:start -->
## Sol + Luna Orchestration

Use **division of labor rather than duplicated parallel work**.

Sol owns:
- requirement interpretation and acceptance criteria
- task decomposition and assignment
- architecture and priority decisions
- integration of subagent evidence
- conflict resolution
- final integration-level verification and response

Luna owns execution stages:
- exploration / call-chain tracing → `explorer`
- bounded implementation → `worker`
- reproduction / tests / regression checks → `tester`
- independent post-change review → `reviewer`
- difficult narrow investigation → `deep` only after Medium is insufficient

### No-duplication rule

Once Sol delegates a task, Sol should not independently redo that same exploration, implementation, test, or review.

Each Luna task must have a distinct objective and scope. Do not assign multiple agents to answer the same question unless conflicting evidence requires an explicit second opinion.

Parallelize only complementary independent work, for example:
- frontend investigation + backend investigation
- code exploration + independent reproduction
- separate non-overlapping modules

Avoid:
- Sol and Luna investigating the same issue in parallel
- two explorers inspecting the same scope
- multiple workers editing overlapping files
- `deep` and a Medium agent investigating the same question simultaneously

Use staged handoffs:

`User → Sol plans → Luna investigates → Sol decides → Luna implements → Luna verifies/reviews → Sol accepts → User`

Do not spawn every role automatically. Use only roles that add distinct value.

Sol may directly perform trivial work where delegation would add more overhead than value.
<!-- sol-luna:end -->
