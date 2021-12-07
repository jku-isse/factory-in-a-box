package fiab.mes.productioncell.foldingstation;

import com.google.common.collect.Lists;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.transport.actor.transportmodule.InternalCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;

import java.util.*;

public class HardcodedFoldingCellTransportRoutingAndMapping implements TransportRoutingInterface, InternalCapabilityToPositionMapping {

	// FACTORY IN A BOX - SHOPFLOOR LAYOUT:
	// TT1 and TT2 in the middle, 20,21
	// Input Outputstation left and right: 34,35
	// Plotters top middle, bottom middle, 31,32,37,38 in use, 30,33,36,39 currently not in use
	// IP Addresses encode locations and remain fixed (USB lan adapter stay with their locations)
	// IP 23 is used for an iostation at pos 21!
	// IP 22 is used for an iostation at pos 20!
	// 30 | 31   | 32   | 33
	// 34 | 20/2 | 21/3 | 35
	// 36 | 37   | 38   | 39

	// routing table: - boy, are we implementing the IP stack here?
	private Map<Position, Position> edgeNodeMapping = new HashMap<>();
	private Map<Position, Set<Position>> routerConnections = new HashMap<>();

	private Position pos31 = new Position("31");
	private Position pos34 = new Position("34");
	private Position pos37 = new Position("37");
	private Position pos20 = new Position("20");
	private Position pos21 = new Position("21");
	private Position pos23 = new Position("23");

	public HardcodedFoldingCellTransportRoutingAndMapping() {
		setupHardcodedLayout();
		setupHardcodedCapabilityToPositionMapping();
	}

	private void setupHardcodedLayout() {
		// default Gateways
		edgeNodeMapping.put(pos31, pos20);
		edgeNodeMapping.put(pos34, pos20);
		edgeNodeMapping.put(pos37, pos20);
		edgeNodeMapping.put(pos21, pos20);
		//edgeNodeMapping.put(pos32, pos21);
		//edgeNodeMapping.put(pos35, pos21);
		//edgeNodeMapping.put(pos38, pos21);
		edgeNodeMapping.put(pos20, pos21);
		// alternative pos at 21 and 22 for ip 22 and 23
		//edgeNodeMapping.put(pos22, pos21); // TT2 to 22
		//edgeNodeMapping.put(pos21, pos22);
		//edgeNodeMapping.put(pos22, pos23); // 22 to 23
		//edgeNodeMapping.put(pos23, pos22);
		//edgeNodeMapping.put(pos23, pos20); //TT1 to 23
		//edgeNodeMapping.put(pos20, pos23);
		// router ports, note that 22 and 23 are not router position but rather just placeholders for single TT usecases
		routerConnections.put(pos20, new HashSet<Position>(Arrays.asList(pos31,pos34,pos37,pos21,pos23)));
		//routerConnections.put(pos21, new HashSet<Position>(Arrays.asList(pos32,pos35,pos38,pos20,pos22)));
	}

	// for now we use machine ids
	@Override
	public List<Position> calculateRoute(Position fromMachine, Position toMachine) throws RoutingException{
		// a route consists of a list of stations, starting with the fromMachineId, and ending with toMachineId, and any transportsystem hops inbetween
		if (!edgeNodeMapping.containsKey(fromMachine))
			throw new RoutingException("Source position not known", RoutingException.Error.UNKNOWN_POSITION);
		if (!edgeNodeMapping.containsKey(toMachine))
			throw new RoutingException("Source position not known", RoutingException.Error.UNKNOWN_POSITION);

		List<Position> route = new ArrayList<Position>();
		route.add(fromMachine);
		if (isDirectlyConnected(fromMachine, toMachine)) {
			//done
		} else if (isSameRouter(fromMachine, toMachine)) {
			route.add(edgeNodeMapping.get(fromMachine));
		} else { // Intermediary routers needed,
			route.addAll(collectRouterConnections(edgeNodeMapping.get(fromMachine), edgeNodeMapping.get(toMachine)));
		}
		route.add(toMachine);
		return route;
	}

