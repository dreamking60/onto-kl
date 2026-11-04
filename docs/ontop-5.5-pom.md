# 公司内部引入 Ontop 5.5 的 POM 依赖清单

> 适用版本：**Ontop 5.5.0 + Apache Jena 4.8.0 + Java 11**（Java 17/21 也可运行，见文末说明）。
> 本文列出"要引入 Ontop 需要哪些 POM 依赖、哪些是传递引入、哪些需要显式声明"，并给出可直接复制的
> `pom.xml` 文件（也存放在 `docs/examples/` 下）。

---

## 0. 先回答两个问题

### Q1：引入 Ontop 是否必须同时引入 OWLAPI 的包？

**不需要显式引入。** Ontop 5.5.0 的 `ontop-system-sql-owlapi` 会传递引入完整的 **OWLAPI 5.5.1**
（`owlapi-api` 由 `ontop-system-sql-owlapi` 直接传递，`owlapi-apibinding` 及其实现模块由
`ontop-mapping-owlapi` 带入）：

```
it.unibz.inf.ontop:ontop-system-sql-owlapi:5.5.0
 ├─ net.sourceforge.owlapi:owlapi-api:5.5.1
 └─ it.unibz.inf.ontop:ontop-mapping-owlapi:5.5.0
     ├─ it.unibz.inf.ontop:ontop-ontology-owlapi:5.5.0
     └─ net.sourceforge.owlapi:owlapi-apibinding:5.5.1
         ├─ net.sourceforge.owlapi:owlapi-impl:5.5.1
         ├─ net.sourceforge.owlapi:owlapi-parsers:5.5.1
         ├─ net.sourceforge.owlapi:owlapi-oboformat:5.5.1
         ├─ net.sourceforge.owlapi:owlapi-tools:5.5.1
         └─ net.sourceforge.owlapi:owlapi-rio:5.5.1
```

只有下面这种情况才需要**显式声明** OWLAPI：

- 你们要在 Java 代码里直接操作 `OWLOntology` / `OWLAxiom`（遍历公理、合并本体、做本体差异）；
- 或者要接 OWL DL 推理机（HermiT / Openllet / ELK / JFact），它们基于 OWLAPI API。

显式声明时**版本必须保持 5.5.1**（与 Ontop 5.5.0 管理的版本一致），不要混 5.1.x 或其他版本：

```xml
<dependency>
    <groupId>net.sourceforge.owlapi</groupId>
    <artifactId>owlapi-apibinding</artifactId>
    <version>5.5.1</version>
</dependency>
```

### Q2：Ontop 5.5 的版本基线是什么？

| 组件 | 版本 | Java 基线 | 说明 |
| --- | --- | --- | --- |
| Ontop | **5.5.0** | Java 11（字节码 55） | Maven Central 当前最新 release |
| RDF4J | **5.1.4** | Java 11 | Ontop 核心查询/存储层 |
| OWLAPI | **5.5.1** | Java 11（字节码 55） | 本体解析（由 Ontop 传递引入） |
| Guice / HikariCP / H2 | 5.0.1 / 3.4.5 / 2.2.220 | Java 11 | Ontop 内部依赖 |
| Jackson | **2.15.4** | 无 | Jena 4.8 用 2.14.2，Ontop 5.5 用 2.15.4，需仲裁 |
| SLF4J | **1.7.36** | 无 | 与 Jena 4.8 一致；只保留一个 binding |
| Jena（公司已有） | **4.8.0** | Java 11 | 与 Ontop 无依赖关系，可同 JVM 共存 |
| TDSQL 驱动 | MySQL Connector/J **8.0.33** | Java 8+ | TDSQL 走 MySQL 协议 |
| Spring Boot（可选） | 2.7.18（Java 11） / 3.5.x（Java 17） | — | 只影响 Web 层，不影响 Ontop/Jena |

> Ontop **没有发布 `ontop-bom`**（Maven Central 不存在该构件），所以公司内部需要在父 POM 里自己锁版本；
> OWLAPI 有 `owlapi-bom:5.5.1`，如果内部另有 OWLAPI 模块要声明，可以 import 它。

---

## 1. 必须直接声明的依赖

