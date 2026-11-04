# 开发与移植指南（简明版）

> 目标：把这套「**Jena 4.8 + Ontop 5.5 + 多模型推理 + 文档驱动本体建模**」能力快速复制到另一台机器、另一个 Spring Boot 工程或另一个业务项目中。
>
> 本文只讲「怎么搬、改哪里、别踩什么坑」。原理与完整 API 见：
> [本体建模模块文档](ontology-modeling.md)；Ontop 依赖细节见 [Ontop 5.5 POM 指南](ontop-5.5-pom.md)。

## TL;DR（最快移植路径）

1. 复制 `pom.xml` + `src/main/java/com/example/ontology/**` + `src/main/resources/application.yml` + `ontology/` + `mapping/` + `sql/`；
2. 改 4 处：数据源 URL/账号、`app.data-dir`、`OntologyVocabulary.NS`（换自己的命名空间）、T-Box/OBDA/SQL 业务内容；
3. 用 **Java 11** 执行 `./scripts/build.sh -B -DskipTests package` → `./scripts/run-server.sh` → `./scripts/smoke-test.sh`；
4. 只想要「文档生成本体」：可以不接 TDSQL，保留**文件型 A-Box** 加载即可；只想要「推理」：删掉 `modeling/` 与 PDFBox 依赖。

---

## 1. 技术栈基线（建议照搬，不要随手升级）

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | **11** | 当前基线；Jena 4.8 / Ontop 5.5 字节码均为 55 |
| Spring Boot | **2.7.18** | 最后一代支持 Java 11 的 Boot 2.7 |
| Maven | 3.8+ | 项目自带 `scripts/build.sh` 自动选 JDK 11 + 镜像 |
| Apache Jena | **4.8.0** | `jena-core` + `jena-arq` |
| Ontop | **5.5.0** | `ontop-rdf4j` + `ontop-system-sql-owlapi` |
| RDF4J / OWLAPI | 5.1.4 / 5.5.1 | 由 Ontop 传递引入，不要显式乱配版本 |
| MySQL/TDSQL 驱动 | Connector/J **8.0.33** | TDSQL 兼容 MySQL 协议 |
| H2 | 2.2.220 | 零安装演示 / 测试 |
| PDFBox | **2.0.31** | 仅文档建模的 PDF 文本抽取 |
| springdoc | 1.7.0 | Swagger UI |
| Jackson | **2.15.4**（BOM 覆盖） | 满足 Jena/Ontop 与 Boot 2.7 的兼容 |

关键判断：

- **Spring Boot 版本与 Jena/Ontop 版本解耦**，Ontop/Jena 是普通 Java 库，jar 中没有 Spring 依赖；
- 锁 Boot 2.7 的原因是 **Java 11**，不是 Ontop；
- 如允许 Java 17，可按 [docs/boot3-upgrade.md](boot3-upgrade.md) 升至 Boot 3.5.x：主要改 pom + 9 个 `javax.*` → `jakarta.*`，推理/建模业务代码不动。

---

## 2. 模块边界（先决定搬多少）

```
com.example.ontology
├── OntologyApplication.java
├── config/            # AppProperties、异常处理、CORS、OpenAPI、启动日志
├── controller/        # /load、/reason、/query、/models、/explain
├── service/           # ModelService、ReasoningService、QueryService
├── domain/            # OntologyModel、ModelRegistry 等领域对象与异常
├── dto/               # 请求/响应 DTO
├── infrastructure/    # Ontop 虚拟图、Jena 引擎、ModelLoader、文件/DB 适配
└── modeling/          # 文档驱动本体建模（可整体裁剪）
    ├── config/        #   app.modeling.* 配置
    ├── controller/    #   /modeling/projects/*、/modeling/ai/status
    ├── service/       #   项目流程编排、载入推理
    ├── domain/ dto/
    └── infrastructure/#   文档解析、分块、AI 客户端、Turtle 生成、项目文件存储
```

依赖方向：`controller → service → infrastructure`；`modeling.service` 复用核心 `ModelService` / `ReasoningService`，
**核心模块不反向依赖 modeling**，因此 modeling 可以单独摘除或替换。

### 三种运行模式

