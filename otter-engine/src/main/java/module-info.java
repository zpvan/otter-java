module se.l4.otter.engine
{
	requires transitive se.l4.otter.common;
	requires transitive se.l4.otter.operations;
	requires org.slf4j;
	requires se.l4.ylem.ids;

	exports se.l4.otter.engine;
	exports se.l4.otter.engine.events;
}
