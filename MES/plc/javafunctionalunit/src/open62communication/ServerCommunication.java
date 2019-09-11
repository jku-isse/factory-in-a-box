/**
   [Class description.  The first sentence should be a meaningful summary of the class since it
   will be displayed as the class summary on the Javadoc package page.]

   [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
   about desired improvements, etc.]
   @author Michael Bishara
   @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
   @author <A HREF="https://github.com/michaelanis14">[Github]</A>
   @date 10 Sep 2019
**/

package open62communication;

import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_Argument;
import open62Wrap.UA_MethodAttributes;
import open62Wrap.UA_NodeId;

public class ServerCommunication {

	public static Object CreateServer(String host, int port) {
		return ServerAPIBase.CreateServer(host, port);
	}

	public static int RunServer(Object server) {
		return ServerAPIBase.RunServer((SWIGTYPE_p_UA_Server) server);
	}

	public void AddMonitoredItem(Object jAPIBase, Object server, Object immId) {
		ServerAPIBase.AddMonitoredItem((ServerAPIBase) jAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) immId);
	}

	public static Object AddObject(Object server, Object requestedNewNodeId, String name) {
		return ServerAPIBase.AddObject((SWIGTYPE_p_UA_Server) server, (UA_NodeId) requestedNewNodeId, name);
	}

	public static Object AddVariableNode(Object server, Object objectId, Object requestedNewNodeId, String name,
			int typeId, int accessLevel) {
		return ServerAPIBase.AddVariableNode((SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
				(UA_NodeId) requestedNewNodeId, name, typeId, accessLevel);
	}

	public static int WriteVariable(Object server, Object nodeId, int intValue) {
		return ServerAPIBase.WriteVariable((SWIGTYPE_p_UA_Server) server, (UA_NodeId) nodeId, intValue);
	}

	public int WriteVariable(Object server, Object nodeId, String stringValue) {
		return ServerAPIBase.WriteVariable((SWIGTYPE_p_UA_Server) server, (UA_NodeId) nodeId, stringValue);
	}

	public int WriteVariable(Object server, Object nodeId, double doubleValue) {

		return ServerAPIBase.WriteVariable((SWIGTYPE_p_UA_Server) server, (UA_NodeId) nodeId, doubleValue);
	}

	public static Object AddMethod(Object jAPIBase, Object server, Object objectId, Object requestedNewNodeId,
			Object inputArgument, Object outputArgument, Object methodAttr) {

		return ServerAPIBase.AddMethod((ServerAPIBase) jAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
				(UA_NodeId) requestedNewNodeId, (UA_Argument) inputArgument, (UA_Argument) outputArgument,
				(UA_MethodAttributes) methodAttr);

	}

	public void SetMethodOutput(Object nodeId, String output) {

		ServerAPIBase.SetMethodOutput((UA_NodeId) nodeId, output);
	}
}
