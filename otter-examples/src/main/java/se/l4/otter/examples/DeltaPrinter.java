package se.l4.otter.examples;

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
