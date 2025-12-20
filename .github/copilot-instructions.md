# Copilot Instructions for J-Dep Analyzer 2 (Java Edition)

## 1. 角色与目标
你是一位全栈 Java 工程师（精通 Spring Boot 和数据可视化）。
**目标**：维护一个 Web 应用程序，允许用户上传 Maven `pom.xml` 文件，解析依赖关系，并通过交互式 Web 界面展示依赖网络、列表以及动态聚合的依赖树。

## 2. 技术栈 (Web Focus)
- **运行环境**: Java 17+
- **构建工具**: Maven (with Maven Wrapper)
- **后端框架**: **Spring Boot 3.2+** (Spring Web, Spring Data JPA)
- **模板引擎**: **Thymeleaf** (用于服务端渲染 HTML)
- **核心逻辑**:
  - **Java DOM/XPath**: 解析 XML (pom.xml)
  - **JGraphT**: 构建图结构、合并节点、计算依赖路径
  - **Spring Data JPA**: 数据库 ORM
    - **Local**: SQLite (`jdbc:sqlite:dependencies.db`)
    - **GCP**: Cloud SQL PostgreSQL via `postgres-socket-factory` (IAM Auth)
- **前端技术**:
  - **HTMX**: 用于处理无刷新的文件上传、表格搜索和视图切换
  - **Cytoscape.js**: **强制使用**此库在前端渲染交互式依赖图（支持节点合并、布局切换）
  - **Tailwind CSS** (通过 CDN): 用于快速构建现代 UI

## 3. 数据模型规范
- **Artifact (GAV)**:
  - `groupId`: String (默认为 "Unknown")
  - `artifactId`: String (必须存在)
  - `version`: String (默认为 "Unknown")
  - **唯一性逻辑**: `gav` 字段存储完整的 `groupId:artifactId:version` 作为唯一标识
- **DependencyEdge**:
  - 必须记录 `scope` (compile, test, parent, etc.)
  - `fromGav` -> `toGav` 表示依赖关系

## 4. 核心业务逻辑指令
- **解析容错**：如果在 `pom.xml` 的 properties 或 parent 中找不到 version，**不要报错**，直接赋值为 "Unknown"
- **节点聚合 (Node Aggregation)**:
  - 提供后端服务，能够根据用户开关（Show Group? Show Version?），动态重组 JGraphT 图
  - **合并规则**：如果用户隐藏 Version，则所有 `log4j:1.2` 和 `log4j:2.0` 的节点应合并为同一个 `log4j` 节点
- **依赖方向**:
  - Forward 视图：A depends on B (A -> B)
  - Reverse 视图：Who depends on B (显示 B 的前驱节点)

## 5. [UI/UX Design System] - Modern Material (Indigo/Slate)

**核心原则**：现代、简洁、Material UI 风格 (Inspired)，使用 Tailwind CSS 构建。

### 5.1. 配色方案 (Color Palette)
- **Primary Brand**: Indigo (`#6366f1` / `primary-500` to `#4f46e5` / `primary-600`)
- **背景 (Backgrounds)**:
  - App Background: `#f8fafc` (Slate 50)
  - Surface/Card: `#ffffff` (White)
  - Hover/Highlight: `gray-50` or `indigo-50`
- **文字 (Typography Colors)**:
  - Primary Text: `#1e293b` (Slate 800)
  - Secondary Text: `#64748b` (Slate 500)

### 5.2. 字体策略 (Typography)
- **UI 字体 (Sans)**: `Inter`, `Roboto`, `sans-serif`
- **代码字体 (Mono)**: `Fira Code`, `monospace` (用于 GroupId, ArtifactId, Version)

### 5.3. 组件风格 (Component Styles)
- **阴影**: 使用 `shadow-mate-1`, `shadow-mate-2` 等
- **圆角**: 卡片用 `rounded-xl`，按钮用 `rounded-full` 或 `rounded-lg`
- **图标**: Google **Material Symbols Outlined**

## 6. Cloud Infrastructure & Database (GCP)
- **数据库连接原则**:
  - 生产环境**必须**使用 `postgres-socket-factory` 进行 CloudSQL 连接
  - 认证：优先开启 IAM Auth，避免硬编码密码
- **环境配置 (application.yml profiles)**:
  - `default`: SQLite (本地开发)
  - `postgresql`: CloudSQL PostgreSQL (生产)
- **环境变量**:
  - `JDEP_DB_HOST`: CloudSQL Instance Connection Name
  - `JDEP_DB_NAME`: 数据库名
  - `JDEP_DB_USER`: IAM 用户
  - `GOOGLE_APPLICATION_CREDENTIALS`: Service Account Key 路径

## 7. 前端图形规范 (Cytoscape.js Style)

### Nodes
- 完整 GAV：显示为圆角矩形
- 聚合节点：添加 `aggregated` class，使用不同样式（如更大尺寸）

### Layout
- **默认布局**: Concentric (同心圆)
- 层级布局：Dagre (Hierarchy) — 适合展示依赖树
- 网状布局：Cose — 适合复杂网络

### 交互
- 节点点击高亮
- 鼠标悬停显示详情
- 双击跳转到 Visualize 页面

## 8. URL 路由规范

| 类型 | 前缀 | 示例 |
|------|------|------|
| 首页 | (无) | `/` |
| 页面路由 | `/page` | `/page/dependencies/list`, `/page/visualize/{id}`, `/page/export` |
| 数据接口 | `/api` | `/api/upload`, `/api/graph/data`, `/api/export/{table}.csv` |

**原则**：
- 首页 `/` 是唯一不带前缀的页面
- 所有返回 HTML 的页面使用 `/page` 前缀
- 所有返回 JSON/CSV 等数据的接口使用 `/api` 前缀

## 9. 文档编写规范
- **禁止**在文档中添加冗余的"项目结构"章节 — 这些信息可以通过代码仓库直接查看
- **禁止**在文档中罗列所有文件路径 — 除非是为了说明特定的架构决策
- 文档应聚焦于：设计决策、业务逻辑、API 契约、非显而易见的约定
- 保持文档简洁，避免重复代码库中已经显而易见的信息

## 10. API 集成测试规范 (BDD-style)

所有 `/api` 数据端点**必须**有对应的自动化集成测试。当新增功能或修改现有行为时，需同步更新测试。

### 10.1. 技术要求
- **框架**: Spring Test + JUnit 5 (不使用 BDD 框架如 Cucumber)
- **运行方式**: 启动完整 Spring 容器，调用真实 HTTP 端点
- **数据库**: SQLite 内存数据库 (`jdbc:sqlite:file::memory:?cache=shared`)
- **配置文件**: `src/test/resources/application-test.yml`

### 10.2. 测试结构
```java
@Test
@DisplayName("Given [前置条件], when [操作], then [预期结果]")
void methodName_shouldExpectedBehavior() {
    // Given: 前置条件描述
    // When: 执行操作
    // Then: 验证结果
}
```

### 10.3. 测试隔离
- 每个测试用例**独立**，互不依赖
- `@BeforeEach` 清空数据库
- 每个测试自行 setup 所需数据
