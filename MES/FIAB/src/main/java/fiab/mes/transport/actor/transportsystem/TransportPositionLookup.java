package fiab.mes.transport.actor.transportsystem;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.impl.Parseable;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public class TransportPositionLookup implements TransportPositionLookupInterface {

	private static final Logger logger = LoggerFactory.getLogger(TransportPositionLookup.class);
	
	private HashMap<Position, AkkaActorBackedCoreModelAbstractActor> lookupTable = new HashMap<>();
	
	@Override
	public Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor) {
		
		Position pos = parseLastIPPos(actor.getModelActor().getUri());
		if (pos != TransportRoutingInterface.UNKNOWN_POSITION)
			lookupTable.put(pos, actor);
		return pos;
	}

	@Override
	public Optional<AkkaActorBackedCoreModelAbstractActor> getActorForPosition(Position pos) {
		return Optional.ofNullable(lookupTable.get(pos));
	}
	
	public static Position parseLastIPPos(String uriAsString) {
		String host = "UNKNOWN";
		try {
			URI uri = new URI(uriAsString);
			// this code just reads the last digit from the IPv4 address and returns this as position			
			host = uri.getHost();
			if (host == null) {
				logger.warn(String.format("URI for actor %s has no host part for resolving Position", uriAsString));
				return TransportRoutingInterface.UNKNOWN_POSITION;
			}
			InetAddress inetAddr = InetAddress.getByName(host);
			int lastPos = (inetAddr.getAddress()[3]+256)%256;
			Position pos = new Position(""+lastPos);			
			return pos;
		} catch (URISyntaxException e) {
			logger.warn(String.format("Unable to load URI for actor %s", uriAsString));
			return TransportRoutingInterface.UNKNOWN_POSITION;
		} catch (UnknownHostException e) {
			logger.warn(String.format("Unable to obtain IP address for host %s for actor %s", host, uriAsString));
			return TransportRoutingInterface.UNKNOWN_POSITION;
		}
	}
	
	// USE THIS FOR TESTS ONLY
	public static Position parsePosViaPortNr(String uriAsString) {
		try {
			URI uri = new URI(uriAsString);
			// this uses ports used for testing mock hardware on localhost, do not use in production		
			int port = uri.getPort();			
			if (port == 4840) return new Position("34");
			if (port == 4841) return new Position("35");
			if (port == 4843) return new Position("20");
			if (port == 4843) return new Position("21");
			if (port == 4845) return new Position("31");
			if (port == 4846) return new Position("32");
			if (port == 4847) return new Position("37");
			if (port == 4848) return new Position("38");			
			return TransportRoutingInterface.UNKNOWN_POSITION;
		} catch (URISyntaxException e) {
			logger.warn(String.format("Unable to load URI for actor %s", uriAsString));
			return TransportRoutingInterface.UNKNOWN_POSITION;		
		}
	}
}
