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
import open62Wrap.UA_LocalizedText;
import open62Wrap.UA_MethodAttributes;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541;

public class ServerCommunication extends ServerAPIBase {
	@Override
	public void monitored_itemChanged(UA_NodeId nodeId, int value) {
		System.out.println("iiiiii FROM JAVA 2: monitored_itemChanged::monitored_itemChanged() invoked." + value
				+ nodeId.getIdentifierType() + nodeId.getIdentifier().getString());

	}

	@Override
	public void methods_callback(UA_NodeId methodId, UA_NodeId objectId, String input, String output,
			ServerAPIBase jAPIBase) {
		System.out.println(" iiiii Got a methods_callback With input 2:" + input);
		jAPIBase.SetMethodOutput(methodId, "FROM JAVA 2: " + input + " " + methodId.getIdentifier().getNumeric());
		// System.out.println(" iiiii Got a methods_callback setting the output " +
		// jAPIBase.getData());

	}

	public Object createServer(String host, int port) {
		return ServerAPIBase.CreateServer(host, port);
	}

	public int runServer(Object server) {
		return ServerAPIBase.RunServer((SWIGTYPE_p_UA_Server) server);
	}

	public void addMonitoredItem(Object jAPIBase, Object server, Object immId) {
		ServerAPIBase.AddMonitoredItem((ServerAPIBase) jAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) immId);
	}

	public Object addObject(Object server, Object requestedNewNodeId, String name) {
		return ServerAPIBase.AddObject((SWIGTYPE_p_UA_Server) server, (UA_NodeId) requestedNewNodeId, name);
	}

	public Object addVariableNode(Object server, Object objectId, Object requestedNewNodeId, String name, int typeId,
			int accessLevel) {
		return ServerAPIBase.AddVariableNode((SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
				(UA_NodeId) requestedNewNodeId, name, typeId, accessLevel);
	}

	public int writeVariable(Object server, Object nodeId, int intValue) {
		return ServerAPIBase.WriteVariable((SWIGTYPE_p_UA_Server) server, (UA_NodeId) nodeId, intValue);
	}

	public int writeVariable(Object server, Object nodeId, String stringValue) {
		return ServerAPIBase.WriteVariable((SWIGTYPE_p_UA_Server) server, (UA_NodeId) nodeId, stringValue);
	}

	public int writeVariable(Object server, Object nodeId, double doubleValue) {

		return ServerAPIBase.WriteVariable((SWIGTYPE_p_UA_Server) server, (UA_NodeId) nodeId, doubleValue);
	}

	public Object addMethod(Object jAPIBase, Object server, Object objectId, Object requestedNewNodeId,
			Object inputArgument, Object outputArgument, Object methodAttr) {

		return ServerAPIBase.AddMethod((ServerAPIBase) jAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
				(UA_NodeId) requestedNewNodeId, (UA_Argument) inputArgument, (UA_Argument) outputArgument,
				(UA_MethodAttributes) methodAttr);

	}

	public Object createNodeNumeric(int nameSpace, int id) {
		return open62541.UA_NODEID_NUMERIC(nameSpace, id);
	}

	public Object addMethod_string(Object jAPIBase, Object server, Object objectId, int methodId, String methodTitle,
			String inputTitle, String outputTitle) {
		UA_LocalizedText locale = new UA_LocalizedText();
		locale.setLocale("en-US");
		locale.setText("A String");

		UA_Argument input = new UA_Argument();
		input.setDescription(locale);
		input.setName(inputTitle);
		input.setDataType(ServerAPIBase.GetDataTypeNode(open62541.UA_TYPES_STRING));
		input.setValueRank(open62541.UA_VALUERANK_SCALAR);

		UA_Argument output = new UA_Argument();
		output.setDescription(locale);
		output.setName(outputTitle);
		output.setDataType(ServerAPIBase.GetDataTypeNode(open62541.UA_TYPES_STRING));
		output.setValueRank(open62541.UA_VALUERANK_SCALAR);

		UA_LocalizedText methodLocale = new UA_LocalizedText();
		methodLocale.setLocale("en-US");
		methodLocale.setText(methodTitle);

		UA_MethodAttributes methodAttr = new UA_MethodAttributes();
		methodAttr.setDescription(methodLocale);
		methodAttr.setDisplayName(methodLocale);
		methodAttr.setExecutable(true);
		methodAttr.setUserExecutable(true);

		return ServerAPIBase.AddMethod((ServerAPIBase) jAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
				open62541.UA_NODEID_NUMERIC(1, methodId), input, output, methodAttr);

	}

	public void setMethodOutput(Object nodeId, String output) {

		ServerAPIBase.SetMethodOutput((UA_NodeId) nodeId, output);
	}
}