| 模式 | T-Box | A-Box | 是否用 Ontop/TDSQL | `profile` / `aboxSource` |
| --- | --- | --- | --- | --- |
| OBDA（正式） | 本地 TTL | TDSQL 关系表，OBDA 映射 | 是 | `tdsql` 或 `h2` / `tdsql` |
| 文件型（生成/离线） | 本地 TTL | 本地 `instances.ttl` | **否** | `local` / `local` |
| 混合 | 本地 TTL | TDSQL + 本地实例 | 是 | `tdsql` / `mixed` |

文件型模式是文档建模能接入推理系统的关键：`POST /load` 传 `instancesPath` / `instancesContent` 即可，不需要数据库。

---

## 3. 移植步骤（按顺序做）

### Step 0：环境

```bash
java -version        # 需要 11
mvn -version         # 3.8+
# 可选：本地 MySQL 8 / TDSQL（正式模式）；Ollama（AI 抽取）
```

### Step 1：复制代码与资源

最小需要复制：

```
pom.xml
scripts/build.sh  scripts/run-server.sh
src/main/java/com/example/ontology/**
src/main/resources/application.yml
src/main/resources/static/**        # 可选：演示 UI
ontology/company.ttl                 # T-Box
ontology/company.rules               # 业务规则（可选）
mapping/company.obda                 # OBDA 映射（TDSQL 模式必需）
sql/{create-database,schema,seed}.sql # TDSQL 模式
```

不要复制：`target/`、`data/`、`.idea/`（运行期生成，已在 `.gitignore`）。

### Step 2：改造 pom

直接以本项目的 [pom.xml](../pom.xml) 为模板。核心依赖：

```xml
<properties>
  <java.version>11</java.version>
  <jena.version>4.8.0</jena.version>
  <ontop.version>5.5.0</ontop.version>
  <jackson-bom.version>2.15.4</jackson-bom.version>
</properties>

<!-- Jena -->
<dependency>org.apache.jena:jena-core:4.8.0</dependency>
<dependency>org.apache.jena:jena-arq:4.8.0</dependency>

<!-- Ontop（注意 exclusions，见下） -->
<dependency>it.unibz.inf.ontop:ontop-rdf4j:5.5.0</dependency>
<dependency>it.unibz.inf.ontop:ontop-system-sql-owlapi:5.5.0</dependency>

<!-- 文档建模可选 -->
<dependency>org.apache.pdfbox:pdfbox:2.0.31</dependency>
```

必须保留/必须排除的依赖（踩坑高发区）：

- **不要排除** `org.apache.commons:commons-rdf-rdf4j`：Ontop 启动需要其 Guice 绑定，排了会 `UnknownClassException`；
- **要排除** Ontop 传递链里的旧 `rdf4j-queryparser-serql`：否则启动/查询出现 `NoSuchFieldError: SERQL`；
- Ontop 传递的旧 `logback-classic/core`、`log4j` 排除，避免与 Spring Boot 日志冲突；
- 不建议显式声明 RDF4J/OWLAPI 版本，交给 Ontop 传递依赖统一管理；
- `spring-boot-starter-parent` 与 `jackson-bom` 同时使用时，用 `jackson-bom.version` 覆盖默认 Jackson。

完整的依赖树、许可与可复制的 POM 见 [ontop-5.5-pom.md](ontop-5.5-pom.md) 和 `docs/examples/`。

### Step 3：配置

`src/main/resources/application.yml` 的最小配置：

```yaml
server:
  port: 8080

app:
  data-dir: data/models              # 加载模型时保存的本体/映射/实例副本
  default-profile: tdsql             # tdsql | h2
  reasoning:
    materialize-enabled: true        # 文件型 A-Box 会自动跳过物化
    materialize-table: inferred_triple
  datasource:
    tdsql:
      driver: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://127.0.0.1:3306/ontology_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: ontology
      password: ontology123
    h2:
      driver: org.h2.Driver
      url-template: jdbc:h2:mem:onto_{modelId};MODE=MySQL;DB_CLOSE_DELAY=-1
      username: sa
      password: ""

  modeling:                           # 不需要文档建模可整段删除
    data-dir: data/modeling
    namespace-base: http://example.org/model/
    chunk-size: 3500
    max-chunks: 15
    max-document-bytes: 5242880
    ai:
      provider: ollama
      base-url: http://127.0.0.1:11434
      model: qwen2.5:7b
      timeout-seconds: 180
      fallback-to-heuristic: true
```

