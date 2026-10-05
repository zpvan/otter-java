# Otter

[English](README.md) | 简体中文

[![CI](https://github.com/zpvan/otter-java/actions/workflows/ci.yml/badge.svg)](https://github.com/zpvan/otter-java/actions/workflows/ci.yml)

Otter 是一个基于[操作转换(Operational Transformation)](https://en.wikipedia.org/wiki/Operational_transformation)实现协同实时编辑的库。
本仓库是其 Java 实现(fork 自
[LevelFourAB/otter-java](https://github.com/LevelFourAB/otter-java))。

## OT 算法原理

### 文档与操作

文档是一个字符序列。一次编辑表达为一个 *delta*:由 `retain(n)`、`insert(s)`、`delete(s)` 按序组合、从左到右应用,例如 `retain(5) insert(" A")`。一个 delta 的*输入长度*(retain + delete 的字符数)必须等于它所应用的文档长度。

### Transform(转换)

当两个客户端基于同一版本并发编辑时,它们的操作会互相 *transform*:每个操作被重写,使其能够应用在另一个操作之上。给定基于同一文档的两个操作 `a` 和 `b`,transform 产出 `a'` 和 `b'`,满足:

```
apply(apply(doc, a), b') == apply(apply(doc, b), a')
```

这就是收敛性(convergence):无论操作以什么顺序到达,所有客户端最终得到相同的文档。

示例(即控制台演示的场景 1),初始文档为 `hello`:

- 客户端 A 应用 `retain(5) insert(" A")` → `hello A`
- 客户端 B 应用 `insert("B ") retain(5)` → `B hello`

两个编辑并发发生。当操作交叉时,引擎对它们做 transform:

- B 收到 A 的操作,被转换为 `retain(7) insert(" A")` → `B hello A`
- A 收到 B 的操作,被转换为 `insert("B ") retain(7)` → `B hello A`

两端收敛到 `B hello A`,无需任何人工冲突解决。

### Compose(合并)

两个连续的操作可以合并为一个等价操作(`compose`)。引擎用它来做两件事:编辑器持锁期间批量合并本地编辑,以及压缩操作历史。

### 与代码结构的对应关系

- `otter-operations` 实现数据模型,以及 string / list / map 三种类型的 transform 和 compose 函数;`CombinedType` 按对象 id 将多种类型组合在一起。
- `EditorControl`(如 `DefaultEditorControl`)是权威端(服务端):它存储收到的操作,并将其与该客户端基线版本之后发生的一切操作做 transform。
- `Editor`(如 `DefaultEditor`)是客户端:本地操作立即应用,收到的远端操作先与本地未确认的操作做 transform 再应用。

运行控制台演示(见下文)可以逐步观察这一过程。

## 环境要求

- Java 21+
- Maven 3.6+

## 构建

本 fork 未发布到 Maven Central,请在本地构建安装:

```bash
mvn install
```

API 兼容性检查(revapi)只在 `release` profile 下运行:

```bash
mvn -Prelease verify -Dgpg.skip=true
```

## 模块

| 模块 | 说明 |
| --- | --- |
| `otter-common` | 共享工具(锁、事件辅助) |
| `otter-operations` | string / list / map 及组合文档的 OT 算法 |
| `otter-engine` | 编辑引擎:编辑器、同步、历史 |
| `otter-model` | 高层 API,提供共享对象(map、string、list) |
| `otter-examples` | 可运行示例,含并发编辑控制台演示 |

## 使用 Otter

Otter 由三部分组成:操作库、编辑引擎和高层模型。通常你想用的是高层模型,除非你在实现特殊需求。

### Operations(操作)

Otter 的最底层是操作转换算法,支持 map、list 和 string 上的转换。还有一个组合类型(combined type),可以基于唯一 id 把多个类型组合起来。高层模型正是由这些转换组合而成的。

### Engine(引擎)

引擎包含编辑控制,支持在任何受支持的操作转换之上创建编辑器。

```java
OperationSync<Operation<StringHandler>> sync = new YourOperationSync(new StringType(), ...);
Editor<Operation<StringHandler>> editor = new DefaultEditor<>(uniqueSessionId, sync);

// 获取初始内容并注册监听器
try(CloseableLock lock = editor.lock()) {
  editor.getCurrent().apply( ... );

  editor.addEditorListener(new EditorEventHandler());
}

// 执行一个操作
try(CloseableLock lock = editor.lock()) {
  // 通过锁来安全地构造 delta
  editor.apply(StringDelta.builder()
    .retain(currentStringLength)
    .insert("abc")
    .done()
  );
}
```

编辑器需要一个同步助手来与服务器收发操作。这里有意不提供默认实现,因为不同应用的要求不同。

编辑器执行的所有操作最终都会交给一个 `EditorControl` 实例处理:

```java
EditorControl control = new DefaultEditorControl(historyStorage);

// 新编辑器连接时,可以获取最新版本:
control.getLatest();

// 收到客户端操作后,需要存储它,
// 并把结果广播给所有客户端:
TaggedOperation op = control.store(taggedOperation);
```

### Model(模型)

这是让共享编辑更易用的高层 API。模型提供不同类型的共享对象,在模型的所有编辑器之间保持同步。

一个使用模型的小例子:

```java
Editor editor = new DefaultEditor(uniqueSessionId, sync);
Model model = Model.builder(editor)
  .build();

// 创建一个新字符串并存入根 map
SharedString title = model.newString();
title.set("Cookies are tasty");
model.set("title", title);

// 在 map 中存一个基本类型值
model.set("priority", 10);
```

## 运行控制台演示

`otter-examples` 模块包含一个演示程序,在两个编辑器上回放三个经典并发编辑场景,并打印每一步转换过程:

```bash
mvn install -DskipTests
mvn -pl otter-examples dependency:build-classpath -Dmdep.outputFile=cp.txt -q
java -cp "otter-examples/target/classes:$(cat otter-examples/cp.txt)" se.l4.otter.examples.OtConsoleDemo
```

如果两个客户端没有收敛到同一文档,演示程序会以非零码退出。

## 许可证

[Apache License 2.0](LICENSE.txt)
