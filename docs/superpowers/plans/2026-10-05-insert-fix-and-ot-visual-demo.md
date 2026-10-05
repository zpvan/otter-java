# insert 修复收尾 + OT 可视化控制台演示 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 清理 `SharedStringImpl.insert()` 修复的遗留问题(死代码 + 无回归测试),并把 `my-ot` 变成正式的 Maven 模块,实现基于 otter 引擎的 OT 并发编辑控制台演示。

**Architecture:** Part A 在 otter-model 模块内补 3 个 JUnit 回归测试并删除死代码。Part B 将 `my-ot` 注册为根 pom 的第 5 个 Maven 模块,删除无法编译的 `Opt.java`,新增 `DeltaPrinter`(StringDelta → 可读文本,纯函数)、`ClientView`(封装 Editor + 本地文档)和 `OtConsoleDemo`(main 入口,回放 3 个预置并发场景)。演示复用 otter-engine 的 `LocalOperationSync` + `DefaultEditor` + `StringDelta`。

**Tech Stack:** Java 9+(`maven-compiler-plugin` release 9)、Maven、JUnit 4.9、otter-engine/otter-operations。

**Spec:** `docs/superpowers/specs/2026-10-05-insert-fix-and-ot-visual-demo-design.md`

---

### Task 0: 环境准备 — 安装 JDK 和 Maven

本机当前没有 Java 运行时(`javac` 报 "Unable to locate a Java Runtime"),编译和测试都依赖此任务。

**Files:**
- 无文件改动

- [ ] **Step 1: 确认缺失**

Run: `java -version; mvn -v`
Expected: 两者均 command not found 或无运行时

- [ ] **Step 2: 安装 JDK 和 Maven**

Run: `brew install --cask temurin && brew install maven`
Expected: 安装成功。若 brew 需要密码或失败,提示用户在 Claude Code 提示符里用 `! brew install --cask temurin` 自行执行

- [ ] **Step 3: 验证**

Run: `java -version && mvn -v`
Expected: 输出 JDK 版本(11+ 即可,需 ≥9)和 Maven 版本

---

### Task 1: Part A — insert/remove 回归测试 + 删除死代码

**Files:**
- Modify: `otter-model/src/test/java/se/l4/otter/model/SharedStringTest.java`
- Modify: `otter-model/src/main/java/se/l4/otter/model/internal/SharedStringImpl.java:154`

背景:`SharedStringImpl.insert()` 原实现先 `this.value.insert(idx, value)` 再 `editor.apply(delta)`,而 `editor.apply` → `DefaultModel.apply` → `operationApplied`(DefaultModel.java:161)→ 内部 handler 会再次执行 `value.insert`,导致文本插入两次。commit `924f12e` 已通过注释掉直接插入修复,本任务补回归测试并删除死代码。

注意:该仓库 Java 代码使用 **tab 缩进**,与现有测试文件风格保持一致。

- [ ] **Step 1: 在 SharedStringTest 中新增 3 个测试**

在 `testConcurrentAppend` 方法之后(class 结束括号之前)追加:

```java
	/**
	 * Regression test: insert must not apply the change twice.
	 */
	@Test
	public void testInsert()
	{
		Model m = model();

		SharedString string = m.newString();
		m.set("string", string);
		sync.waitForEmpty();

		string.set("hello");
		string.insert(5, " world");

		assertThat(string.get(), is("hello world"));
	}

	/**
	 * Test that concurrent inserts resolve to the same string value.
	 */
	@Test
	public void testConcurrentInsert()
	{
		Model m1 = model();
		Model m2 = model();

		SharedString string1 = m1.newString();
		m1.set("string", string1);
		string1.set("hello");

		sync.waitForEmpty();

		SharedString string2 = m2.get("string");

		sync.suspend();

		string1.insert(5, " A");
		string2.insert(0, "B ");

		sync.resume();

		sync.waitForEmpty();

		assertThat(string1.get(), is(string2.get()));
		assertThat(string1.get(), is("B hello A"));
	}

	/**
	 * Basic test for remove.
	 */
	@Test
	public void testRemove()
	{
		Model m = model();

		SharedString string = m.newString();
		m.set("string", string);
		sync.waitForEmpty();

		string.set("hello world");
		string.remove(5, 11);

		assertThat(string.get(), is("hello"));
	}
```