| groupId | artifactId | version | scope | 是否必需 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `it.unibz.inf.ontop` | `ontop-rdf4j` | 5.5.0 | compile | ✅ 必需 | RDF4J Repository API（`OntopRepository` / `OntopVirtualRepository`） |
| `it.unibz.inf.ontop` | `ontop-system-sql-owlapi` | 5.5.0 | compile | ✅ 必需 | SQL 配置 + 本体解析 + OBDA 映射（`.obda`），并传递 OWLAPI |
| `com.mysql` | `mysql-connector-j` | 8.0.33 | runtime | ✅ TDSQL 需要 | TDSQL/MySQL JDBC 驱动 |
| `org.slf4j` | `slf4j-simple` 或 `logback-classic` | 1.7.36 / 1.2.x | runtime | ✅ 二选一 | 必须只有一个 SLF4J binding |
| `it.unibz.inf.ontop` | `ontop-cli` / `ontop-endpoint` | 5.5.0 | compile | ❌ 可选 | 只有需要命令行 / 独立 SPARQL Endpoint 时才加 |
| `org.apache.jena` | `jena-core` / `jena-arq` | 4.8.0 | compile | ❌ 公司已有 | 用于 Jena 推理/SPARQL，与 Ontop 独立 |

**Exclusions（`ontop-system-sql-owlapi`，可复制）：**

```xml
<exclusions>
    <!-- 避免与公司日志实现冲突 -->
    <exclusion><groupId>ch.qos.logback</groupId><artifactId>logback-classic</artifactId></exclusion>
    <exclusion><groupId>ch.qos.logback</groupId><artifactId>logback-core</artifactId></exclusion>
    <exclusion><groupId>log4j</groupId><artifactId>log4j</artifactId></exclusion>
    <!-- 旧版 SeRQL 解析器与 RDF4J 5.x 不兼容（会打 NoSuchFieldError 日志） -->
    <exclusion><groupId>org.eclipse.rdf4j</groupId><artifactId>rdf4j-queryparser-serql</artifactId></exclusion>
</exclusions>
```

> ⚠️ **不要**排除 `org.apache.commons:commons-rdf-rdf4j`：Ontop 启动时要用
> `org.apache.commons.rdf.rdf4j.RDF4J` 作为 RDF 工厂，排除后启动直接失败。
> 也不要排除 `ontop-mapping-r2rml` / OWLAPI 相关包（默认配置会加载 R2RML 模块与本体解析器）。

---

## 2. 方案 A：只引入 Ontop（最小 POM，可直接复制）

> 文件位置：`docs/examples/pom-ontop-only.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  最小可用 POM：只引入 Ontop 5.5.0（进程内 OBDA / 虚拟 RDF）。
  OWLAPI 不需要在这里声明：ontop-system-sql-owlapi 会传递引入 owlapi-* 5.5.1。
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company.ontology</groupId>
    <artifactId>ontop-integration</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>11</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <ontop.version>5.5.0</ontop.version>
        <mysql.version>8.0.33</mysql.version>
        <slf4j.version>1.7.36</slf4j.version>
    </properties>

    <dependencies>
        <!-- 1) RDF4J Repository API：OntopVirtualRepository / OntopRepository -->
        <dependency>
            <groupId>it.unibz.inf.ontop</groupId>
            <artifactId>ontop-rdf4j</artifactId>
            <version>${ontop.version}</version>
        </dependency>

        <!-- 2) SQL + OWLAPI：JDBC 配置、本体解析、OBDA 映射（.obda） -->
        <dependency>
            <groupId>it.unibz.inf.ontop</groupId>
            <artifactId>ontop-system-sql-owlapi</artifactId>
            <version>${ontop.version}</version>
            <exclusions>
                <!-- R2RML 传递链里的旧日志实现 -->
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-core</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>log4j</groupId>
                    <artifactId>log4j</artifactId>
                </exclusion>
                <!-- 旧版 SeRQL 解析器与 RDF4J 5.x 不兼容，会打印 NoSuchFieldError 噪声 -->
                <exclusion>
                    <groupId>org.eclipse.rdf4j</groupId>
                    <artifactId>rdf4j-queryparser-serql</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- 3) TDSQL 走 MySQL 协议 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- 4) 日志门面只有一个实现：这里用 slf4j-simple；
             如果公司统一用 logback，请删掉本依赖并引入 logback-classic -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
    </dependencies>
</project>
```

