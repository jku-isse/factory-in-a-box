
/**
   [Class description.  The first sentence should be a meaningful summary of the class since it
   will be displayed as the class summary on the Javadoc package page.]

   [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
   about desired improvements, etc.]
   @author Michael Bishara
   @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
   @author <A HREF="https://github.com/michaelanis14">[Github: Michael Bishara]</A>
   @date 4 Sep 2019
**/
package controller;

import capabilities.HandshakeFU;
import communication.Communication;
import helper.CapabilityId;
import robot.RobotBase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

public class Controller {
	static {
		try {
			System.out.println("Looking for native lib");
			loadNativeLib();    //change the library in this method depending on your platform
			System.out.println("Found native lib");
		} catch (IOException e) {
			System.out.println("Cannot find native lib");
			e.printStackTrace();
		}
	}
	private static void loadNativeLib() throws IOException {
		String libName = "libOpcua-Java-API_hf.so"; //use this on BrickPi, use the one w/o _hf suffix on ev3
		//String libName = "opcua_java_api.dll"; //use this on windows (needs 32 bit java)
		URL url = RobotBase.class.getResource("/" + libName);
		File tmpDir = Files.createTempDirectory("my-native-lib").toFile();
		tmpDir.deleteOnExit();
		File nativeLibTmpFile = new File(tmpDir, libName);
		nativeLibTmpFile.deleteOnExit();
		try (InputStream in = url.openStream()) {
			Files.copy(in, nativeLibTmpFile.toPath());
		} catch (Exception e) {
			System.out.println("Error in loadNativeLib");
			e.printStackTrace();
		}
		System.load(nativeLibTmpFile.getAbsolutePath());
	}


	public static Object turn(int[] input) {

		System.out.println("Turning Callback stop");
		return "TURNING";

	}

	/**
	 * main method for testing purposes. calling the constructor then running the
	 * server in a separate thread.
	 */


	public static void main(String[] args) {

		//Turning Table
		Communication opcua_comm = new Communication();
		Object opcua_server = opcua_comm.getServerCommunication().createServer("localhost", 4840);
		Object rootObjectId = opcua_comm.getServerCommunication().createNodeNumeric(1, opcua_comm.getServerCommunication().getUnique_id());
		Object opcua_object = opcua_comm.getServerCommunication().addObject(opcua_server, rootObjectId, "Turntable");


				//should be moved to the base class




		HandshakeFU hsFU = new HandshakeFU(opcua_comm.getServerCommunication(),opcua_server,opcua_object,CapabilityId.NORTH_SERVER);

	//	hsFU.getEndpoint_object()
		Object opcua_client = opcua_comm.getClientCommunication().initClient();
		HandshakeFU hsFUClient = new HandshakeFU(opcua_comm,opcua_server,opcua_client,opcua_object,CapabilityId.NORTH_CLIENT);

		new Thread(new Runnable() {
			@Override
			public void run() {
				opcua_comm.getServerCommunication().runServer(opcua_server);
			}
		}).start();






		// Process Engine
		//hsFU.setRequiredCapability(CapabilityId.NORTH_CLIENT, CapabilityType.EngageInUnLoading);
		//hsFU.setWiring(CapabilityId.NORTH_CLIENT, "opc.tcp://localhost:4840");
		//hsFU.initiateUnloading("NORTH", "0001");
		System.err.println("Controller Main Started");

	}
}