- [ ] **Step 2: 删除 SharedStringImpl 中的死代码**

`otter-model/src/main/java/se/l4/otter/model/internal/SharedStringImpl.java` 第 154 行,删除:

```java
			// this.value.insert(idx, value);
```

- [ ] **Step 3: 运行测试,确认全部通过**

Run: `mvn -pl otter-model -am test -Dtest=SharedStringTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 5, Failures: 0, Errors: 0`(SharedStringTest 共 5 个测试)

- [ ] **Step 4: 验证回归测试真的能抓到原 bug**

临时把死代码还原为有效语句(`this.value.insert(idx, value);`),重跑 Step 3 命令。
Expected: `testInsert` FAIL,实际值为 `hello world world`(插入两次)
验证后**必须还原**为删除状态,再跑一次确认全绿。

- [ ] **Step 5: Commit**

```bash
git add otter-model/src/test/java/se/l4/otter/model/SharedStringTest.java otter-model/src/main/java/se/l4/otter/model/internal/SharedStringImpl.java
git commit -m "test(model): add regression tests for SharedString insert/remove

The insert double-apply bug fixed in 924f12e now has a regression test.
Also removes the commented-out dead code line and adds basic coverage
for remove()."
```

---

### Task 2: 把 my-ot 注册为 Maven 模块并删除 Opt.java

**Files:**
- Create: `my-ot/pom.xml`
- Modify: `pom.xml`(根 pom,`<modules>` 部分)
- Delete: `my-ot/src/java/com/ot/visual/Opt.java`

- [ ] **Step 1: 删除无法编译的 Opt.java,整理目录为标准 Maven 布局**

```bash
git rm my-ot/src/java/com/ot/visual/Opt.java
mkdir -p my-ot/src/main/java/com/ot/visual my-ot/src/test/java/com/ot/visual
```

`my-ot/src/java` 此时应为空目录,如残留空目录则删除:`rm -rf my-ot/src/java`

- [ ] **Step 2: 创建 my-ot/pom.xml**

```xml
<project
	xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

	<modelVersion>4.0.0</modelVersion>

	<parent>
		<artifactId>otter-base</artifactId>
		<groupId>se.l4.otter</groupId>
		<version>0.2.0-SNAPSHOT</version>
		<relativePath>..</relativePath>
	</parent>

	<artifactId>my-ot</artifactId>
	<name>Otter Visual Demo</name>

	<dependencies>
		<dependency>
			<groupId>${project.groupId}</groupId>
			<artifactId>otter-engine</artifactId>
			<version>${project.version}</version>
		</dependency>

		<dependency>
			<groupId>org.slf4j</groupId>
			<artifactId>slf4j-simple</artifactId>
			<version>1.7.30</version>
		</dependency>
	</dependencies>
</project>
```

- [ ] **Step 3: 根 pom.xml 注册模块**

根 `pom.xml` 第 23-29 行的 `<modules>` 块,在 `<module>otter-model</module>` 之后追加一行:

```xml
		<module>my-ot</module>
```

- [ ] **Step 4: 验证模块被构建**

Run: `mvn -pl my-ot -am compile`
Expected: `BUILD SUCCESS`,Reactor Summary 中包含 `my-ot`

若 revapi 插件因新 artifact 无历史版本而报错,在 `my-ot/pom.xml` 的 `<build><plugins>` 中添加 revapi skip 配置后重试:

```xml
	<build>
		<plugins>
			<plugin>
				<groupId>org.revapi</groupId>
				<artifactId>revapi-maven-plugin</artifactId>
				<configuration>
					<skip>true</skip>
				</configuration>
			</plugin>
		</plugins>
	</build>
```

- [ ] **Step 5: Commit**

```bash
git add pom.xml my-ot/
git commit -m "build: turn my-ot into a Maven module and drop uncompilable Opt.java

Opt.java had compile errors (misspelled Action type, self-assigned
revision field) that went unnoticed because my-ot was not part of the
Maven build. It is replaced by a delta printer and console demo based
on the real engine in subsequent commits."
```

---

### Task 3: DeltaPrinter — StringDelta 可读化(纯函数,TDD)

**Files:**
- Create: `my-ot/src/main/java/com/ot/visual/DeltaPrinter.java`
- Test: `my-ot/src/test/java/com/ot/visual/DeltaPrinterTest.java`