---

## 3. 方案 B：Ontop 5.5 + Jena 4.8 + Spring Boot 2.7（公司服务基线）

> 文件位置：`docs/examples/pom-ontop-jena-springboot.xml`
> 如果公司不用 Spring Boot，可以删掉 starter 依赖，只保留 Ontop + Jena 部分即可。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  公司内部"Ontop 5.5 + Jena 4.8 + Spring Boot 2.7（Java 11）"的完整 POM 基线。
  - Ontop / OWLAPI：Ontop 5.5.0 传递带入 OWLAPI 5.5.1（无需显式声明）
  - Jena 4.8.0：公司现有版本，与 Ontop 无版本耦合
  - Jackson：Jena 4.8 用 2.14.2、Ontop 5.5 用 2.15.4、Boot 2.7.18 默认 2.13.5，
             统一由 Boot 的 jackson-bom.version 属性钉到 2.15.4
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <groupId>com.company.ontology</groupId>
    <artifactId>ontology-reasoning-service</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>11</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <ontop.version>5.5.0</ontop.version>
        <jena.version>4.8.0</jena.version>
        <mysql.version>8.0.33</mysql.version>
        <h2.version>2.2.220</h2.version>
        <springdoc.version>1.7.0</springdoc.version>

        <!-- 满足 Jena 4.8 / Ontop 5.5 的 Jackson 版本，覆盖 Boot 2.7 默认的 2.13.5 -->
        <jackson-bom.version>2.15.4</jackson-bom.version>
    </properties>

    <dependencies>
        <!-- ===== Spring Boot Web / 校验 / 监控 / OpenAPI ===== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- ===== Apache Jena 4.8（公司已有） ===== -->
        <dependency>
            <groupId>org.apache.jena</groupId>
            <artifactId>jena-core</artifactId>
            <version>${jena.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.jena</groupId>
            <artifactId>jena-arq</artifactId>
            <version>${jena.version}</version>
        </dependency>

        <!-- ===== Ontop 5.5 + OWLAPI 5.5.1（OWLAPI 由此传递引入） ===== -->
        <dependency>
            <groupId>it.unibz.inf.ontop</groupId>
            <artifactId>ontop-rdf4j</artifactId>
            <version>${ontop.version}</version>
        </dependency>
        <dependency>
            <groupId>it.unibz.inf.ontop</groupId>
            <artifactId>ontop-system-sql-owlapi</artifactId>
            <version>${ontop.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-core</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>log4j</groupId>
                    <artifactId>log4j</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.eclipse.rdf4j</groupId>
                    <artifactId>rdf4j-queryparser-serql</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- ===== JDBC 驱动 ===== -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>${h2.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- ===== 测试 ===== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

> **如果允许 Java 17**：把 parent 换成 `3.5.16`、`java.version` 改成 `17`、springdoc 换成
> `springdoc-openapi-starter-webmvc-ui:2.8.17`，并把 7 个 `javax.validation/servlet` import 改成
> `jakarta.validation/servlet` 即可；**Jena 4.8 / Ontop 5.5 无需改动**（项目副本已实测 24/24 测试通过；该数字为当时测试集）。
> 详见 `docs/boot3-upgrade.md`。

---

## 4. OWLAPI 相关包清单（Ontop 传递引入，无需声明）

| groupId | artifactId | 版本 | scope | 作用 |
| --- | --- | --- | --- | --- |
| `net.sourceforge.owlapi` | `owlapi-api` | 5.5.1 | compile | OWL 2 对象模型 / API 接口 |
| `net.sourceforge.owlapi` | `owlapi-apibinding` | 5.5.1 | compile | API 与实现的绑定（聚合入口） |
| `net.sourceforge.owlapi` | `owlapi-impl` | 5.5.1 | compile | API 实现 |
| `net.sourceforge.owlapi` | `owlapi-parsers` | 5.5.1 | compile | OWL/XML、Functional、Manchester 等解析器 |
| `net.sourceforge.owlapi` | `owlapi-rio` | 5.5.1 | compile | 基于 RDF4J Rio 的 Turtle/RDF/XML 解析、序列化 |
| `net.sourceforge.owlapi` | `owlapi-oboformat` | 5.5.1 | compile | OBO 格式支持 |
| `net.sourceforge.owlapi` | `owlapi-tools` | 5.5.1 | compile | 工具类 |
| `net.sourceforge.owlapi` | `owlapi-distribution` | 5.5.1 | ❌ 可选 | OSGi 聚合分发包，普通 Maven 项目用 `owlapi-apibinding` 即可 |

注意：`owlapi-rio` 会带入 **RDF4J 5.0.2 的 rio 模块**（binary/n3/rdfjson/rdfxml/trix/hdt/jsonld），
与 Ontop 核心用的 **RDF4J 5.1.4** 在依赖树中并存——这是 Ontop 5.5 的官方组合，不需要强行统一；
但旧版 **RDF4J 2.2.2** 的 sail 模块（来自 R2RML API 传递链）也只打包、不参与查询路径，不用处理。

---

## 5. 其他关键传递依赖（了解即可，不建议手动写版本）

### Ontop 模块（全部 5.5.0，同一版本）

`ontop-model`、`ontop-obda-core`、`ontop-reformulation-core`、`ontop-system-core`、`ontop-optimization`、
`ontop-rdb`、`ontop-mapping-core`、`ontop-mapping-owlapi`、`ontop-ontology-owlapi`、`ontop-mapping-sql-core`、
`ontop-mapping-sql-all`、`ontop-mapping-native`、`ontop-mapping-r2rml`、`ontop-mapping-sql-owlapi`、
`ontop-reformulation-sql`、`ontop-system-sql-core`、`ontop-system-owlapi`、`ontop-kg-query`、`ontop-rdf4j`、
`ontop-system-sql-owlapi`。

> 公司内部统一用 `<ontop.version>` 属性锁定，**所有 `it.unibz.inf.ontop:*` 必须同版本**，不要混 5.4.x/5.5.0。

### RDF4J（核心 5.1.4 + owlapi-rio 的 5.0.2 + 旧 2.2.2）

核心模块：`rdf4j-model(-api, -vocabulary)`、`rdf4j-query`、`rdf4j-queryparser-sparql`、
`rdf4j-repository-api`、`rdf4j-repository-sparql`、`rdf4j-rio-api`、`rdf4j-rio-turtle`、
`rdf4j-sail-api` 等（版本 5.1.4）。

### 关键第三方库（由 Ontop 管理，通常无需关心）

Guice 5.0.1、Guava 32.0.1-jre、HikariCP 3.4.5、tomcat-jdbc 10.0.0-M7、Jackson 2.15.4、
SLF4J 1.7.36、ANTLR 4.13.1、jsqlparser 4.4、jsonld-java 0.13.0、caffeine 3.1.8、
commons-rdf 0.5.0、commons-codec 1.14、commons-io、proj4j 1.1.1、jgrapht 0.9.3、toml4j 0.7.2 等。

---

## 6. 版本对齐规则（公司内部落地的关键）

1. **Ontop**：所有 `it.unibz.inf.ontop:*` 使用同一版本（本文 5.5.0）；升级时整体升级并回归。
2. **OWLAPI**：锁 **5.5.1**；如果公司其他系统也用 OWLAPI，把版本收敛到 5.5.1，不要混 5.1.x。
3. **RDF4J**：以 Ontop 5.5.0 的 **5.1.4** 为准；如果内部平台强制 RDF4J 4.x/6.x，需要单独做兼容性验证。
4. **Jackson**：Jena 4.8 需要 2.14.2、Ontop 5.5 需要 2.15.4、Boot 2.7 默认 2.13.5，
   统一用 **2.15.4**（向上兼容）：
   - Spring Boot 2.7：在 properties 里设置 `<jackson-bom.version>2.15.4</jackson-bom.version>`；
   - 非 Spring 项目：`dependencyManagement` 中 import `com.fasterxml.jackson:jackson-bom:2.15.4`。
5. **SLF4J**：统一 **1.7.36**，classpath 只允许一个 binding（logback 或 slf4j-simple，二选一）。
6. **Servlet**：如果公司用 Spring Boot 3（Java 17），需要 `jakarta.*`；与 Ontop 无关。
7. **TDSQL 驱动**：MySQL Connector/J 8.0.33（或公司统一版本）；TDSQL 走 MySQL 协议。

### 公司父 POM / 内部 BOM 建议

```xml
<properties>
    <ontop.version>5.5.0</ontop.version>
    <jena.version>4.8.0</jena.version>
    <mysql.version>8.0.33</mysql.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Jackson 统一版本（也可在 Boot 2.7 中用 jackson-bom.version 属性） -->
        <dependency>
            <groupId>com.fasterxml.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>2.15.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Ontop 没有官方 BOM，需要自己管理 -->
        <dependency>
            <groupId>it.unibz.inf.ontop</groupId>
            <artifactId>ontop-rdf4j</artifactId>
            <version>${ontop.version}</version>
        </dependency>
        <dependency>
            <groupId>it.unibz.inf.ontop</groupId>
            <artifactId>ontop-system-sql-owlapi</artifactId>
            <version>${ontop.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.jena</groupId>
            <artifactId>jena-core</artifactId>
            <version>${jena.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.jena</groupId>
            <artifactId>jena-arq</artifactId>
            <version>${jena.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> 若内部已有 OWLAPI 依赖管理，可直接 `import net.sourceforge.owlapi:owlapi-bom:5.5.1`，
> 但不要单独声明与 Ontop 冲突的 OWLAPI 版本。

---

## 7. 引入后的验证命令

```bash
# 1. 确认 OWLAPI 传递引入（应显示 5.5.1 的 api/apibinding/impl/parsers/rio/...）
mvn dependency:tree -Dincludes=net.sourceforge.owlapi

# 2. 确认 Ontop 全部为 5.5.0
mvn dependency:tree -Dincludes=it.unibz.inf.ontop

# 3. 确认 RDF4J 版本（5.1.4 核心 + 5.0.2 rio + 旧 2.2.2 sail）
mvn dependency:tree -Dincludes=org.eclipse.rdf4j

# 4. 检查是否有版本冲突（omitted for conflict）
mvn dependency:tree -Dverbose | grep -i "omitted for conflict"
```

最小启动代码（验证 Ontop 能否用本地 T-Box + OBDA 映射连上 TDSQL）：

```java
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepository;
import it.unibz.inf.ontop.rdf4j.repository.impl.OntopVirtualRepository;
import java.io.File;

OntopSQLOWLAPIConfiguration configuration = OntopSQLOWLAPIConfiguration.defaultBuilder()
        .ontologyFile(new File("ontology/company.ttl"))          // 本地 T-Box
        .nativeOntopMappingFile(new File("mapping/company.obda")) // 本地 OBDA 映射
        .jdbcUrl("jdbc:mysql://<tdsql-host>:3306/ontology_db?useSSL=false&serverTimezone=Asia/Shanghai")
        .jdbcUser("<user>")
        .jdbcPassword("<password>")
        .jdbcDriver("com.mysql.cj.jdbc.Driver")
        .build();

OntopVirtualRepository repository = OntopRepository.defaultRepository(configuration);
repository.init();
// repository.getConnection() -> prepareTupleQuery(SPARQL) ...
```

---

## 8. 许可证一览（公司合规）

| 组件 | 许可证 | 备注 |
| --- | --- | --- |
| Ontop 5.5.0 | **Apache License 2.0** | 商用友好 |
| OWLAPI 5.5.1 | **Apache 2.0 / LGPL 3.0 双许可** | 可选 Apache 2.0 |
| RDF4J 5.1.4 | **Eclipse Distribution License 1.0（BSD 风格）** | 商用友好 |
| Apache Jena 4.8.0 | **Apache License 2.0** | 商用友好 |
| MySQL Connector/J 8.0.33 | **GPLv2 + Universal FOSS Exception** | 内部使用通常无问题；对外分发二进制需评估，或换 MariaDB Connector/J（LGPL） |

---

## 附：完整依赖树（来自最小 POM：ontop-rdf4j + ontop-system-sql-owlapi 5.5.0 + MySQL 驱动）

```text
com.example:ontop-deps-probe:pom:1.0.0
+- it.unibz.inf.ontop:ontop-rdf4j:jar:5.5.0:compile
|  +- it.unibz.inf.ontop:ontop-model:jar:5.5.0:compile
|  |  +- org.apache.commons:commons-rdf-simple:jar:0.5.0:compile
|  |  +- com.google.inject.extensions:guice-assistedinject:jar:5.0.1:compile
|  |  +- io.github.solf:nullanno:jar:3.0.0:compile
|  |  +- io.mikael:urlbuilder:jar:2.0.9:compile
|  |  +- org.locationtech.proj4j:proj4j:jar:1.1.1:compile
|  |  +- commons-codec:commons-codec:jar:1.14:compile
|  |  \- javax.inject:javax.inject:jar:1:compile
|  +- it.unibz.inf.ontop:ontop-obda-core:jar:5.5.0:compile
|  |  \- org.javabits.jgrapht:jgrapht-core:jar:0.9.3:compile
|  +- it.unibz.inf.ontop:ontop-reformulation-core:jar:5.5.0:compile
|  |  +- it.unibz.inf.ontop:ontop-kg-query:jar:5.5.0:compile
|  |  +- org.eclipse.rdf4j:rdf4j-queryalgebra-model:jar:5.1.4:compile
|  |  \- org.eclipse.rdf4j:rdf4j-queryparser-sparql:jar:5.1.4:compile
|  |     \- org.eclipse.rdf4j:rdf4j-rio-trig:jar:5.1.4:compile
|  +- it.unibz.inf.ontop:ontop-system-core:jar:5.5.0:compile
|  +- org.slf4j:slf4j-api:jar:1.7.36:compile
|  +- org.apache.httpcomponents:httpcore:jar:4.4.14:compile
|  +- com.fasterxml.jackson.core:jackson-core:jar:2.15.4:compile
|  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.15.4:compile
|  +- com.fasterxml.jackson.core:jackson-databind:jar:2.15.4:compile
|  +- com.google.inject:guice:jar:5.0.1:compile
|  |  \- aopalliance:aopalliance:jar:1.0:compile
|  +- com.google.guava:guava:jar:32.0.1-jre:compile
|  |  +- com.google.guava:failureaccess:jar:1.0.1:compile
|  |  +- com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava:compile
|  |  +- org.checkerframework:checker-qual:jar:3.33.0:compile
|  |  +- com.google.errorprone:error_prone_annotations:jar:2.18.0:compile
|  |  \- com.google.j2objc:j2objc-annotations:jar:2.8:compile
|  +- com.google.code.findbugs:jsr305:jar:3.0.2:compile
|  +- org.apache.commons:commons-rdf-api:jar:0.5.0:compile
|  +- com.github.jsonld-java:jsonld-java:jar:0.13.0:compile
|  |  +- org.slf4j:jcl-over-slf4j:jar:1.7.29:runtime
|  |  \- commons-io:commons-io:jar:2.6:compile
|  +- org.eclipse.rdf4j:rdf4j-model:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-model-api:jar:5.1.4:compile
|  |  |  \- org.eclipse.rdf4j:rdf4j-common-annotation:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-model-vocabulary:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-common-exception:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-common-text:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-common-io:jar:5.1.4:compile
|  |  \- org.eclipse.rdf4j:rdf4j-common-iterator:jar:5.1.4:compile
|  |     \- org.eclipse.rdf4j:rdf4j-common-order:jar:5.1.4:compile
|  +- org.eclipse.rdf4j:rdf4j-query:jar:5.1.4:compile
|  |  \- org.apache.commons:commons-text:jar:1.10.0:compile
|  |     \- org.apache.commons:commons-lang3:jar:3.12.0:compile
|  +- org.eclipse.rdf4j:rdf4j-repository-sparql:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-http-client:jar:5.1.4:compile
|  |  |  +- org.eclipse.rdf4j:rdf4j-http-protocol:jar:5.1.4:compile
|  |  |  +- org.eclipse.rdf4j:rdf4j-queryresultio-api:jar:5.1.4:compile
|  |  |  +- org.eclipse.rdf4j:rdf4j-queryresultio-binary:jar:5.1.4:runtime
|  |  |  +- org.apache.httpcomponents:httpclient:jar:4.5.14:compile
|  |  |  \- org.eclipse.rdf4j:rdf4j-sail-api:jar:5.1.4:compile
|  |  |     \- org.eclipse.rdf4j:rdf4j-collection-factory-api:jar:5.1.4:compile
|  |  \- org.eclipse.rdf4j:rdf4j-queryresultio-sparqlxml:jar:5.1.4:compile
|  +- org.eclipse.rdf4j:rdf4j-rio-nquads:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-rio-ntriples:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-rio-datatypes:jar:5.1.4:compile
|  |  \- org.eclipse.rdf4j:rdf4j-rio-languages:jar:5.1.4:compile
|  +- org.eclipse.rdf4j:rdf4j-repository-api:jar:5.1.4:compile
|  |  \- org.eclipse.rdf4j:rdf4j-common-transaction:jar:5.1.4:compile
|  +- org.eclipse.rdf4j:rdf4j-queryparser-api:jar:5.1.4:compile
|  +- org.eclipse.rdf4j:rdf4j-rio-api:jar:5.1.4:compile
|  |  +- org.eclipse.rdf4j:rdf4j-common-xml:jar:5.1.4:compile
|  |  \- no.hasmac:hasmac-json-ld:jar:0.9.0:compile
|  +- org.eclipse.rdf4j:rdf4j-queryrender:jar:5.1.4:compile
|  \- com.moandjiezana.toml:toml4j:jar:0.7.2:compile
|     \- com.google.code.gson:gson:jar:2.8.1:compile
+- it.unibz.inf.ontop:ontop-system-sql-owlapi:jar:5.5.0:compile
|  +- it.unibz.inf.ontop:ontop-optimization:jar:5.5.0:compile
|  +- it.unibz.inf.ontop:ontop-rdb:jar:5.5.0:compile
|  |  +- com.fasterxml.jackson.datatype:jackson-datatype-guava:jar:2.15.4:compile
|  |  \- com.github.jsqlparser:jsqlparser:jar:4.4:compile
|  +- it.unibz.inf.ontop:ontop-mapping-core:jar:5.5.0:compile
|  |  +- org.antlr:antlr4-runtime:jar:4.13.1:compile
|  |  \- org.eclipse.rdf4j:rdf4j-rio-turtle:jar:5.1.4:compile
|  +- it.unibz.inf.ontop:ontop-mapping-owlapi:jar:5.5.0:compile
|  |  +- it.unibz.inf.ontop:ontop-ontology-owlapi:jar:5.5.0:compile
|  |  +- net.sourceforge.owlapi:owlapi-apibinding:jar:5.5.1:compile
|  |  |  +- net.sourceforge.owlapi:owlapi-impl:jar:5.5.1:compile
|  |  |  +- net.sourceforge.owlapi:owlapi-parsers:jar:5.5.1:compile
|  |  |  +- net.sourceforge.owlapi:owlapi-oboformat:jar:5.5.1:compile
|  |  |  +- net.sourceforge.owlapi:owlapi-tools:jar:5.5.1:compile
|  |  |  \- net.sourceforge.owlapi:owlapi-rio:jar:5.5.1:compile
|  |  |     +- org.eclipse.rdf4j:rdf4j-rio-binary:jar:5.0.2:compile
|  |  |     +- org.eclipse.rdf4j:rdf4j-rio-n3:jar:5.0.2:compile
|  |  |     +- org.eclipse.rdf4j:rdf4j-rio-rdfjson:jar:5.0.2:compile
|  |  |     +- org.eclipse.rdf4j:rdf4j-rio-rdfxml:jar:5.0.2:compile
|  |  |     +- org.eclipse.rdf4j:rdf4j-rio-trix:jar:5.0.2:compile
|  |  |     +- org.eclipse.rdf4j:rdf4j-rio-hdt:jar:5.0.2:compile
|  |  |     \- org.eclipse.rdf4j:rdf4j-rio-jsonld:jar:5.0.2:compile
|  |  |        +- jakarta.json:jakarta.json-api:jar:2.0.1:compile
|  |  |        +- org.glassfish:jakarta.json:jar:2.0.1:compile
|  |  |        \- org.apache.httpcomponents:httpclient-cache:jar:4.5.14:compile
|  |  +- edu.stanford.protege:org.protege.xmlcatalog:jar:1.0.5:compile
|  |  |  \- xml-resolver:xml-resolver:jar:1.2:compile
|  |  \- org.slf4j:log4j-over-slf4j:jar:1.7.36:runtime
|  +- it.unibz.inf.ontop:ontop-mapping-sql-core:jar:5.5.0:compile
|  +- it.unibz.inf.ontop:ontop-mapping-sql-all:jar:5.5.0:compile
|  |  +- it.unibz.inf.ontop:ontop-mapping-native:jar:5.5.0:compile
|  |  \- it.unibz.inf.ontop:ontop-mapping-r2rml:jar:5.5.0:compile
|  |     +- org.apache.commons:commons-rdf-rdf4j:jar:0.5.0:compile
|  |     |  +- org.eclipse.rdf4j:rdf4j-sail-memory:jar:2.2.2:compile
|  |     |  |  +- org.eclipse.rdf4j:rdf4j-sail-base:jar:2.2.2:compile
|  |     |  |  +- org.eclipse.rdf4j:rdf4j-sail-inferencer:jar:2.2.2:compile
|  |     |  |  |  \- org.eclipse.rdf4j:rdf4j-sail-model:jar:2.2.2:compile
|  |     |  |  +- org.eclipse.rdf4j:rdf4j-queryalgebra-evaluation:jar:2.2.2:compile
|  |     |  |  |  \- org.mapdb:mapdb:jar:1.0.8:compile
|  |     |  |  \- org.eclipse.rdf4j:rdf4j-util:jar:2.2.2:compile
|  |     |  \- org.eclipse.rdf4j:rdf4j-repository-sail:jar:2.2.2:compile
|  |     +- eu.optique-project:r2rml-api-core:jar:0.9.1:compile
|  |     \- eu.optique-project:r2rml-api-rdf4j-binding:jar:0.9.1:compile
|  +- it.unibz.inf.ontop:ontop-mapping-sql-owlapi:jar:5.5.0:compile
|  +- it.unibz.inf.ontop:ontop-reformulation-sql:jar:5.5.0:compile
|  +- it.unibz.inf.ontop:ontop-system-sql-core:jar:5.5.0:compile
|  |  +- org.apache.tomcat:tomcat-jdbc:jar:10.0.0-M7:compile
|  |  |  \- org.apache.tomcat:tomcat-juli:jar:10.0.0-M7:compile
|  |  \- com.zaxxer:HikariCP:jar:3.4.5:compile
|  +- it.unibz.inf.ontop:ontop-system-owlapi:jar:5.5.0:compile
|  \- net.sourceforge.owlapi:owlapi-api:jar:5.5.1:compile
|     +- org.tukaani:xz:jar:1.9:compile
|     +- com.github.vsonnier:hppcrt:jar:0.7.5:compile
|     \- com.github.ben-manes.caffeine:caffeine:jar:3.1.8:compile
\- com.mysql:mysql-connector-j:jar:8.0.33:runtime
```

---

## 附：版本升级注意

- 本文所有坐标以 **Ontop 5.5.0** 为准；升级 Ontop 时，`it.unibz.inf.ontop:*` 全部一起升，
  并复核它管理的 RDF4J / OWLAPI 版本是否变化。
- Jena 4.8 与 Ontop 5.5 无耦合：升级 Jena（4.x 内）不需要动 Ontop；升级 Ontop 也不需要动 Jena，
  只需重新对齐 Jackson/SLF4J 这类共享库。
- 如果公司未来把 Java 基线升到 17，可平滑换 Spring Boot 3.5.x（见 `docs/boot3-upgrade.md`），
  Ontop 5.5.0 / Jena 4.8.0 保持不变。
