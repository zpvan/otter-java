module se.l4.otter.operations
{
	requires org.slf4j;
	requires se.l4.exobytes;
	requires org.eclipse.collections.api;
	requires org.eclipse.collections.impl;

	exports se.l4.otter.operations;
	exports se.l4.otter.operations.combined;
	exports se.l4.otter.operations.list;
	exports se.l4.otter.operations.map;
	exports se.l4.otter.operations.string;

	// DefaultComposer is used by DefaultEditor in the engine module
	exports se.l4.otter.operations.internal to se.l4.otter.engine;
}