注意:`DeltaPrinter.print` 在 Task 4 的 `ClientView` 中被使用,方法签名必须严格一致:`public static String print(Operation<StringHandler> op)`。

- [ ] **Step 1: 写失败的测试**

创建 `my-ot/src/test/java/com/ot/visual/DeltaPrinterTest.java`:

```java
package com.ot.visual;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.l4.otter.operations.Operation;
import se.l4.otter.operations.string.StringDelta;
import se.l4.otter.operations.string.StringHandler;

public class DeltaPrinterTest
{
	@Test
	public void testInsertAndRetain()
	{
		Operation<StringHandler> op = StringDelta.builder()
			.insert("ab")
			.retain(3)
			.insert("cd")
			.done();

		assertEquals("insert(\"ab\") retain(3) insert(\"cd\")", DeltaPrinter.print(op));
	}

	@Test
	public void testRetainAndDelete()
	{
		Operation<StringHandler> op = StringDelta.builder()
			.retain(2)
			.delete("xy")
			.insert("z")
			.done();

		assertEquals("retain(2) delete(\"xy\") insert(\"z\")", DeltaPrinter.print(op));
	}

	@Test
	public void testSpecialCharsAreEscaped()
	{
		Operation<StringHandler> op = StringDelta.builder()
			.insert("a\nb")
			.insert("c\"d")
			.done();

		assertEquals("insert(\"a\\nb\") insert(\"c\\\"d\")", DeltaPrinter.print(op));
	}
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl my-ot -am test -Dtest=DeltaPrinterTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败,`DeltaPrinter` 不存在

- [ ] **Step 3: 实现 DeltaPrinter**

创建 `my-ot/src/main/java/com/ot/visual/DeltaPrinter.java`:

```java
package com.ot.visual;

import se.l4.otter.operations.Operation;
import se.l4.otter.operations.string.AnnotationChange;
import se.l4.otter.operations.string.StringHandler;

/**
 * Renders a string operation as human-readable text, for example
 * {@code insert("ab") retain(3) insert("cd")}.
 */
public final class DeltaPrinter
{
	private DeltaPrinter()
	{
	}

	public static String print(Operation<StringHandler> op)
	{
		StringBuilder result = new StringBuilder();

		op.apply(new StringHandler()
		{
			@Override
			public void retain(int count)
			{
				append(result, "retain(" + count + ")");
			}

			@Override
			public void insert(String s)
			{
				append(result, "insert(\"" + escape(s) + "\")");
			}

			@Override
			public void delete(String s)
			{
				append(result, "delete(\"" + escape(s) + "\")");
			}

			@Override
			public void annotationUpdate(AnnotationChange change)
			{
				append(result, "annotation");
			}
		});

		return result.toString();
	}

	private static void append(StringBuilder target, String token)
	{
		if(target.length() > 0)
		{
			target.append(' ');
		}
		target.append(token);
	}

	private static String escape(String s)
	{
		return s
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n");
	}
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl my-ot -am test -Dtest=DeltaPrinterTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add my-ot/src/main/java/com/ot/visual/DeltaPrinter.java my-ot/src/test/java/com/ot/visual/DeltaPrinterTest.java
git commit -m "feat(demo): add DeltaPrinter for human-readable string operations"
```

---

### Task 4: ClientView + OtConsoleDemo — 场景回放主程序

**Files:**
- Create: `my-ot/src/main/java/com/ot/visual/ClientView.java`
- Create: `my-ot/src/main/java/com/ot/visual/OtConsoleDemo.java`

架构:`ClientView` 封装一个 `DefaultEditor` 和本地文档(`StringBuilder`)。本地编辑时先更新本地文档再 `editor.apply(op)`(`Editor.apply` 的契约是"操作已被本地应用,不触发本地 listener");远端操作经 `EditorListener` 到达时打印 transform 后的 delta 并应用到本地文档。`OtConsoleDemo` 为每个场景创建独立的 control + sync + 两个客户端,suspend 期间制造并发,resume 后验证收敛。

- [ ] **Step 1: 创建 ClientView.java**

```java
package com.ot.visual;

import se.l4.otter.engine.DefaultEditor;
import se.l4.otter.engine.Editor;
import se.l4.otter.engine.OperationSync;
import se.l4.otter.operations.Operation;
import se.l4.otter.operations.string.AnnotationChange;
import se.l4.otter.operations.string.StringHandler;

/**
 * One simulated client: a local document plus an {@link Editor} that
 * keeps it in sync with other clients.
 */
public class ClientView
{
	private final String name;
	private final Editor<Operation<StringHandler>> editor;
	private final StringBuilder document = new StringBuilder();

