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

import open62communication.ClientCommunication;
import open62communication.ServerCommunication;

public class Communication {

	ClientCommunication clientCommunication = new ClientCommunication();
	ServerCommunication serverCommunication = new ServerCommunication();

	public ClientCommunication getClientCommunication() {
		return new ClientCommunication();
	}

	public ServerCommunication getServerCommunication() {
		return serverCommunication;
	}

}
