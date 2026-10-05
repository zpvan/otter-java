package se.l4.otter.examples;

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
		// The builder normalizes operations: at the same position an
		// insert is sorted before a delete.
		Operation<StringHandler> op = StringDelta.builder()
			.retain(2)
			.delete("xy")
			.insert("z")
			.done();

		assertEquals("retain(2) insert(\"z\") delete(\"xy\")", DeltaPrinter.print(op));
	}

	@Test
	public void testSpecialCharsAreEscaped()
	{
		Operation<StringHandler> op = StringDelta.builder()
			.insert("a\nb")
			.retain(1)
			.insert("c\"d")
			.done();

		assertEquals("insert(\"a\\nb\") retain(1) insert(\"c\\\"d\")", DeltaPrinter.print(op));
	}
}
