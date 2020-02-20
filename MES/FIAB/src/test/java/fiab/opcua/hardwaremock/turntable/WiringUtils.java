package fiab.opcua.hardwaremock.turntable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WiringUtils {

	static String FILE = "wiringinfo.json";
	
	public static Optional<HashMap<String, WiringInfo>> loadWiringInfoFromFileSystem(String machinePrefix) {
		ObjectMapper objectMapper = new ObjectMapper();
	
		try {
			File file = new File(machinePrefix+FILE);
			HashMap<String, WiringInfo> info = objectMapper.readValue(file, new TypeReference<HashMap<String, WiringInfo>>(){});
			return Optional.of(info);
		} catch (IOException e) {			
			e.printStackTrace();
			return Optional.empty();
		}
	}
	
	public static void writeWiringInfoToFileSystem(HashMap<String, WiringInfo> wireMap, String machinePrefix) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.writeValue(
				    new FileOutputStream(machinePrefix+FILE), wireMap);
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	public static class WiringInfo {
		public WiringInfo() {
			super();
		}

		String localCapabilityId;
		String remoteCapabilityId;
		String remoteEndpointURL;
		String remoteNodeId;
		String remoteRole;
		public String getLocalCapabilityId() {
			return localCapabilityId;
		}
		public void setLocalCapabilityId(String localCapabilityId) {
			this.localCapabilityId = localCapabilityId;
		}
		public String getRemoteCapabilityId() {
			return remoteCapabilityId;
		}
		public void setRemoteCapabilityId(String remoteCapabilityId) {
			this.remoteCapabilityId = remoteCapabilityId;
		}
		public String getRemoteEndpointURL() {
			return remoteEndpointURL;
		}
		public void setRemoteEndpointURL(String remoteEndpointURL) {
			this.remoteEndpointURL = remoteEndpointURL;
		}
		public String getRemoteNodeId() {
			return remoteNodeId;
		}
		public void setRemoteNodeId(String remoteNodeId) {
			this.remoteNodeId = remoteNodeId;
		}
		public String getRemoteRole() {
			return remoteRole;
		}
		public void setRemoteRole(String remoteRole) {
			this.remoteRole = remoteRole;
		}
		
		public WiringInfo(String localCapabilityId, String remoteCapabilityId, String remoteEndpointURL,
				String remoteNodeId, String remoteRole) {
			super();
			this.localCapabilityId = localCapabilityId;
			this.remoteCapabilityId = remoteCapabilityId;
			this.remoteEndpointURL = remoteEndpointURL;
			this.remoteNodeId = remoteNodeId;
			this.remoteRole = remoteRole;
		}
		
		
	}
	
}
