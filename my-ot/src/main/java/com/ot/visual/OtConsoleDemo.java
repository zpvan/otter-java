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
