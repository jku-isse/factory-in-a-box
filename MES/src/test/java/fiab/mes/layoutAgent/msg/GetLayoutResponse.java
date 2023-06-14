package fiab.mes.layoutAgent.msg;

import fiab.mes.shopfloor.utils.TransportPositionLookupAndParser;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

public class GetLayoutResponse {

    private final TransportPositionLookupAndParser transportPositionLookup;
    private final TransportRoutingAndMappingInterface transportRouting;

    public GetLayoutResponse(TransportPositionLookupAndParser transportPositionLookup, TransportRoutingAndMappingInterface transportRouting) {
        this.transportPositionLookup = transportPositionLookup;
        this.transportRouting = transportRouting;
    }

    public TransportPositionLookupAndParser getTransportPositionLookup() {
        return transportPositionLookup;
    }

    public TransportRoutingAndMappingInterface getTransportRouting() {
        return transportRouting;
    }
}
