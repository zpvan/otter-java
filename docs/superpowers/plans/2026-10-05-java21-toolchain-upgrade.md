# Java 21 + 构建工具链 + CI 升级 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Java 基线 9 → 21,升级 Maven 插件与直接依赖,重写失效的 GitHub Actions CI。

**Architecture:** 仅构建配置变更,零业务代码改动。根 pom 集中升级插件与属性;五个模块 pom 的 slf4j-simple 版本统一引用根 pom 新属性;ci.yml 整体重写。

**Spec:** `docs/superpowers/specs/2026-10-05-java21-toolchain-upgrade-design.md`

---

### Task 1: 根 pom.xml 升级

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: properties 增加 slf4j 版本**

`<properties>` 块(`<project.build.sourceEncoding>` 之后)加一行:

```xml
		<slf4j.version>2.0.17</slf4j.version>
```

- [ ] **Step 2: 升级 slf4j-api,删除 randomizedtesting**

根 pom `<dependencies>` 中:
- `<version>1.7.30</version>`(slf4j-api)→ `<version>${slf4j.version}</version>`
- 整块删除:

```xml
 		<dependency>
			<groupId>com.carrotsearch.randomizedtesting</groupId>
			<artifactId>randomizedtesting-runner</artifactId>
			<version>2.3.3</version>
			<scope>test</scope>
 		</dependency>
```

- [ ] **Step 3: junit 4.9 → 4.13.2**

`<dependencyManagement>` 中 junit 的 `<version>4.9</version>` → `<version>4.13.2</version>`

- [ ] **Step 4: 插件升级**

- maven-compiler-plugin:`<version>3.8.1</version>` → `<version>3.14.1</version>`;`<release>9</release>` → `<release>21</release>`
- maven-surefire-plugin:`<version>3.0.0-M5</version>` → `<version>3.5.4</version>`
- spotbugs-maven-plugin:`<version>4.2.0</version>` → `<version>4.10.4.1</version>`,并删除其内部的手动 spotbugs 覆盖块:

```xml
				<dependencies>
					<dependency>
						<groupId>com.github.spotbugs</groupId>
						<artifactId>spotbugs</artifactId>
						<version>4.2.1</version>
					</dependency>
				</dependencies>
```

- revapi:不动(保持 0.13.2)

- [ ] **Step 5: release profile javadoc**

`<source>9</source>` → `<source>21</source>`

- [ ] **Step 6: 编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

### Task 2: 五个模块 pom 的 slf4j-simple 版本统一

**Files:**
- Modify: `otter-common/pom.xml`、`otter-operations/pom.xml`、`otter-engine/pom.xml`、`otter-model/pom.xml`、`my-ot/pom.xml`

- [ ] **Step 1: 每个文件中 slf4j-simple 的 `<version>1.7.30</version>` → `<version>${slf4j.version}</version>`**

- [ ] **Step 2: 编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit Task 1+2**

```bash
git add pom.xml otter-common/pom.xml otter-operations/pom.xml otter-engine/pom.xml otter-model/pom.xml my-ot/pom.xml
git commit -m "build: upgrade to Java 21 baseline and modern toolchain

- maven-compiler-plugin 3.14.1 with release 21 (was 3.8.1 / Java 9)
- maven-surefire-plugin 3.5.4 (was 3.0.0-M5 milestone)
- spotbugs-maven-plugin 4.10.4.1 (4.2.0 crashes on modern JDKs)
- junit 4.13.2 (was 4.9), slf4j 2.0.17 unified via property
- drop unused randomizedtesting-runner dependency"
```

### Task 3: 重写 CI

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: 整体替换为**

```yaml
name: CI

on: [push, pull_request]

jobs:
  build:
    name: CI (JDK ${{ matrix.java }})
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: [21, 25]

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK ${{ matrix.java }}
      uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: ${{ matrix.java }}
        cache: maven

    - name: Build and test
      run: mvn -B test --file pom.xml
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "build(ci): rewrite workflow for Java 21/25 with current actions

The old workflow used actions/checkout@v1 and setup-java@v1 with JDK 9,
which no longer run on GitHub-hosted runners. Also drops the release
profile package step (requires GPG keys not available in CI)."
```

### Task 4: 全量验证

- [ ] **Step 1: 全量测试**

Run: `mvn test`
Expected: 122 个测试全绿,BUILD SUCCESS

- [ ] **Step 2: install 不再需要 spotbugs skip**

Run: `mvn install -DskipTests -Drevapi.skip=true`
Expected: BUILD SUCCESS。
若 spotbugs 4.10.4.1 仍不支持本机 JDK 27 字节码(报 Unsupported class file major version 71):保留 `-Dspotbugs.skip=true` 用法,在最终报告中注明,不算失败(CI 用 JDK 21/25 不受影响)。

- [ ] **Step 3: 演示程序回归**

```bash
mvn -pl my-ot dependency:build-classpath -Dmdep.outputFile=cp.txt -q
/opt/homebrew/opt/openjdk/bin/java -cp "my-ot/target/classes:$(cat my-ot/cp.txt)" com.ot.visual.OtConsoleDemo
rm my-ot/cp.txt
```

Expected: 3 个场景全 `OK: clients converged`,最后 `ALL SCENARIOS OK`,退出码 0

## Self-Review 记录

- Spec 覆盖:根 pom 全部变更 → Task 1;模块 pom → Task 2;CI → Task 3;三条验证标准 → Task 4 三步一一对应。无遗漏。
- 占位符:无。
- 一致性:slf4j.version 属性在 Task 1 定义、Task 2 使用;验证命令与本机环境(memory 中记录的 Java 路径)一致。