所有配置均可通过环境变量覆盖，例如：

```bash
export APP_DATASOURCE_TDSQL_URL='jdbc:mysql://host:3306/ontology_db?...'
export APP_DATASOURCE_TDSQL_PASSWORD='...'
export APP_MODELING_AI_PROVIDER=heuristic
export APP_MODELING_AI_MODEL=qwen2.5:7b
```

### Step 4：替换成本领域本体（可选）

如果只搬「框架」，保留 `company.ttl / company.obda / company.rules / schema.sql` 即可先跑通；
换成自己的业务时：

1. 新写 T-Box（类、属性、公理）；
2. 新写 OBDA 映射（表/列 → 类/属性）；
3. 新写业务规则（可选，Jena ARQL）；
4. 改 SQL 建表与种子数据；
5. 改 `OntologyVocabulary.NS` / 前端示例中的命名空间（或直接用文件型 A-Box，避免映射）。
6. 若只需要「文档生成模型」，可以完全不用 TDSQL/OBDA，只保留文件型 A-Box。

### Step 5：构建与启动

```bash
./scripts/build.sh -B -DskipTests package
./scripts/run-server.sh
# 或
java -jar target/ontology-reasoning-server.jar
```

打开：

- 工作台：<http://localhost:8080/>
- 本体建模：<http://localhost:8080/modeling.html>（若保留 modeling）
- Swagger：<http://localhost:8080/swagger-ui.html>
- 健康检查：<http://localhost:8080/actuator/health>

### Step 6：验收

```bash
./scripts/build.sh test              # 期望 51/51
./scripts/smoke-test.sh              # 需要服务已启动，H2 演示模式
```

若只需核心推理，删掉 modeling 与 PDFBox 后，按 `OntologyReasoningApiTest` 和 `RdfBridgeTest` 的用例改造验收。

---

## 4. 核心扩展点（移植时优先看）

| 类 | 作用 | 移植时改什么 |
| --- | --- | --- |
| `config/AppProperties` | 读取 `app.*` 配置 | 数据源、数据目录、物化开关 |
| `controller/ModelController` | `/load`、模型列表/详情/卸载 | 一般不改 |
| `service/ModelService` | 解析来源、落盘、加载、注册 | 来源解析/默认值 |
| `infrastructure/loader/ModelLoader` | 校验 T-Box、建 Ontop、读 A-Box、建 Jena | 换数据库/接入方式 |
| `infrastructure/ontop/OntopVirtualGraph` | SPARQL→SQL 虚拟图 | 换 DB 时改驱动/方言 |
| `infrastructure/reasoning/JenaReasoningEngine` | 规则 + OWL 推理、SPARQL、解释 | 换业务规则/推理配置 |
| `infrastructure/store/ModelRegistry` | 多模型注册表 | 一般不改 |
| `modeling/infrastructure/ai/LlmClient` | AI 客户端抽象 | 接公司大模型网关 |
| `modeling/infrastructure/extract/*` | 文档解析/规则抽取 | 换文档格式/领域规则 |
| `modeling/infrastructure/build/OntologyTurtleBuilder` | T-Box/A-Box 生成 | 换 IRI 规范/公理风格 |

推荐扩展方式：**新增实现类**（新的 `LlmClient`、新的 `DocumentTextExtractor` 分支、新的 T-Box 模板），
而不是修改核心接口；这样后续同步本仓库更新更省事。

---

## 5. 最小集成 API

