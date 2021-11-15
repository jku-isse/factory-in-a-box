package fiab.handshake.fu.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fiab.core.capabilities.wiring.WiringInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

public class WiringUtils {

	static String FILE = "wiringinfo.json";
	
	public static Optional<HashMap<String, WiringInfo>> loadWiringInfoFromFileSystem(String machinePrefix) {
		ObjectMapper objectMapper = new ObjectMapper();
		//If file cannot be found check IntelliJ working directory in run configuration
		try {
			File file = new File(machinePrefix+FILE);
			System.out.println("Searching for wiring info file in path: " + file.getAbsolutePath());
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
	
}
