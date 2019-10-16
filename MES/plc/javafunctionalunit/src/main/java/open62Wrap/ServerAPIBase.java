/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package open62Wrap;

public class ServerAPIBase {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected ServerAPIBase(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(ServerAPIBase obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        open62541JNI.delete_ServerAPIBase(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  protected void swigDirectorDisconnect() {
    swigCMemOwn = false;
    delete();
  }

  public void swigReleaseOwnership() {
    swigCMemOwn = false;
    open62541JNI.ServerAPIBase_change_ownership(this, swigCPtr, false);
  }

  public void swigTakeOwnership() {
    swigCMemOwn = true;
    open62541JNI.ServerAPIBase_change_ownership(this, swigCPtr, true);
  }

  public void setRunning(boolean value) {
    open62541JNI.ServerAPIBase_running_set(swigCPtr, this, value);
  }

  public boolean getRunning() {
    return open62541JNI.ServerAPIBase_running_get(swigCPtr, this);
  }

  public static ServerAPIBase Get() {
    long cPtr = open62541JNI.ServerAPIBase_Get();
    return (cPtr == 0) ? null : new ServerAPIBase(cPtr, false);
  }

  public static void stopHandler(int sig) {
    open62541JNI.ServerAPIBase_stopHandler(sig);
  }

  public static SWIGTYPE_p_UA_Server CreateServerDefaultConfig() {
    long cPtr = open62541JNI.ServerAPIBase_CreateServerDefaultConfig();
    return (cPtr == 0) ? null : new SWIGTYPE_p_UA_Server(cPtr, false);
  }

  public static SWIGTYPE_p_UA_Server CreateServer(String host, int port) {
    long cPtr = open62541JNI.ServerAPIBase_CreateServer(host, port);
    return (cPtr == 0) ? null : new SWIGTYPE_p_UA_Server(cPtr, false);
  }

  public static int RunServer(SWIGTYPE_p_UA_Server server) {
    return open62541JNI.ServerAPIBase_RunServer(SWIGTYPE_p_UA_Server.getCPtr(server));
  }

  public static void AddMonitoredItem(ServerAPIBase jAPIBase, SWIGTYPE_p_UA_Server server, UA_NodeId monitoredItemId) {
    open62541JNI.ServerAPIBase_AddMonitoredItem(ServerAPIBase.getCPtr(jAPIBase), jAPIBase, SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(monitoredItemId), monitoredItemId);
  }

  public static UA_NodeId AddObject(SWIGTYPE_p_UA_Server server, UA_NodeId requestedNewNodeId, String name) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_AddObject__SWIG_0(SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(requestedNewNodeId), requestedNewNodeId, name), true);
  }

  public static UA_NodeId AddObject(SWIGTYPE_p_UA_Server server, UA_NodeId parent, UA_NodeId requestedNewNodeId, String name) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_AddObject__SWIG_1(SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(parent), parent, UA_NodeId.getCPtr(requestedNewNodeId), requestedNewNodeId, name), true);
  }

  public static UA_NodeId AddVariableNode(SWIGTYPE_p_UA_Server server, UA_NodeId objectId, UA_NodeId requestedNewNodeId, String name, int typeId, int accessLevel) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_AddVariableNode(SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(objectId), objectId, UA_NodeId.getCPtr(requestedNewNodeId), requestedNewNodeId, name, typeId, accessLevel), true);
  }

  public static UA_NodeId ManuallyDefineIMM(SWIGTYPE_p_UA_Server server) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_ManuallyDefineIMM(SWIGTYPE_p_UA_Server.getCPtr(server)), true);
  }

  public static UA_NodeId ManuallyDefineRobot(SWIGTYPE_p_UA_Server server) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_ManuallyDefineRobot(SWIGTYPE_p_UA_Server.getCPtr(server)), true);
  }

  public static int WriteVariable(SWIGTYPE_p_UA_Server server, UA_NodeId nodeId, int intValue) {
    return open62541JNI.ServerAPIBase_WriteVariable__SWIG_0(SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(nodeId), nodeId, intValue);
  }

  public static int WriteVariable(SWIGTYPE_p_UA_Server server, UA_NodeId nodeId, String stringValue) {
    return open62541JNI.ServerAPIBase_WriteVariable__SWIG_1(SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(nodeId), nodeId, stringValue);
  }

  public static int WriteVariable(SWIGTYPE_p_UA_Server server, UA_NodeId nodeId, double doubleValue) {
    return open62541JNI.ServerAPIBase_WriteVariable__SWIG_2(SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(nodeId), nodeId, doubleValue);
  }

  public static UA_NodeId GetDataTypeNode(int typeId) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_GetDataTypeNode(typeId), true);
  }

  public static UA_NodeId AddMethod(ServerAPIBase jAPIBase, SWIGTYPE_p_UA_Server server, UA_NodeId objectId, UA_NodeId requestedNewNodeId, UA_Argument inputArgument, UA_Argument outputArgument, UA_MethodAttributes methodAttr) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_AddMethod(ServerAPIBase.getCPtr(jAPIBase), jAPIBase, SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(objectId), objectId, UA_NodeId.getCPtr(requestedNewNodeId), requestedNewNodeId, UA_Argument.getCPtr(inputArgument), inputArgument, UA_Argument.getCPtr(outputArgument), outputArgument, UA_MethodAttributes.getCPtr(methodAttr), methodAttr), true);
  }

  public static UA_NodeId AddArrayMethod(ServerAPIBase jAPIBase, SWIGTYPE_p_UA_Server server, UA_NodeId objectId, UA_NodeId requestedNewNodeId, UA_Argument outputArgument, UA_MethodAttributes methodAttr, String name, String description, int typeId, int pDimension) {
    return new UA_NodeId(open62541JNI.ServerAPIBase_AddArrayMethod(ServerAPIBase.getCPtr(jAPIBase), jAPIBase, SWIGTYPE_p_UA_Server.getCPtr(server), UA_NodeId.getCPtr(objectId), objectId, UA_NodeId.getCPtr(requestedNewNodeId), requestedNewNodeId, UA_Argument.getCPtr(outputArgument), outputArgument, UA_MethodAttributes.getCPtr(methodAttr), methodAttr, name, description, typeId, pDimension), true);
  }

  public static void SetMethodOutput(UA_NodeId methodId, String output) {
    open62541JNI.ServerAPIBase_SetMethodOutput(UA_NodeId.getCPtr(methodId), methodId, output);
  }

  public void monitored_itemChanged(UA_NodeId nodeId, int value) {
    if (getClass() == ServerAPIBase.class) open62541JNI.ServerAPIBase_monitored_itemChanged(swigCPtr, this, UA_NodeId.getCPtr(nodeId), nodeId, value); else open62541JNI.ServerAPIBase_monitored_itemChangedSwigExplicitServerAPIBase(swigCPtr, this, UA_NodeId.getCPtr(nodeId), nodeId, value);
  }

  public void methods_callback(UA_NodeId methodId, UA_NodeId objectId, String input, String output, ServerAPIBase jAPIBase) {
    if (getClass() == ServerAPIBase.class) open62541JNI.ServerAPIBase_methods_callback(swigCPtr, this, UA_NodeId.getCPtr(methodId), methodId, UA_NodeId.getCPtr(objectId), objectId, input, output, ServerAPIBase.getCPtr(jAPIBase), jAPIBase); else open62541JNI.ServerAPIBase_methods_callbackSwigExplicitServerAPIBase(swigCPtr, this, UA_NodeId.getCPtr(methodId), methodId, UA_NodeId.getCPtr(objectId), objectId, input, output, ServerAPIBase.getCPtr(jAPIBase), jAPIBase);
  }

  public ServerAPIBase() {
    this(open62541JNI.new_ServerAPIBase(), true);
    open62541JNI.ServerAPIBase_director_connect(this, swigCPtr, true, true);
  }

}