```bash
BASE=http://localhost:8080

# 1) 加载 TDSQL 模型（T-Box + OBDA 使用默认本地文件）
curl -s -X POST $BASE/load -H 'Content-Type: application/json' \
  -d '{"modelId":"company","profile":"tdsql"}'

# 2) 加载文件型 A-Box 模型（无数据库）
curl -s -X POST $BASE/load -H 'Content-Type: application/json' \
  -d '{"modelId":"my-model","ontologyPath":"/abs/ontology.ttl","instancesPath":"/abs/instances.ttl"}'

# 3) 多模型：切换 modelId 查询
curl -s -X POST $BASE/reason -H 'Content-Type: application/json' \
  -d '{"modelId":"company"}'
curl -s -X POST $BASE/query -H 'Content-Type: application/json' \
  -d '{"modelId":"company","sparql":"SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10"}'

# 4) 文档建模一步完成（heuristic 不需要 AI）
curl -s -X POST $BASE/modeling/projects -H 'Content-Type: application/json' \
  -d '{"name":"demo","fileName":"law.txt","content":"第一章 总则\n第一条 ……"}'
# 取返回的 projectId 后：
curl -s -X POST $BASE/modeling/projects/$PID/pipeline -H 'Content-Type: application/json' \
  -d '{"extract":{"provider":"heuristic"},"generate":{},"load":{"autoReason":true}}'
```

HTTP 调用约定：成功返回 JSON；失败统一 `ErrorResponse {timestamp,status,code,message,path,details}`。

---

## 6. 高频踩坑清单

| 坑 | 现象 | 处理 |
| --- | --- | --- |
| 用默认 Java 27 跑 Maven | 依赖下载慢/编译异常 | 用 `./scripts/build.sh` 或显式 `JAVA_HOME` 指向 JDK 11 |
| 排除了 `commons-rdf-rdf4j` | Ontop 启动 `UnknownClassException` | 不要排 |
| 未排除旧 `rdf4j-queryparser-serql` | 启动/查询 `NoSuchFieldError: SERQL` | 排除 |
| H2 URL 加了 `DATABASE_TO_LOWER=TRUE` | Ontop 找不到 `EMPLOYEE` 表 | 去掉该参数 |
| SPARQL 前缀名含 `/`（如 `:emp/5`） | 语法错误 | 用完整 IRI `<http://...#emp/5>` |
| 生成模型查询 `:` 前缀查不到 | `:` 被绑定到 demo 命名空间 | 现版本已按模型动态绑定；老版本需手动写全 IRI |
| 文件型模型 `/reason` 物化报错 | 没有 TDSQL 表 | 文件型模型自动关闭物化（`materializeEnabled=false`） |
| 生成模型继承了 demo 业务规则 | 推导出奇怪结果 | 现版本规则默认只用于 OBDA/demo 模式；不要显式给文件模型传默认 `rulesPath` |
| 前端/接口报 415/400 | `/load` 带错 Content-Type | `/load` 不限制 consumes；上传用 multipart |
| PDF 全是空白/乱码 | 扫描版 PDF 只有图片 | 先 OCR；PDFBox 只抽文本层 |
| Ollama 抽取很慢/超时 | 大模型首次加载 + 长输出 | 用 7B 级模型，`timeout-seconds=180`，或 `provider=heuristic` |
| AI 抽取把文档发到外部 | 安全合规 | 用本地 Ollama 或 `heuristic`；不要对涉密文档用外部 API |

---

## 7. 验收标准

移植完成后至少确认：

- [ ] `./scripts/build.sh test` → **51/51 通过**
- [ ] `POST /load` + `POST /reason` + `POST /query` 正常（TDSQL 或 H2）
- [ ] 文件型 A-Box 模型可加载、推理、查询（无数据库环境）
- [ ] `GET /models` 可看到多个模型，按 `modelId` 切换正常
- [ ] 若保留建模：`/modeling.html` 可用；`pipeline` 生成并载入成功
- [ ] 若配置 Ollama：`GET /modeling/ai/status` 返回 `reachable=true` 且模型列表正确
- [ ] `./scripts/smoke-test.sh` 全绿

---

## 8. 延伸阅读

- [README](../README.md)：项目总览、完整 API、配置、故障排查
- [docs/ontology-modeling.md](ontology-modeling.md)：文档建模模块完整设计
- [docs/ontop-5.5-pom.md](ontop-5.5-pom.md)：Ontop 5.5 依赖/许可/POM
- [docs/boot3-upgrade.md](boot3-upgrade.md)：Java 17 + Boot 3.5 迁移路径
- `docs/examples/`：可直接复制的最小 Ontop POM / Ontop+Jena+Boot POM
