package fiab.mes.productioncell.foldingstation;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.productioncell.FoldingProductionCell;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Optional;

public class DefaultFoldingCellTransportPositionLookup implements TransportPositionLookupInterface, TransportPositionParser {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTransportPositionLookup.class);

    private final HashMap<TransportRoutingInterface.Position, AkkaActorBackedCoreModelAbstractActor> lookupTable = new HashMap<>();

    @Override
    public String getLookupPrefix() {
        return FoldingProductionCell.LOOKUP_PREFIX;
    }

    @Override
    public TransportRoutingInterface.Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor) {
        TransportRoutingInterface.Position pos = parseLastIPPos(actor.getModelActor().getUri());
        if (pos != TransportRoutingInterface.UNKNOWN_POSITION)
            lookupTable.put(pos, actor);
        return pos;
    }

    @Override
    public Optional<AkkaActorBackedCoreModelAbstractActor> getActorForPosition(TransportRoutingInterface.Position pos) {
        return Optional.ofNullable(lookupTable.get(pos));
    }

    @Override
    public TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
        String host = "UNKNOWN";
        try {
            URI uri = new URI(uriAsString);
            // this code just reads the last digit from the IPv4 address and returns this as position
            host = uri.getHost();
            if (host == null) {
                logger.warn(String.format("URI for actor %s has no host part for resolving Position", uriAsString));
                return TransportRoutingInterface.UNKNOWN_POSITION;
            }
            if (host.equals("127.0.0.1") || host.equals("localhost")) {
                return parsePosViaPortNr(uriAsString);
            }
            return parsePosViaIPAddress(uriAsString, host);
            //InetAddress inetAddr = InetAddress.getByName(host);
            //int lastPos = (inetAddr.getAddress()[3]+256)%256;
            //return new TransportRoutingInterface.Position(""+lastPos);
        } catch (URISyntaxException e) {
            logger.warn(String.format("Unable to load URI for actor %s", uriAsString));
            return TransportRoutingInterface.UNKNOWN_POSITION;
        } catch (UnknownHostException e) {
            logger.warn(String.format("Unable to obtain IP address for host %s for actor %s", host, uriAsString));
            return TransportRoutingInterface.UNKNOWN_POSITION;
        }
    }

    public TransportRoutingInterface.Position parsePosViaIPAddress(String uriAsString, String host) throws UnknownHostException {
        InetAddress inetAddr = InetAddress.getByName(host);
        int lastPos = (inetAddr.getAddress()[3] + 256) % 256;
        //Hardcoded ip address to position translation. Should work for demo purposes
        if (lastPos == 24 || lastPos == 40 || lastPos == 41) return parsePosViaPortNr(uriAsString);   //FoldingStations or IO
        //if (lastPos == 41) return new TransportRoutingInterface.Position("20");  //LocalTT
        return TransportRoutingInterface.UNKNOWN_POSITION;

    }

    // USE THIS FOR TESTS ONLY
    @Override
    public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
        try {
            URI uri = new URI(uriAsString);
            // this uses ports used for testing mock hardware on localhost, do not use in production
            int port = uri.getPort();
            if (port == 4847) return new TransportRoutingInterface.Position("34");
            if (port == 4848) return new TransportRoutingInterface.Position("20");
            if (port == 4849) return new TransportRoutingInterface.Position("31");
            if (port == 4850) return new TransportRoutingInterface.Position("21");
            if (port == 4851) return new TransportRoutingInterface.Position("37");
            return TransportRoutingInterface.UNKNOWN_POSITION;
        } catch (URISyntaxException e) {
            logger.warn(String.format("Unable to load URI for actor %s", uriAsString));
            return TransportRoutingInterface.UNKNOWN_POSITION;
        }
    }
}
