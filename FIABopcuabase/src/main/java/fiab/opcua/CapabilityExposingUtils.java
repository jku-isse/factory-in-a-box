package fiab.opcua;

import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;

import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.opcua.server.OPCUABase;

public class CapabilityExposingUtils {

	public static void setupCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path, CapabilityImplementationMetadata cp) {
			setupCapabilities(opcuaBase, ttNode, path, "", cp);
	}
		
	private static void setupCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path, String capabilityPostfix, CapabilityImplementationMetadata cp) {
		// add capabilities 
		String capString = OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY+capabilityPostfix;
		UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(ttNode, path, new String( OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
		opcuaBase.generateStringVariableNode(capability1, path+"/"+capString,  OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE, cp.getCapabilityURI());
		opcuaBase.generateStringVariableNode(capability1, path+"/"+capString,  OPCUACapabilitiesAndWiringInfoBrowsenames.ID, cp.getImplId());
		opcuaBase.generateStringVariableNode(capability1, path+"/"+capString,  OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE, 
				cp.isProvided() ? OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED : OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_REQUIRED);
	}

	public static void setupCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path, List<CapabilityImplementationMetadata> cps) {
		IntStream.range(0, cps.size())
		.forEach(i -> setupCapabilities(opcuaBase, ttNode, path, ""+(i+1), cps.get(i)) );
	}
	
	
//	@Value public static class CapabilityProvisioning {
//		String uri;
//		String id;
//		boolean isProvided;
//	}
	
}
