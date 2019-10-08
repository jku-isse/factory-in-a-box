/**
 * [Class description.  The first sentence should be a meaningful summary of the class since it
 * will be displayed as the class summary on the Javadoc package page.]
 * [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
 * about desired improvements, etc.]
 *
 * @author Michael Bishara
 * @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
 * @author <A HREF="https://github.com/michaelanis14">[Github]</A>
 * @date 10 Sep 2019
 **/

package communication.open62communication;

import communication.utils.RequestedNodePair;
import open62Wrap.*;

import java.util.HashMap;
import java.util.function.Function;

public class ServerCommunication extends ServerAPIBase {

    private HashMap<Integer, Function<String, String>> functionMap;
    private int unique_id = 10;

    public int getUnique_id() {
        return unique_id += 1;
    }

    public ServerCommunication() {
        functionMap = new HashMap<>();
    }


    /**
     * Creates a String to String function to use in the method callback.
     *
     * @param function string to string function. If the String should not change use x -> x
     */
    private void addStringFunction(Integer methodId, Function<String, String> function) {
        functionMap.put(methodId, function);
    }

    /**
     * Returns the function
     *
     * @return function
     */
    private Function<String, String> getFunction(Object methodId) {
        return functionMap.get(methodId);
    }

    @Override
    public void monitored_itemChanged(UA_NodeId nodeId, int value) {
        System.out.println("iiiiii FROM JAVA 2: monitored_itemChanged::monitored_itemChanged() invoked." + value
                + nodeId.getIdentifierType() + nodeId.getIdentifier().getString());

    }

    /**
     * Callback that executes the function and sets it's return value as the output.
     *
     * @param methodId method id
     * @param objectId object id
     * @param input    input
     * @param jAPIBase serverAPIBase of the server
     */
    @Override
    public void methods_callback(UA_NodeId methodId, UA_NodeId objectId, String input, String output, ServerAPIBase jAPIBase) {
        setMethodOutput(methodId, getFunction(methodId.getIdentifier().getNumeric()).apply(input));
    }

    public Object createServer(String host, int port) {
        return ServerAPIBase.CreateServer(host, port);
    }

    public int runServer(Object server) {
        return ServerAPIBase.RunServer((SWIGTYPE_p_UA_Server) server);
    }