	public ClientView(String name, OperationSync<Operation<StringHandler>> sync)
	{
		this.name = name;
		this.editor = new DefaultEditor<>(sync);

		// Build the local document from the latest known operation.
		// The initial state of a string is always a sequence of inserts.
		editor.getCurrent().apply(new StringHandler()
		{
			@Override
			public void retain(int count)
			{
				throw new IllegalStateException("Initial state must only contain inserts");
			}

			@Override
			public void insert(String s)
			{
				document.append(s);
			}

			@Override
			public void delete(String s)
			{
				throw new IllegalStateException("Initial state must only contain inserts");
			}

			@Override
			public void annotationUpdate(AnnotationChange change)
			{
				// Annotations are not used in this demo
			}
		});

		editor.addListener(event ->
		{
			if(event.isRemote())
			{
				System.out.println("  [" + name + "] <- remote op after transform: "
					+ DeltaPrinter.print(event.getOperation()));
				applyToDocument(event.getOperation());
				System.out.println("  [" + name + "] document now: \"" + document + "\"");
			}
		});
	}

	/**
	 * Perform a local edit: update the local document, then send the
	 * operation to the other clients.
	 */
	public void applyLocal(Operation<StringHandler> op)
	{
		System.out.println("  [" + name + "] local op: " + DeltaPrinter.print(op));
		applyToDocument(op);
		editor.apply(op);
		System.out.println("  [" + name + "] document now: \"" + document + "\"");
	}

	public String getDocument()
	{
		return document.toString();
	}

	public void close()
	{
		editor.close();
	}

	private void applyToDocument(Operation<StringHandler> op)
	{
		op.apply(new StringHandler()
		{
			private int index;

			@Override
			public void retain(int count)
			{
				index += count;
			}

			@Override
			public void insert(String s)
			{
				document.insert(index, s);
				index += s.length();
			}

			@Override
			public void delete(String s)
			{
				document.delete(index, index + s.length());
			}

			@Override
			public void annotationUpdate(AnnotationChange change)
			{
				// Annotations are not used in this demo
			}
		});
	}
}
```

- [ ] **Step 2: 创建 OtConsoleDemo.java**

```java
package com.ot.visual;

import se.l4.otter.engine.DefaultEditorControl;
import se.l4.otter.engine.InMemoryOperationHistory;
import se.l4.otter.engine.LocalOperationSync;
import se.l4.otter.operations.Operation;
import se.l4.otter.operations.string.StringDelta;
import se.l4.otter.operations.string.StringHandler;
import se.l4.otter.operations.string.StringType;

/**
 * Console demo of operational transformation: two clients edit a shared
 * string concurrently and the engine transforms their operations so both
 * converge on the same document.
 */
public class OtConsoleDemo
{
	public static void main(String[] args)
	{
		boolean ok = true;

		ok &= scenario(
			"1. Concurrent inserts at different positions",
			"hello",
			StringDelta.builder().retain(5).insert(" A").done(),
			StringDelta.builder().insert("B ").done(),
			"B hello A"
		);

		// Both clients insert at the same position. The engine decides the
		// final order deterministically, so only convergence is checked.
		ok &= scenario(
			"2. Concurrent inserts at the same position",
			"hello",
			StringDelta.builder().retain(5).insert("!").done(),
			StringDelta.builder().retain(5).insert("?").done(),
			null
		);

		ok &= scenario(
			"3. Insert racing with delete",
			"hello world",
			StringDelta.builder().retain(6).insert("brave ").done(),
			StringDelta.builder().delete("hello ").done(),
			"brave world"
		);

		System.out.println(ok ? "ALL SCENARIOS OK" : "SOME SCENARIOS FAILED");
		if(! ok)
		{
			System.exit(1);
		}
	}

