# insert 修复收尾 + OT 可视化控制台演示 — 设计文档

日期:2026-10-05
状态:已获用户批准

## 背景

对 otter-java 当前实现的分析结论:

1. **commit `924f12e` (fix insert)**:`SharedStringImpl.insert()` 原实现先直接执行
   `this.value.insert(idx, value)`,再调用 `editor.apply(delta)`。后者经
   `SharedObjectEditorImpl.apply` → `DefaultModel.apply`(`DefaultModel.java:161` 调用
   `objectEditor.operationApplied(op, false)`)→ `SharedStringImpl` 内部 handler 会**再次**
   执行 `value.insert(index, s)`,导致同一文本被插入两次。注释掉直接插入的修复方向正确,
   但遗留:死代码注释未删除、无回归测试。
2. **commit `1cc85fa` (ot visual)**:`my-ot/src/java/com/ot/visual/Opt.java` 无法编译
   (`Ation` 类型名拼写错误、构造器参数 `revisiton` 拼写错误导致 `this.revision = revision`
   变成字段自赋值、revision 永远为 0、`clientId` 从未赋值),且 `my-ot` 不在 Maven
   `<modules>` 中、无 pom.xml,根本不会被构建。功能上只有一个残缺数据类,无 UI、无
   main、无 transform 逻辑,远未完成。

## Part A:insert 修复收尾

1. 删除 `SharedStringImpl.java` 中注释掉的死代码行 `// this.value.insert(idx, value);`。
2. 在 `otter-model/src/test/java/se/l4/otter/model/SharedStringTest.java` 补回归测试:
   - `testInsert`:单客户端对字符串调用 `insert(idx, text)` 后 `get()` 内容正确 ——
     直接验证"不会插入两次"这个原 bug。
   - `testConcurrentInsert`:仿照现有 `testConcurrentAppend`,两个客户端在
     `sync.suspend()` 期间并发 `insert()`,`sync.resume()` 后两端 `get()` 一致。
   - `testRemove`:补上 `remove(fromIndex, toIndex)` 的基础测试(该方法同样无覆盖)。

## Part B:OT 可视化控制台演示

### 决策(已与用户确认)

- 形式:**控制台文本演示**,不引入 GUI 依赖。
- 实现基础:**复用现有 otter 引擎**(`LocalOperationSync` + `DefaultEditor` +
  `StringDelta`),而非自写独立 OT 实现。
- 演示方式:**预置场景自动播放**,可重复运行。

### 代码组织

- 为 `my-ot` 目录补 `pom.xml`,注册为根 pom 的第 5 个 Maven 模块,依赖
  otter-engine(及传递依赖)。源码目录规范为 Maven 标准布局
  `my-ot/src/main/java/com/ot/visual/...`。这样该目录被构建覆盖,编译错误不再隐身。

### Opt.java 的处理

**删除 `Opt.java`**。它的字段模型(action/position/content/revision/clientId)照抄自
operational-transformation.github.io 的操作模型,与 otter 的 `StringDelta`
(retain/insert/delete 序列)不匹配,保留它无法驱动真实引擎。改为实现
`DeltaPrinter`:把 `StringDelta` 渲染为可读文本,如 `retain(3) insert("ab") retain(5)`。

### 演示程序结构

```
OtConsoleDemo (main 入口)
 ├─ 搭建 LocalOperationSync + 两个 DefaultEditor(clientA / clientB,基于 StringType)
 ├─ DeltaPrinter:StringDelta → 可读文本
 └─ 场景回放器,每个场景的步骤:
     1. 打印初始文档
     2. sync.suspend() → 两个客户端各自产生操作,打印各自视角的 delta
     3. sync.resume() → 通过 EditorListener 打印每个客户端收到的(已 transform 的)
        远端操作及文档状态演变
     4. 打印最终文档,断言两端一致(convergence check)
```

### 预置场景(3 个经典并发场景)

1. 不同位置并发 insert;
2. 相同位置并发 insert;
3. insert 与 delete 交叉冲突。

### 错误处理

- 场景回放结束若两端文档不一致,打印差异并以非零退出码结束(作为可执行的收敛性验证)。
- demo 不在 CI 中强制运行(它是 example),但必须能通过 `mvn compile`。

### 测试

- Part A 的 3 个回归测试通过 `mvn -pl otter-model test` 验证。
- Part B 的 `DeltaPrinter` 补单元测试(纯函数,易测)。
- 场景回放器本身以 main 运行结果人工验证。

### 环境前提

本机当前无 Java 运行时。实施第一步需安装 JDK(建议 Temurin JDK 11+,经 Homebrew),
否则无法编译与运行测试。
