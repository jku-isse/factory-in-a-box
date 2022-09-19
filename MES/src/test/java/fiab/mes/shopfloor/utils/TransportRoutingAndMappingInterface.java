package fiab.mes.shopfloor.utils;

import fiab.mes.transport.actor.transportmodule.InternalCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

/**
 * This interface is used to create an anonymous class that implements both interfaces
 */
public interface TransportRoutingAndMappingInterface extends TransportRoutingInterface, InternalCapabilityToPositionMapping {
}
