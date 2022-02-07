package fiab.opcua.wiring.utils;

import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;

import fiab.core.capabilities.wiring.WiringInfo;
import fiab.opcua.CapabilityImplInfo;
import fiab.opcua.client.OPCUAClientFactory;

public class RewiringUtil {

    public static void main(String[] args) {


        // remove the wiring between two actors/systems/components
        //		requires ID of capability and wiring to remove --> will be set to nulls
        //		requires to become an OPC UA client for original wiring endpoint
        // add the wiring between two other actors
        // 		require details of new wiring to add (similar as in the json file)
        //		requires to become an OPC UA client for new wiring endpoint
        // pragmatically this occurs only on the client side of a required/offers relation

        // for station relocation scenario there are two modes possible:
        // 	1) two-step = first removing, then relocating, then setting new wiring (thus some break inbetween)
        //  2) single-step = relocating while keeping the wiring info intact, then deleting and setting in one go --> ONLY THIS ONE IS SUPPORTED FOR NOW HERE
//		WiringInfo newWI = new WiringInfo("NORTH_CLIENT",
//									"DefaultHandshakeServerSide",
//									"opc.tcp://localhost:4841/milo",
//									"ns=2;s=OutputStation1/IOSTATION/HANDSHAKE_FU_DefaultServerSideHandshake/CAPABILITIES/CAPABILITY",
//									"RemoteRole1");
        WiringInfo newWI = new WiringInfo("NORTH_CLIENT",
                "DefaultHandshakeServerSide",
                "opc.tcp://192.168.0.32:4840",
                "ns=1;i=327",
                "RemoteRole1");


        //String endpointUrl = "opc.tcp://localhost:4843/milo";
        String endpointUrl = "opc.tcp://192.168.0.21:4842/milo";
        String newCapToWire = "NORTH_CLIENT";
        try {
            new RewiringUtil().removeWireFromAndAddInfoTo(endpointUrl, "EAST_CLIENT", endpointUrl, newCapToWire, newWI);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void removeWireFromAndAddInfoTo(String oldEndpointUrl, String capWiringToDelete, String newEndpointUrl, String capToWire, WiringInfo wi) throws Exception {
        OpcUaClient oldClient = new OPCUAClientFactory().createClient(oldEndpointUrl);
        oldClient.connect().get();
        new CapabilityDiscovery(oldEndpointUrl, oldClient).discoverAll().stream().filter(cii -> cii.metaData.getImplId().equals(capWiringToDelete)).findAny().ifPresent(
                cii -> executeWiringInformation(cii, new WiringInfo("","","","",""))					// to override the old wiring
        );
        oldClient.disconnect();

        // now wire the new capability
        OpcUaClient newClient = new OPCUAClientFactory().createClient(newEndpointUrl);
        newClient.connect().get();
        new CapabilityDiscovery(newEndpointUrl, newClient).discoverAll().stream().filter(cii -> cii.metaData.getImplId().equals(capToWire)).findAny().ifPresent(
                cii -> executeWiringInformation(cii, wi)
        );
        newClient.disconnect();
    }

    private void executeWiringInformation(CapabilityImplInfoExt cii, WiringInfo wi) {
        try {
            NodeId actorId = cii.getActorNode();
            //NodeId objectId = cii.getCapabilitiesNode();
            //NodeId methodId = NodeId.parse(cii.getCapabilitiesNode().toParseableString().replace("/CAPABILITIES", "") + "/SET_WIRING");
            NodeId methodId = NodeId.parse(cii.getActorNode().toParseableString() + "/CAPABILITIES/SET_WIRING");
            String lCapID = wi.getLocalCapabilityId();
            String rCapID = wi.getRemoteCapabilityId();
            String rEndpoint = wi.getRemoteEndpointURL();
            String rNodeID = wi.getRemoteNodeId();
            String role = wi.getRemoteRole();
            CallMethodRequest req = new CallMethodRequest(actorId, methodId, new Variant[] { new Variant(lCapID),
                    new Variant(rCapID), new Variant(rEndpoint), new Variant(rNodeID), new Variant(role) });
            System.out.println(cii.getClient().call(req).get());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



}
