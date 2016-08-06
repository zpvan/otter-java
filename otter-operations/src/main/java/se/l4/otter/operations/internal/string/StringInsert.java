package se.l4.otter.operations.internal.string;

import se.l4.otter.operations.Operation;
import se.l4.otter.operations.string.StringHandler;

/**
 * Operation on a {@link String} where some characters are inserted into the
 * string at the current position.
 * 
 * @author Andreas Holstenson
 *
 */
public class StringInsert
	implements Operation<StringHandler>
{
	private final String value;

	public StringInsert(String value)
	{
		this.value = value;
	}
	
	public String getValue()
	{
		return value;
	}
	
	@Override
	public void apply(StringHandler target)
	{
		target.insert(value);
	}
	
	@Override
	public Operation<StringHandler> invert()
	{
		return new StringDelete(value);
	}
	
	@Override
	public String toString()
	{
		return "+" + value;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		StringInsert other = (StringInsert) obj;
		if(value == null)
		{
			if(other.value != null)
				return false;
		}
		else if(!value.equals(other.value))
			return false;
		return true;
	}
}
