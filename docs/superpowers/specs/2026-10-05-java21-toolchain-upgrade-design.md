# Java 21 + 构建工具链 + CI 升级 — 设计文档

日期:2026-10-05
状态:已获用户批准(基线定为 Java 21)

## 目标

将 otter-java 的 Java 基线从 9(EOL 多年的非 LTS)升到 21(LTS),同步升级构建插件与直接依赖,重写已失效的 GitHub Actions CI。

## 已核实的事实

- 根 pom `<release>9</release>`;CI 用 actions/checkout@v1、setup-java@v1、JDK 9 —— v1/v2 actions 已随 Node 16 下线,CI 基本必坏。
- spotbugs 4.2.0 在 JDK 27 上直接崩溃("Unsupported class file major version 71");revapi 0.13.2 报 0.2.0-SNAPSHOT 相对已发布 0.1.1 的 breaking 变更。
- `randomizedtesting-runner` 在代码中零引用,可删。
- `eclipse-collections`、`caffeine`、`checker-qual`、`error_prone` 均为 `se.l4.exobytes`/`se.l4.ylem` 的传递依赖,不直接声明,不动。
- `se.l4.exobytes`/`se.l4.ylem.*` 是上游作者生态,保持锁定。
- slf4j-simple 1.7.30 在 otter-common / otter-operations / otter-engine / otter-model / my-ot 五个 pom 中重复声明。

## 变更内容

### 根 pom.xml

- maven-compiler-plugin 3.8.1 → 3.14.1,`<release>9</release>` → `<release>21</release>`
- maven-surefire-plugin 3.0.0-M5 → 3.5.4
- spotbugs-maven-plugin 4.2.0 → 4.10.4.1(2026-09 发布,支持新版 JDK 字节码),删除其内部 spotbugs 4.2.1 的手动覆盖(插件自带匹配版本)
- revapi 保持 0.13.2 不动(升级与 0.2.0 发布基线决策绑定,后续单独立项)
- junit 4.9 → 4.13.2(dependencyManagement)
- slf4j 版本收敛为根 pom 属性 `<slf4j.version>2.0.17</slf4j.version>`,slf4j-api 引用该属性
- 删除 randomizedtesting-runner 依赖
- release profile 中 javadoc `<source>9</source>` → `21`

### 各模块 pom.xml

- otter-common / otter-operations / otter-engine / otter-model / my-ot 的 slf4j-simple 版本 `1.7.30` → `${slf4j.version}`

### .github/workflows/ci.yml 重写

- on: [push, pull_request]
- actions/checkout@v4 + actions/setup-java@v4(distribution: temurin,cache: maven)
- matrix: java [21, 25]
- 构建命令:`mvn -B test`
- 删除 `-Prelease package` 步骤(release profile 含 GPG 签名,CI 无密钥)
- spotbugs/revapi 不进 CI gate(spotbugs 存量代码未 triage;revapi 待发布决策)

## 验证标准

1. `mvn test` 全绿(当前 122 个测试)
2. `mvn install -DskipTests -Drevapi.skip=true` 不再需要 `-Dspotbugs.skip=true`(若 spotbugs 4.10.4.1 仍不支持本机 JDK 27 字节码,回退为保留 skip 并在报告中注明;CI 的 JDK 21/25 不受影响)
3. `OtConsoleDemo` 仍能运行且 3 场景全 OK

## 明确不做(后续 backlog)

JUnit 5 迁移 · revapi 升级 + 0.2.0 发布基线 · spotbugs 存量 triage 与 CI gate · README/CONTRIBUTING/CHANGELOG · `my-ot` 改名 `otter-examples` · JPMS module-info.java
