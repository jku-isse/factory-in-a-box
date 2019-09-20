/**
   [Class description.  The first sentence should be a meaningful summary of the class since it
   will be displayed as the class summary on the Javadoc package page.]

   [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
   about desired improvements, etc.]
   @author Michael Bishara
   @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
   @author <A HREF="https://github.com/michaelanis14">[Github]</A>
   @date 9 Sep 2019
**/
package communication;

import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;

import java.util.function.Function;

public class Communication {

	private ClientCommunication clientCommunication = null;
	private ServerCommunication serverCommunication = null;

	public Communication() {
		// clientCommunication = new ClientCommunication();
		// serverCommunication = new ServerCommunication();
	}

	public ClientCommunication getClientCommunication() {
		if (clientCommunication == null) {
			clientCommunication = new ClientCommunication();
		}
		return clientCommunication;
	}

	public ServerCommunication getServerCommunication() {
		if (serverCommunication == null) {
			serverCommunication = new ServerCommunication();
		}
		return serverCommunication;
	}




}
