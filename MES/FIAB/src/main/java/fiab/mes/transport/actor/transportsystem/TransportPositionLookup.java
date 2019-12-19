package fiab.mes.transport.actor.transportsystem;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public class TransportPositionLookup implements TransportPositionLookupInterface {

	private static final Logger logger = LoggerFactory.getLogger(TransportPositionLookup.class);
	
	@Override
	public Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor) {
		String host = null;
		try {
			// this code just reads the last digit from the IPv4 address and returns this as position
			URI uri = new URI(actor.getModelActor().getUri());
			host = uri.getHost();
			if (host == null) {
				logger.warn(String.format("URI for actor %s has no host part for resolving Position", actor.getModelActor().getUri()));
				return TransportRoutingInterface.UNKNOWN_POSITION;
			}
			InetAddress inetAddr = InetAddress.getByName(host);
			int lastPos = (inetAddr.getAddress()[3]+256)%256;
			return new Position(""+lastPos);
		} catch (URISyntaxException e) {
			logger.warn(String.format("Unable to load URI for actor %s", actor.toString()));
			return TransportRoutingInterface.UNKNOWN_POSITION;
		} catch (UnknownHostException e) {
			logger.warn(String.format("Unable to obtain IP address for host %s for actor %s", host, actor.getModelActor().getUri()));
			return TransportRoutingInterface.UNKNOWN_POSITION;
		}
		
	}
	
}
