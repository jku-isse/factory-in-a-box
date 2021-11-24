package fiab.mes.transport.actor.transportsystem;

import fiab.mes.transport.actor.transportmodule.InternalCapabilityToPositionMapping;

import java.util.List;
import java.util.Optional;

// FACTORY IN A BOX - SHOPFLOOR LAYOUT:
// TT1, TT2 and TT3 in the middle, 20,21,22
// Input Outputstation left and right + one for plot only: 37,43, 32
// Plotters top left, bottom left*2, 31,37,38
// Folding stations middle, 41*3
// Note: This will allow the handshake to address multiple foldingStations at different positions,
//      the actual transport is performed via the FoldingCellCoordinator
// Transit station, 42 - It is not connected to Folding Stations, but the fs will relocate the pallet here
// IP Addresses encode locations and remain fixed (USB lan adapter stay with their locations)
// 30(#)  | 31(Pl)   | 32(O)    | 41(Fo) | 34(#)  | 36(#)  | 37(#)
// 38(IO) | 20/2(TT) | 21/3(TT) | 41(Fo) | 42(Tr) | 22(TT) | 44(O)
// 45(#)  | 46(Pl)   | 47(Pl)   | 41(Fo) | 49(#)  | 50(#)  | 51(#)

public class HardcodedFoldingTransportRoutingAndMapping implements TransportRoutingInterface, InternalCapabilityToPositionMapping {

    //TODO implement layout from above



    @Override
    public Position getPositionForCapability(String capabilityId, Position selfPos) {
        return null;
    }

    @Override
    public Optional<String> getCapabilityIdForPosition(Position pos, Position selfPos) {
        return Optional.empty();
    }

    @Override
    public List<Position> calculateRoute(Position fromMachine, Position toMachine) throws RoutingException {
        return null;
    }
}
