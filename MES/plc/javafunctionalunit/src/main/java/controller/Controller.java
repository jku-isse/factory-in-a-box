
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

public class Controller {



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
