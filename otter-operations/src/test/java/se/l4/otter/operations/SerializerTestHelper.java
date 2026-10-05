package se.l4.otter.operations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import se.l4.exobytes.Serializer;
import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;

public class SerializerTestHelper
{
	private SerializerTestHelper()
	{
	}

	public static <T> void testSymmetry(Serializer<T> serializer, T value)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try(StreamingOutput json = StreamingFormat.JSON.createOutput(out))
		{
			serializer.write(value, json);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}

		T readValue;
		try(StreamingInput in = StreamingFormat.JSON.createInput(new ByteArrayInputStream(out.toByteArray())))
		{
			readValue = serializer.read(in);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}

		assertThat(readValue, is(value));
	}

	public static <T> void testStatic(String json, Serializer<T> serializer, T value)
	{
		T readValue;
		try(StreamingInput in = StreamingFormat.JSON.createInput(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))))
		{
			readValue = serializer.read(in);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}

		assertThat(readValue, is(value));
	}
}
