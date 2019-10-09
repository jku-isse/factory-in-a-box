/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package open62Wrap;

public class ClientAPIBase {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected ClientAPIBase(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(ClientAPIBase obj) {
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
        open62541JNI.delete_ClientAPIBase(swigCPtr);
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
    open62541JNI.ClientAPIBase_change_ownership(this, swigCPtr, false);
  }

  public void swigTakeOwnership() {
    swigCMemOwn = true;
    open62541JNI.ClientAPIBase_change_ownership(this, swigCPtr, true);
  }

  public void setRunning(boolean value) {
    open62541JNI.ClientAPIBase_running_set(swigCPtr, this, value);
  }

  public boolean getRunning() {
    return open62541JNI.ClientAPIBase_running_get(swigCPtr, this);
  }

  public static ClientAPIBase Get() {
    long cPtr = open62541JNI.ClientAPIBase_Get();
    return (cPtr == 0) ? null : new ClientAPIBase(cPtr, false);
  }

  public static void stopHandler(int sign) {
    open62541JNI.ClientAPIBase_stopHandler(sign);
  }

  public static void inactivityCallback(SWIGTYPE_p_UA_Client client) {
    open62541JNI.ClientAPIBase_inactivityCallback(SWIGTYPE_p_UA_Client.getCPtr(client));
  }

  public static SWIGTYPE_p_UA_Client InitClient() {
    long cPtr = open62541JNI.ClientAPIBase_InitClient();
    return (cPtr == 0) ? null : new SWIGTYPE_p_UA_Client(cPtr, false);
  }

  public static int ClientConnect(ClientAPIBase jClientAPIBase, SWIGTYPE_p_UA_Client client, String serverUrl) {
    return open62541JNI.ClientAPIBase_ClientConnect(ClientAPIBase.getCPtr(jClientAPIBase), jClientAPIBase, SWIGTYPE_p_UA_Client.getCPtr(client), serverUrl);
  }

  public static UA_NodeId NodeIter(UA_NodeId childId, SWIGTYPE_p_UA_Client client, String nodeName) {
    return new UA_NodeId(open62541JNI.ClientAPIBase_NodeIter(UA_NodeId.getCPtr(childId), childId, SWIGTYPE_p_UA_Client.getCPtr(client), nodeName), true);
  }

  public static UA_NodeId GetNodeByName(SWIGTYPE_p_UA_Client client, String nodeName) {
    return new UA_NodeId(open62541JNI.ClientAPIBase_GetNodeByName(SWIGTYPE_p_UA_Client.getCPtr(client), nodeName), true);
  }

  public static int ClientSubtoNode(ClientAPIBase jClientAPIBase, SWIGTYPE_p_UA_Client client, UA_NodeId nodeID) {
    return open62541JNI.ClientAPIBase_ClientSubtoNode(ClientAPIBase.getCPtr(jClientAPIBase), jClientAPIBase, SWIGTYPE_p_UA_Client.getCPtr(client), UA_NodeId.getCPtr(nodeID), nodeID);
  }

  public static UA_Variant SetGetVariant(UA_Variant value) {
    return new UA_Variant(open62541JNI.ClientAPIBase_SetGetVariant(UA_Variant.getCPtr(value), value), true);
  }

  public static void ClientRemoveSub(SWIGTYPE_p_UA_Client client, int subId) {
    open62541JNI.ClientAPIBase_ClientRemoveSub(SWIGTYPE_p_UA_Client.getCPtr(client), subId);
  }

  public static UA_Variant ClientReadValue(SWIGTYPE_p_UA_Client client, UA_NodeId nodeID) {
    long cPtr = open62541JNI.ClientAPIBase_ClientReadValue(SWIGTYPE_p_UA_Client.getCPtr(client), UA_NodeId.getCPtr(nodeID), nodeID);
    return (cPtr == 0) ? null : new UA_Variant(cPtr, false);
  }

  public static int ClientReadIntValue(SWIGTYPE_p_UA_Client client, UA_NodeId nodeID) {
    return open62541JNI.ClientAPIBase_ClientReadIntValue(SWIGTYPE_p_UA_Client.getCPtr(client), UA_NodeId.getCPtr(nodeID), nodeID);
  }

  public static int ClientWriteValue(String serverUrl, UA_NodeId nodeId, int value) {
    return open62541JNI.ClientAPIBase_ClientWriteValue(serverUrl, UA_NodeId.getCPtr(nodeId), nodeId, value);
  }

  public static String GetMethodOutput() {
    return open62541JNI.ClientAPIBase_GetMethodOutput();
  }

  public static String CallMethod(String serverUrl, UA_NodeId objectId, UA_NodeId methodId, String argInputString) {
    return open62541JNI.ClientAPIBase_CallMethod__SWIG_0(serverUrl, UA_NodeId.getCPtr(objectId), objectId, UA_NodeId.getCPtr(methodId), methodId, argInputString);
  }

  public static String CallMethod(String serverUrl, UA_NodeId objectId, UA_NodeId methodId, int[] argInput, int arraySize) {
    return open62541JNI.ClientAPIBase_CallMethod__SWIG_1(serverUrl, UA_NodeId.getCPtr(objectId), objectId, UA_NodeId.getCPtr(methodId), methodId, argInput, arraySize);
  }

  public void monitored_itemChanged(UA_NodeId nodeId, int value) {
    if (getClass() == ClientAPIBase.class) open62541JNI.ClientAPIBase_monitored_itemChanged(swigCPtr, this, UA_NodeId.getCPtr(nodeId), nodeId, value); else open62541JNI.ClientAPIBase_monitored_itemChangedSwigExplicitClientAPIBase(swigCPtr, this, UA_NodeId.getCPtr(nodeId), nodeId, value);
  }

  public void client_connected(ClientAPIBase jClientAPIBase, SWIGTYPE_p_UA_Client client, String serverUrl) {
    if (getClass() == ClientAPIBase.class) open62541JNI.ClientAPIBase_client_connected(swigCPtr, this, ClientAPIBase.getCPtr(jClientAPIBase), jClientAPIBase, SWIGTYPE_p_UA_Client.getCPtr(client), serverUrl); else open62541JNI.ClientAPIBase_client_connectedSwigExplicitClientAPIBase(swigCPtr, this, ClientAPIBase.getCPtr(jClientAPIBase), jClientAPIBase, SWIGTYPE_p_UA_Client.getCPtr(client), serverUrl);
  }

  public void methods_callback(UA_NodeId objectId, UA_NodeId methodId, String input, String output, ClientAPIBase jAPIBase) {
    if (getClass() == ClientAPIBase.class) open62541JNI.ClientAPIBase_methods_callback(swigCPtr, this, UA_NodeId.getCPtr(objectId), objectId, UA_NodeId.getCPtr(methodId), methodId, input, output, ClientAPIBase.getCPtr(jAPIBase), jAPIBase); else open62541JNI.ClientAPIBase_methods_callbackSwigExplicitClientAPIBase(swigCPtr, this, UA_NodeId.getCPtr(objectId), objectId, UA_NodeId.getCPtr(methodId), methodId, input, output, ClientAPIBase.getCPtr(jAPIBase), jAPIBase);
  }

  public void arrayTest(int[] outputArray) {
    open62541JNI.ClientAPIBase_arrayTest(swigCPtr, this, outputArray);
  }

  public ClientAPIBase() {
    this(open62541JNI.new_ClientAPIBase(), true);
    open62541JNI.ClientAPIBase_director_connect(this, swigCPtr, true, true);
  }

}
