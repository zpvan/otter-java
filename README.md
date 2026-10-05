# Otter

[![CI](https://github.com/zpvan/otter-java/actions/workflows/ci.yml/badge.svg)](https://github.com/zpvan/otter-java/actions/workflows/ci.yml)

Otter is a library to support collaborative realtime editing using
[Operational Transformation](https://en.wikipedia.org/wiki/Operational_transformation).
This repository contains the Java-implementation (forked from
[LevelFourAB/otter-java](https://github.com/LevelFourAB/otter-java)).

## How Operational Transformation works

### Documents and operations

A document is a sequence of characters. An edit is expressed as a *delta*: an
ordered sequence of `retain(n)`, `insert(s)` and `delete(s)` steps applied
left to right, e.g. `retain(5) insert(" A")`. A delta's *input length*
(retained + deleted characters) must equal the length of the document it is
applied to.

### Transform

When two clients edit the same version concurrently, their operations are
*transformed* against each other: each operation is rewritten so that it can
be applied on top of the other one. Given two operations `a` and `b` based on
the same document, transform produces `a'` and `b'` such that

```
apply(apply(doc, a), b') == apply(apply(doc, b), a')
```

This is the convergence property: no matter in which order the operations
arrive, all clients end up with the same document.

Example (scenario 1 of the console demo), initial document `hello`:

- Client A applies `retain(5) insert(" A")` → `hello A`
- Client B applies `insert("B ") retain(5)` → `B hello`

Both edits happen concurrently. When the operations cross, the engine
transforms them:

- B receives A's operation transformed to `retain(7) insert(" A")` → `B hello A`
- A receives B's operation transformed to `insert("B ") retain(7)` → `B hello A`

Both converge on `B hello A` with no manual conflict resolution.

### Compose

Two consecutive operations can be merged into a single equivalent one
(`compose`). The engine uses this to batch local edits made while an editor
is locked, and to compact operation history.

### How this maps to the code

- `otter-operations` implements the data model plus the transform and compose
  functions for strings, lists and maps; `CombinedType` combines several of
  them keyed by object id.
- `EditorControl` (e.g. `DefaultEditorControl`) is the authoritative side: it
  stores incoming operations and transforms them against everything that
  happened since the client's base version.
- `Editor` (e.g. `DefaultEditor`) is the client side: it applies local
  operations immediately and transforms incoming remote operations against
  its pending local edits.

Run the console demo (see below) to watch this happen step by step.

## Requirements

- Java 21+
- Maven 3.6+

## Building

This fork is not published to Maven Central; build and install it locally:

```bash
mvn install
```

API compatibility checks (revapi) only run with the `release` profile:

```bash
mvn -Prelease verify -Dgpg.skip=true
```

## Modules

| Module | Description |
| --- | --- |
| `otter-common` | Shared utilities (locks, event helpers) |
| `otter-operations` | OT algorithms for strings, lists, maps and combined documents |
| `otter-engine` | Editing engine: editors, synchronization, history |
| `otter-model` | High-level API with shared objects (map, string, list) |
| `otter-examples` | Runnable examples, including a console demo of concurrent edits |

## Using Otter

Otter consists of three parts, the operations library, the editing engine and
a high level model. The high level model is what you usually want to use
unless you are implementing something special.

### Operations

The lowest level of Otter is the operational transformation algorithms. Otter
supports transformations on maps, lists and strings. There is also a combined
type that can be used to combine several other types based on unique
identifiers. All of these transformations are used together to create the
higher level model.

### Engine

The engine contains editing control. It provides support for creating
editors on top of any supported operational transformation.


```java
OperationSync<Operation<StringHandler>> sync = new YourOperationSync(new StringType(), ...);
Editor<Operation<StringHandler>> editor = new DefaultEditor<>(uniqueSessionId, sync);

// Get the initial content and register a listener
try(CloseableLock lock = editor.lock()) {
  editor.getCurrent().apply( ... );

  editor.addEditorListener(new EditorEventHandler());
}

// Perform an operation
try(CloseableLock lock = editor.lock()) {
  // Use a lock to safely be able to create a delta
  editor.apply(StringDelta.builder()
    .retain(currentStringLength)
    .insert("abc")
    .done()
  );
}
```

Editors require a synchronization helper for sending and receiving operations
from a server. There is intentionally no default implementation of such a sync
as different applications will have different requirements here.

In the end all operations performed by an editor will end up being handled by
an instance of `EditorControl`.

```java
EditorControl control = new DefaultEditorControl(historyStorage);

// When a new editor connects you can get the latest version:
control.getLatest();

// When an operation is received from a client it needs to be stored and
// the result needs to be sent back to all clients
TaggedOperation op = control.store(taggedOperation);
```

### Model

This is the high level API that makes it easier to work with shared editing.
The model provides shared objects of different types that are synchronized
between all editors of the model.

Here is a tiny example of working with the model:

```java
Editor editor = new DefaultEditor(uniqueSessionId, sync);
Model model = Model.builder(editor)
  .build();

// Create a new string and store it in the root map
SharedString title = model.newString();
title.set("Cookies are tasty");
model.set("title", title);

// Set a primitive value in the map
model.set("priority", 10);
```

## Running the console demo

The `otter-examples` module contains a demo that replays three classic
concurrent-editing scenarios on two editors and prints each transformation
step:

```bash
mvn install -DskipTests
mvn -pl otter-examples dependency:build-classpath -Dmdep.outputFile=cp.txt -q
java -cp "otter-examples/target/classes:$(cat otter-examples/cp.txt)" se.l4.otter.examples.OtterConsoleDemo
```

The demo exits non-zero if the two clients do not converge on the same
document.

## License

[Apache License 2.0](LICENSE.txt)
