# Design Doc for J-Dep Analyzer 2 (Java Edition)

## 1. 系统架构 (Web App)

本工具（J-Dep Analyzer 2）是 J-Dep Analyzer 的 Java 重写版本，旨在解决微服务架构下 Maven 依赖管理混乱的问题。它能扫描整个文件夹下的所有 Java 项目，构建一个全量的**依赖知识图谱**。

采用标准的 MVC 架构：

- **Model**: Spring Data JPA Entities (SQLite/PostgreSQL 存储解析后的原子数据)
- **Controller**: Spring REST Controllers 处理上传、查询和图数据生成
- **View**: Thymeleaf Templates + Cytoscape.js Canvas

## 2. 数据模型 (Schema)

### 2.1 Artifact (Node)

```java
@Entity
public class Artifact {
    @Id @GeneratedValue
    private Long id;
    
    @Column(unique = true)
    private String gav;  // groupId:artifactId:version

    private String groupId;
    private String artifactId;
    private String version;
}
```

### 2.2 DependencyEdge (Edge)

```java
@Entity
public class DependencyEdge {
    @Id @GeneratedValue
    private Long id;
    
    private String fromGav;  // Source artifact GAV
    private String toGav;    // Target artifact GAV
    private String scope;    // compile, test, parent, etc.
    private Boolean optional;
}
```

## 3. 功能模块设计

### 3.1 文件上传与解析 (`POST /api/upload`)

- **UI**: 提供一个拖拽上传区域 (Dropzone)，支持一次上传多个 `pom.xml`
- **Logic**:
  - 接收 `List<MultipartFile>`
  - 使用 Java DOM/XPath 解析
  - 遇到 `${...}` 无法解析时，Version 存为 "Unknown"
  - **Parent 处理**：将 `<parent>` 记录为一条依赖边（`scope="parent"`）
  - Upsert 逻辑：如果 GAV 已存在，忽略；否则插入 DB

### 3.2 视图 A: 全局依赖概览 (`GET /`)

- **功能**: Dashboard 展示 DB 中所有 Artifact 的关系网
- **聚合参数**: `?show_group=bool&show_version=bool`
- **算法 (Graph Aggregation)**:
  - 从 DB 加载全量原子图 (Atomic Graph) 到 JGraphT
  - 根据开关动态合并节点
  - 返回 Cytoscape.js Elements JSON

### 3.3 视图 B: 依赖对列表 (`GET /dependencies/list`)

- **UI**: 双栏布局表格
- **交互**:
  - 顶部 Filters (ArtifactId, GroupId, Scope)
  - Checkbox: "Combine Versions", "Combine Groups"
  - 双击行跳转到 Visualize 页面
  - Export CSV 按钮 → 调用 `/api/dependencies/export`
- **Export CSV 格式**: `source_group,source_artifact,source_version,target_group,target_artifact,target_version,scope`
  - 支持所有筛选参数 (q, group_q, scope, ignore_version, ignore_group)

### 3.4 视图 C: 详细依赖透视 (`GET /visualize/{gav}`)

- **UI**:
  - 左侧：节点信息卡片
  - 右侧：以该节点为中心的局部图谱
- **功能**:
  - **Forward Tree**: 它依赖了谁？ (1层, 2层, All)
  - **Reverse Tree**: 谁依赖了它？ (Impact Analysis)

### 3.5 视图 D: 数据导出 (`GET /export`)

- **功能**：为 DB 中的每个表提供导出为 CSV 的链接
- **下载**：`GET /export/{table}.csv`

## 4. API 接口设计 (Spring Controllers)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/upload` | 上传 POMs |
| GET | `/api/artifacts` | 获取 Artifact 列表 (JSON) |
| GET | `/api/graph/data` | 获取图数据 (Cytoscape format) |
| GET | `/api/dependencies/table` | 获取依赖表格 (HTML) |
| GET | `/api/dependencies/export` | 导出筛选后的依赖 CSV |
| GET | `/api/export/{table}.csv` | 导出原始表为 CSV |
| GET | `/` | Dashboard 页面 |
| GET | `/page/dependencies/list` | 依赖列表页面 |
| GET | `/page/visualize/{gav}` | 可视化详情页 |
| GET | `/page/export` | 导出页面 |

