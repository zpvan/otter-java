package se.l4.otter.operations.combined;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.l4.otter.operations.Operation;
import se.l4.otter.operations.map.MapDelta;

public class CombinedComposeTest
{
	private CombinedType type;

	@BeforeEach
	public void setup()
	{
		type = new CombinedTypeBuilder().build();

	}

	@Test
	public void testSimpleCompose()
	{
		Operation<CombinedHandler> op1 = CombinedDelta.builder()
			.update("1", "map", MapDelta.builder()
				.set("one", null, "abc")
				.done()
			)
			.done();

		Operation<CombinedHandler> op2 = CombinedDelta.builder()
			.update("1", "map", MapDelta.builder()
				.set("one", null, "def")
				.done()
			)
			.done();

		Operation<CombinedHandler> r = type.compose(op1, op2);
		assertThat(r, is(CombinedDelta.builder()
			.update("1", "map", MapDelta.builder()
				.set("one", null, "def")
				.done()
			)
			.done()
		));
	}

	@Test
	public void testComposeSeveral()
	{
		Operation<CombinedHandler> op1 = CombinedDelta.builder()
			.update("1", "map", MapDelta.builder()
				.set("one", null, "abc")
				.done()
			)
			.update("2", "map", MapDelta.builder()
				.set("one", null, "abc")
				.done()
			)
			.done();

		Operation<CombinedHandler> op2 = CombinedDelta.builder()
			.update("1", "map", MapDelta.builder()
				.set("one", null, "def")
				.done()
			)
			.done();

		Operation<CombinedHandler> r = type.compose(op1, op2);
		assertThat(r, is(CombinedDelta.builder()
			.update("1", "map", MapDelta.builder()
				.set("one", null, "def")
				.done()
			)
			.update("2", "map", MapDelta.builder()
				.set("one", null, "abc")
				.done()
			)
			.done()
		));
	}
}