    public void addMonitoredItem(Object serverAPIBase, Object server, Object immId) {
        ServerAPIBase.AddMonitoredItem((ServerAPIBase) serverAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) immId);
    }


    public Object addObject(Object server, Object requestedNewNodeId, String name) {
        return ServerAPIBase.AddObject((SWIGTYPE_p_UA_Server) server, (UA_NodeId) requestedNewNodeId, name);
    }

    public Object addObject(Object server, RequestedNodePair<Integer, Integer> requestedNewNodeId, String name) {
        return ServerAPIBase.AddObject((SWIGTYPE_p_UA_Server) server,
                open62541.UA_NODEID_NUMERIC(requestedNewNodeId.getKey(), requestedNewNodeId.getValue()), name);
    }

    public Object addNestedObject(Object server, Object parent, Object requestedNewNodeId, String name) {
        return ServerAPIBase.AddObject((SWIGTYPE_p_UA_Server) server, (UA_NodeId) parent, (UA_NodeId) requestedNewNodeId, name);
    }

    public Object addVariableNode(Object server, Object objectId, RequestedNodePair<Integer, Integer> requestedNewNodeId, String name,
                                  int typeId, int accessLevel) {
        return ServerAPIBase.AddVariableNode((SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
                open62541.UA_NODEID_NUMERIC(requestedNewNodeId.getKey(), requestedNewNodeId.getValue()), name, typeId, accessLevel);
    }

    public Object addIntegerVariableNode(Object server, Object objectId, RequestedNodePair<Integer, Integer> requestedNewNodeId, String name) {
        return ServerAPIBase.AddVariableNode((SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
                open62541.UA_NODEID_NUMERIC(requestedNewNodeId.getKey(), requestedNewNodeId.getValue()), name,
                open62541.UA_TYPES_INT32, (open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ));
    }

    public Object addStringVariableNode(Object server, Object objectId, RequestedNodePair<Integer, Integer> requestedNewNodeId, String name) {
        return ServerAPIBase.AddVariableNode((SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
                open62541.UA_NODEID_NUMERIC(requestedNewNodeId.getKey(), requestedNewNodeId.getValue()), name,
                open62541.UA_TYPES_STRING, (open62541.UA_ACCESSLEVELMASK_WRITE | open62541.UA_ACCESSLEVELMASK_READ));
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

    public Object addMethod(Object serverAPIBase, Object server, Object objectId, Object requestedNewNodeId,
                            Object inputArgument, Object outputArgument, Object methodAttr) {

        return ServerAPIBase.AddMethod((ServerAPIBase) serverAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
                (UA_NodeId) requestedNewNodeId, (UA_Argument) inputArgument, (UA_Argument) outputArgument,
                (UA_MethodAttributes) methodAttr);
    }

    public Object addStringMethod(Object serverAPIBase, Object server, Object objectId, RequestedNodePair<Integer, Integer> requestedNewNodeId,
                                  String methodName, Function<String, String> function) {
        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText(methodName);

        UA_LocalizedText localeOut = new UA_LocalizedText();
        localeOut.setLocale("en-US");
        localeOut.setText("Success?");

        UA_Argument input = new UA_Argument();
        input.setDescription(localeIn);
        input.setName("Input");
        input.setDataType(ServerAPIBase.GetDataTypeNode(open62541.UA_TYPES_STRING));
        input.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_Argument output = new UA_Argument();
        output.setDescription(localeOut);
        output.setDataType(ServerAPIBase.GetDataTypeNode(open62541.UA_TYPES_STRING));
        output.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_LocalizedText methodLocale = new UA_LocalizedText();
        methodLocale.setText(methodName);

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        UA_NodeId reqMethodId = open62541.UA_NODEID_NUMERIC(requestedNewNodeId.getKey(), requestedNewNodeId.getValue());
        Object methodId = ServerAPIBase.AddMethod((ServerAPIBase) serverAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
                reqMethodId,
                input, output, methodAttributes);
        addStringFunction(reqMethodId.getIdentifier().getNumeric(), function);
        return methodId;
    }

    public Object addStringArrayMethod(Object serverAPIBase, Object server, Object objectId, RequestedNodePair<Integer, Integer> requestedNewNodeId,
                                       String methodName, Function<String, String> function) {
        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText(methodName);

        UA_LocalizedText localeOut = new UA_LocalizedText();
        localeOut.setLocale("en-US");
        localeOut.setText("Success?");

        UA_Argument input = new UA_Argument();
        input.setDescription(localeIn);
        input.setName("Input");
        input.setDataType(ServerAPIBase.GetDataTypeNode(open62541.UA_TYPES_STRING));
        input.setValueRank(open62541.UA_VALUERANK_ONE_DIMENSION);

        UA_Argument output = new UA_Argument();
        output.setDescription(localeOut);
        output.setDataType(ServerAPIBase.GetDataTypeNode(open62541.UA_TYPES_STRING));
        output.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_LocalizedText methodLocale = new UA_LocalizedText();
        methodLocale.setText(methodName);

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        UA_NodeId reqMethodId = open62541.UA_NODEID_NUMERIC(requestedNewNodeId.getKey(), requestedNewNodeId.getValue());
        Object methodId = ServerAPIBase.AddMethod((ServerAPIBase) serverAPIBase, (SWIGTYPE_p_UA_Server) server, (UA_NodeId) objectId,
                reqMethodId,
                input, output, methodAttributes);
        addStringFunction(reqMethodId.getIdentifier().getNumeric(), function);
        return methodId;
    }


    public void setMethodOutput(Object nodeId, String output) {
        ServerAPIBase.SetMethodOutput((UA_NodeId) nodeId, output);
    }

    public Object createNodeNumeric(int nameSpace, int id) {
        return open62541.UA_NODEID_NUMERIC(nameSpace, id);
    }
}
