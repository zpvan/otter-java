package se.l4.otter.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.l4.otter.engine.LocalOperationSync;
import se.l4.otter.operations.Operation;
import se.l4.otter.operations.combined.CombinedHandler;

public class SharedStringTest
{
	private LocalOperationSync<Operation<CombinedHandler>> sync;

	@BeforeEach
	public void before()
	{
		sync = ModelTestHelper.createSync();
	}

	@AfterEach
	public void close()
		throws IOException
	{
		sync.close();
	}

	public Model model()
	{
		return ModelTestHelper.createModel(sync);
	}

	@Test
	public void testInit()
	{
		Model m = model();

		SharedString string = m.newString();
		assertThat(string, notNullValue());
	}


	/**
	 * Test that several concurrent appends resolve to the same string value.
	 */
	@Test
	public void testConcurrentAppend()
	{
		Model m1 = model();
		Model m2 = model();

		SharedString string1 = m1.newString();
		m1.set("string", string1);

		sync.waitForEmpty();

		SharedString string2 = m2.get("string");

		sync.suspend();

		string1.append("a");
		string2.append("b");

		sync.resume();

		sync.waitForEmpty();

		assertThat(string1.get(), is(string2.get()));
	}

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
}
