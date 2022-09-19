package fiab.mes.shopfloor.utils;

import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;

/**
 * Is used to create an anonymous class that implements both TransportPositionLookupInterface and TransportPositionParser
 */
public interface TransportPositionLookupAndParser extends TransportPositionLookupInterface, TransportPositionParser {
}
