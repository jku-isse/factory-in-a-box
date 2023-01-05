package fiab.handshake.client.util;

import fiab.core.capabilities.wiring.WiringInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class WiringInfoOpcUaUtil {

    public static Variant[] wiringInfoAsOpcUaArgs(WiringInfo wiringInfo) {
        return new Variant[]{
                new Variant(wiringInfo.getLocalCapabilityId()),
                new Variant(wiringInfo.getRemoteCapabilityId()),
                new Variant(wiringInfo.getRemoteEndpointURL()),
                new Variant(wiringInfo.getRemoteNodeId()),
                new Variant(wiringInfo.getRemoteRole())
        };
    }
}