	private boolean isDirectlyConnected(Position pos1, Position pos2) {
		return (edgeNodeMapping.get(pos1).equals(pos2) || edgeNodeMapping.get(pos2).equals(pos1));
	}

	private boolean isSameRouter(Position pos1, Position pos2) {
		return edgeNodeMapping.get(pos1).equals(edgeNodeMapping.get(pos2));
	}

	// we limit ourselves to two turntables for now
	private List<Position> collectRouterConnections(Position pos1, Position pos2) throws RoutingException {
		//FIXME: only supports two hops thus distance 3, beyond that Routing exception throws, would need recursive search
		//collect for any of these positions their neighbors (need to be routers
		if (!routerConnections.containsKey(pos1))
			throw new RoutingException("Router not known:"+pos1, RoutingException.Error.UNKNOWN_POSITION);
		if (!routerConnections.containsKey(pos2))
			throw new RoutingException("Router not known:"+pos2, RoutingException.Error.UNKNOWN_POSITION);
		if (routerConnections.get(pos1).contains(pos2)) {
			return Lists.newArrayList(pos1, pos2);
		} else {
			HashSet<Position> copy1 = new HashSet<>(routerConnections.get(pos1));
			HashSet<Position> copy2 = new HashSet<>(routerConnections.get(pos2));
			copy1.retainAll(copy2); // copy1 now is the intersect/overlap
			if (copy1.isEmpty())
				throw new RoutingException(String.format("Route not found between default gateways %s and %s ", pos1, pos2), RoutingException.Error.NO_ROUTE);
			else { // return first option
				return Lists.newArrayList(pos1, copy1.stream().findAny().get(), pos2);
			}
		}
	}

	private Map<String, Position> tt20map = new HashMap<>();
	private Map<String, Position> tt21map = new HashMap<>();
	private Map<Position, String> pos20cap = new HashMap<>();
	private Map<Position, String> pos21cap = new HashMap<>();


	private void setupHardcodedCapabilityToPositionMapping() {
		//tt20map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER, pos21);
		tt20map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, pos21); //when no second TT available
		tt20map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, pos34);
		tt20map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, pos37);
		tt20map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, pos31);
		tt20map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SELF, pos20);

		/*tt21map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, pos35);
		tt21map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, pos20);
		//tt21map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_SERVER, pos22); // when no second TT available; ACTUALLY WESTSERVER should also be pos20
		tt21map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, pos38);
		tt21map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, pos32);
		tt21map.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SELF, pos21);*/

		//pos20cap.put(pos21, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER);
		pos20cap.put(pos21, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT); // when no second TT available
		pos20cap.put(pos34, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT);
		pos20cap.put(pos37, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT);
		pos20cap.put(pos31, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT);
		pos20cap.put(pos20, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SELF);
		
		/*pos21cap.put(pos35, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT);
		pos21cap.put(pos20, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT);
		pos21cap.put(pos22, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT); //when using a server at TT1 pos, instead of TT1
		pos21cap.put(pos38, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT);
		pos21cap.put(pos32, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT);
		pos21cap.put(pos21, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SELF);	*/
	}


	@Override
	public Position getPositionForCapability(String capabilityId, Position selfPos) {
		if (selfPos.equals(pos20)) {
			return tt20map.getOrDefault(capabilityId, TransportRoutingInterface.UNKNOWN_POSITION);
		} else if (selfPos.equals(pos21)) {
			return tt21map.getOrDefault(capabilityId, TransportRoutingInterface.UNKNOWN_POSITION);
		} else return TransportRoutingInterface.UNKNOWN_POSITION;
	}

	@Override
	public Optional<String> getCapabilityIdForPosition(Position pos, Position selfPos) {
		if (selfPos.equals(pos20)) {
			return Optional.ofNullable(pos20cap.get(pos));
		} else if (selfPos.equals(pos21)) {
			return Optional.ofNullable(pos21cap.get(pos));
		} else return Optional.empty();
	}

}
