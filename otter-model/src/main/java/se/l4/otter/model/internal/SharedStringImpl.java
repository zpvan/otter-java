package se.l4.otter.model.internal;

import java.util.LinkedList;

import com.sksamuel.diffpatch.DiffMatchPatch;
import com.sksamuel.diffpatch.DiffMatchPatch.Diff;

import se.l4.otter.model.SharedString;
import se.l4.otter.model.spi.HasApply;
import se.l4.otter.model.spi.SharedObjectEditor;
import se.l4.otter.operations.Operation;
import se.l4.otter.operations.OperationException;
import se.l4.otter.operations.string.StringDelta;
import se.l4.otter.operations.string.StringOperationHandler;

public class SharedStringImpl
	implements SharedString, HasApply<Operation<StringOperationHandler>>
{
	private static final DiffMatchPatch DIFF = new DiffMatchPatch();
	
	private final SharedObjectEditor<Operation<StringOperationHandler>> editor;
	private StringBuilder value;

	public SharedStringImpl(SharedObjectEditor<Operation<StringOperationHandler>> editor)
	{
		this.editor = editor;
		
		value = new StringBuilder();
		editor.getCurrent().apply(new StringOperationHandler()
		{
			@Override
			public void retain(int count)
			{
				throw new OperationException("Latest value invalid, must only contain inserts.");
			}
			
			@Override
			public void insert(String s)
			{
				value.append(s);
			}
			
			@Override
			public void delete(String s)
			{
				throw new OperationException("Latest value invalid, must only contain inserts.");
			}
		});
	}
	
	@Override
	public void apply(Operation<StringOperationHandler> op)
	{
		op.apply(new StringOperationHandler()
		{
			int index = 0;
			
			@Override
			public void retain(int count)
			{
				index += count;
			}
			
			@Override
			public void insert(String s)
			{
				value.insert(index, s);
				index += s.length();
			}
			
			@Override
			public void delete(String s)
			{
				value.delete(index, index + s.length());
			}
		});
	}
	
	@Override
	public String getObjectId()
	{
		return editor.getId();
	}
	
	@Override
	public String getObjectType()
	{
		return editor.getType();
	}
	
	@Override
	public String get()
	{
		return value.toString();
	}
	
	@Override
	public void set(String newValue)
	{
		LinkedList<Diff> diffs = DIFF.diff_main(value.toString(), newValue);
		if(diffs.size() > 2)
		{
			DIFF.diff_cleanupSemantic(diffs);
			DIFF.diff_cleanupEfficiency(diffs);
		}
		
		StringDelta<Operation<StringOperationHandler>> builder = StringDelta.builder();
		for(Diff d : diffs)
		{
			switch(d.operation)
			{
				case EQUAL:
					builder.retain(d.text.length());
					break;
				case DELETE:
					builder.delete(d.text);
					break;
				case INSERT:
					builder.insert(d.text);
					break;
			}
		}
		
		this.value.setLength(0);
		this.value.append(newValue);
		editor.send(builder.done());
	}
	
	@Override
	public void append(String value)
	{
		int length = this.value.length();
		this.value.append(value);
		
		editor.send(StringDelta.builder()
			.retain(length)
			.insert(value)
			.done()
		);
	}
	
	@Override
	public void insert(int idx, String value)
	{
		int length = this.value.length();
		this.value.insert(idx, value);
		
		editor.send(StringDelta.builder()
			.retain(idx)
			.insert(value)
			.retain(length - idx)
			.done()
		);
	}
	
	@Override
	public void remove(int fromIndex, int toIndex)
	{
		int length = this.value.length();
		String deleted = this.value.substring(fromIndex, toIndex);
		this.value.delete(fromIndex, toIndex);
		
		editor.send(StringDelta.builder()
			.retain(fromIndex)
			.delete(deleted)
			.retain(length - toIndex)
			.done()
		);
	}
}