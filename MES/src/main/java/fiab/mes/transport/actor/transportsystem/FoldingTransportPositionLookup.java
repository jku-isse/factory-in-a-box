package fiab.mes.transport.actor.transportsystem;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Optional;

public class FoldingTransportPositionLookup implements TransportPositionLookupInterface {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTransportPositionLookup.class);

    private HashMap<TransportRoutingInterface.Position, AkkaActorBackedCoreModelAbstractActor> lookupTable = new HashMap<>();

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

    public static TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
        String host = "UNKNOWN";
        try {
            URI uri = new URI(uriAsString);
            // this code just reads the last digit from the IPv4 address and returns this as position
            host = uri.getHost();
            if (host == null) {
                logger.warn(String.format("URI for actor %s has no host part for resolving Position", uriAsString));
                return TransportRoutingInterface.UNKNOWN_POSITION;
            }
            if (host.equals("127.0.0.1") || host.equals("localhost") ){
                return parsePosViaPortNr(uriAsString);
            }
            InetAddress inetAddr = InetAddress.getByName(host);
            int lastPos = (inetAddr.getAddress()[3]+256)%256;
            return new TransportRoutingInterface.Position(""+lastPos);
        } catch (URISyntaxException e) {
            logger.warn(String.format("Unable to load URI for actor %s", uriAsString));
            return TransportRoutingInterface.UNKNOWN_POSITION;
        } catch (UnknownHostException e) {
            logger.warn(String.format("Unable to obtain IP address for host %s for actor %s", host, uriAsString));
            return TransportRoutingInterface.UNKNOWN_POSITION;
        }
    }

    // USE THIS FOR TESTS ONLY
    public static TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
        try {
            URI uri = new URI(uriAsString);
            // this uses ports used for testing mock hardware on localhost, do not use in production
            int port = uri.getPort();
            if (port == 4840) return new TransportRoutingInterface.Position("37");
            if (port == 4841) return new TransportRoutingInterface.Position("31");
            if (port == 4842) return new TransportRoutingInterface.Position("32");
            if (port == 4843) return new TransportRoutingInterface.Position("38");
            if (port == 4844) return new TransportRoutingInterface.Position("39");
            if (port == 4845) return new TransportRoutingInterface.Position("45");
            if (port == 4846) return new TransportRoutingInterface.Position("46");
            //if (port == 4847) return new TransportRoutingInterface.Position("37"); Folding in is not visible to MES
            //if (port == 4848) return new TransportRoutingInterface.Position("38"); Folding tt is not visible to MES
            if (port == 4849) return new TransportRoutingInterface.Position("40");
            if (port == 4850) return new TransportRoutingInterface.Position("40");
            if (port == 4851) return new TransportRoutingInterface.Position("40");
            if (port == 4852) return new TransportRoutingInterface.Position("41");
            if (port == 4853) return new TransportRoutingInterface.Position("42");
            if (port == 4854) return new TransportRoutingInterface.Position("43");
            return TransportRoutingInterface.UNKNOWN_POSITION;
        } catch (URISyntaxException e) {
            logger.warn(String.format("Unable to load URI for actor %s", uriAsString));
            return TransportRoutingInterface.UNKNOWN_POSITION;
        }
    }
}