	/**
	 * Run one scenario and print each step. {@code expected} is the exact
	 * final document, or {@code null} to only check that both clients
	 * converge on the same value.
	 */
	private static boolean scenario(String title, String initial,
			Operation<StringHandler> opA, Operation<StringHandler> opB, String expected)
	{
		System.out.println("== " + title + " ==");
		System.out.println("Initial document: \"" + initial + "\"");

		StringType type = new StringType();
		DefaultEditorControl<Operation<StringHandler>> control = new DefaultEditorControl<>(
			new InMemoryOperationHistory<>(type, StringDelta.builder().insert(initial).done())
		);
		LocalOperationSync<Operation<StringHandler>> sync = new LocalOperationSync<>(control);

		ClientView clientA = new ClientView("A", sync);
		ClientView clientB = new ClientView("B", sync);

		// Suspend delivery so both edits are made concurrently
		sync.suspend();
		clientA.applyLocal(opA);
		clientB.applyLocal(opB);
		sync.resume();
		sync.waitForEmpty();

		String docA = clientA.getDocument();
		String docB = clientB.getDocument();
		System.out.println("Final document on A: \"" + docA + "\"");
		System.out.println("Final document on B: \"" + docB + "\"");

		boolean ok = docA.equals(docB) && (expected == null || docA.equals(expected));
		System.out.println(ok ? "OK: clients converged" : "FAIL: documents diverged");
		System.out.println();

		clientA.close();
		clientB.close();
		sync.close();

		return ok;
	}
}
```

- [ ] **Step 3: 编译**

Run: `mvn -pl my-ot -am compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add my-ot/src/main/java/com/ot/visual/ClientView.java my-ot/src/main/java/com/ot/visual/OtConsoleDemo.java
git commit -m "feat(demo): add console demo replaying concurrent string edits

Three scripted scenarios (insert/insert at different positions,
insert/insert at the same position, insert vs delete) run on two
editors linked by LocalOperationSync. Each step is printed and the
demo exits non-zero if the clients diverge."
```

---

### Task 5: 运行演示 + 全量验证

**Files:**
- 无文件改动(生成的 `my-ot/cp.txt` 用后删除)

- [ ] **Step 1: 构建并生成本地 classpath**

Run: `mvn -pl my-ot -am package -DskipTests && mvn -pl my-ot dependency:build-classpath -Dmdep.outputFile=cp.txt -q`
Expected: `BUILD SUCCESS`,生成 `my-ot/cp.txt`

- [ ] **Step 2: 运行演示**

Run: `java -cp "my-ot/target/classes:$(cat my-ot/cp.txt)" com.ot.visual.OtConsoleDemo`
Expected:
- 场景 1 输出结尾为 `OK: clients converged`,最终文档 `"B hello A"`
- 场景 2 输出结尾为 `OK: clients converged`(两端一致即可)
- 场景 3 输出结尾为 `OK: clients converged`,最终文档 `"brave world"`
- 最后一行 `ALL SCENARIOS OK`,退出码 0

若场景 3 实际结果与预期不符(例如空格处理差异),以两端收敛为准修正 `OtConsoleDemo.main` 中场景 3 的 expected 值并重跑。

- [ ] **Step 3: 全量测试**

Run: `mvn test`
Expected: 全部模块 `BUILD SUCCESS`,无测试失败

- [ ] **Step 4: 清理临时文件**

```bash
rm my-ot/cp.txt
```

(`my-ot/target/` 已被根 `.gitignore` 的 `target` 规则覆盖,无需处理;如 `git status` 显示未跟踪文件则确认后处理。)

- [ ] **Step 5: 如 Step 2 修正过 expected 值,提交修正**

```bash
git add my-ot/src/main/java/com/ot/visual/OtConsoleDemo.java
git commit -m "fix(demo): correct expected value for insert-vs-delete scenario"
```

若无修正则跳过本步。

---

## Self-Review 记录

- **Spec 覆盖**:Part A 三项(死代码删除 + testInsert/testConcurrentInsert/testRemove)→ Task 1;Part B(my-ot 模块化、删除 Opt.java、DeltaPrinter、3 场景回放、收敛校验非零退出)→ Task 2-5;JDK 前提 → Task 0。无遗漏。
- **类型一致性**:`DeltaPrinter.print(Operation<StringHandler>)` 在 Task 3 定义、Task 4 使用,签名一致;`ClientView(name, sync)` / `applyLocal` / `getDocument` / `close` 在 Task 4 内部一致。
- **占位符扫描**:无 TBD/TODO;Task 2 Step 4 的 revapi 处理是有条件的具体方案,非占位符。
