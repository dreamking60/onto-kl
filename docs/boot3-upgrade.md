# 兼容性说明：Spring Boot 版本与 Ontop / Jena 的关系

## 结论

**Spring Boot 的版本不决定 Ontop / Jena 的版本。** 二者是普通 Java 类库，与 Spring 没有任何编译期/运行时依赖耦合。

真正决定 Spring Boot 版本的是 **Java 基线**：

| 组件 | 当前版本 | Java 11 | Java 17 / 21 |
| --- | --- | --- | --- |
| Spring Boot | 2.7.18 | ✅（2.x 的最后一条线） | ❌ Boot 3.x 起要求 Java 17 |
| Apache Jena | 4.8.0 | ✅（字节码 major 55） | ✅ 可直接运行 |
| Ontop | 5.5.0 | ✅（字节码 major 55） | ✅ 可直接运行 |
| MySQL Connector/J | 8.0.33 | ✅ | ✅ |
| springdoc | 1.7.0 | ✅ | 需换 2.8.x（Jakarta 版） |

所以：

- 继续用 **Java 11** → Spring Boot 只能停在 **2.7.x**，但这**不会**限制 Ontop / Jena 的版本；
- 如果愿意升到 **Java 17** → 可以直接换 **Spring Boot 3.5.x**，而 **Jena 4.8.0 / Ontop 5.5.0 一行代码都不用改**。

## 依据

1. **字节码基线**：`jena-core-4.8.0.jar` 与 `ontop-rdf4j-5.5.0.jar` 中的 class 均为 `major version 55`（Java 11），在 Java 11/17/21 上都能运行。
2. **无 Spring 依赖**：扫描 `ontop-rdf4j`、`jena-core`、`jena-arq` 的 jar，`org/springframework` 条目数量为 0；检查 Maven POM 也没有 Spring 依赖。
3. **集成方式**：本项目只在 `infrastructure/` 里用普通 Java 调用 Ontop（`OntopSQLOWLAPIConfiguration` / `OntopVirtualRepository`）和 Jena（`ModelFactory` / `InfModel`），Spring 只负责依赖注入和 Web 层。
4. **真正的耦合点**（都只在应用层，不在 Ontop/Jena 内部）：
   - Servlet API：Spring Boot 2.x = `javax.*`，Boot 3.x = `jakarta.*` —— 只影响我们的 Controller / 异常处理器；
   - 依赖仲裁：Jackson、SLF4J/logback 版本由 Spring Boot 的 BOM 管理 —— 项目里已通过固定 Jackson 2.15.4、排除冲突的 logback/log4j/SeRQL 处理；
   - 打包方式：Spring Boot fat jar 的类加载器需要能加载 Ontop 的 Guice / ServiceLoader —— 已在实际运行中验证。

## 已验证的 Boot 3 迁移（2026-11，实验分支）

在项目副本上做了最小迁移并用 **JDK 21** 跑完整测试：

| 修改 | 内容 |
| --- | --- |
| `pom.xml` parent | `2.7.18` → `3.5.16` |
| `java.version` | `11` → `17` |
| springdoc | `springdoc-openapi-ui:1.7.0` → `springdoc-openapi-starter-webmvc-ui:2.8.17` |
| Jackson BOM 覆盖 | 删除（Boot 3.5 自带更新的 Jackson） |
| 9 个 Java 文件 | `javax.validation.*` → `jakarta.validation.*`；`javax.servlet.*` → `jakarta.servlet.*`（新增 modeling 模块的 `CreateProjectRequest`、`ModelingController` 也各需改 1 处） |
| Jena / Ontop | **版本不变**（4.8.0 / 5.5.0） |
| 业务代码 / 本体 / 映射 / 规则 / 前端 / 测试 | **不变** |

结果：`mvn test` → **Tests run: 24, Failures: 0, Errors: 0**（迁移验证时的测试集；新增文档建模模块后当前测试共 51 个），
Web UI、Ontop 虚拟图、Jena 推理、TDSQL/H2 链路全部正常。

迁移只需改 pom 与 9 个 import（含建模模块），不涉及推理与数据访问逻辑。

## 建议

- **如果 Java 11 是硬性要求**：保留当前 Boot 2.7.18。功能上完全可用，且 Ontop/Jena 仍可单独升级（受 Java 11 兼容范围限制）。注意 Spring Boot 2.7 的 OSS 支持已于 2023-11 结束，生产环境建议配合内网隔离/商业支持或规划升级。
- **如果允许 Java 17**：建议平滑升级到 Boot 3.5.x（实验已验证），后续还能继续跟 Boot 4.x。Jena/Ontop 不需要因为 Boot 升级而升级；反之，将来升级 Ontop/Jena 也不需要动 Spring Boot。

> 需要我直接切换到 Java 17 + Boot 3.5 版本时，按本文档的 5 处修改执行即可；也可以只把 `pom.xml` 和 import 改动做成一个补丁/分支。
