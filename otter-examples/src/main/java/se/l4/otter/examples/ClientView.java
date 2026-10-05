package se.l4.otter.examples;

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
