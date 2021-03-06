/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package open62Wrap;

public class UA_Argument {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected UA_Argument(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(UA_Argument obj) {
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
        open62541JNI.delete_UA_Argument(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setName(String value) {
    open62541JNI.UA_Argument_name_set(swigCPtr, this, value);
  }

  public String getName() {
    return open62541JNI.UA_Argument_name_get(swigCPtr, this);
  }

  public void setDataType(UA_NodeId value) {
    open62541JNI.UA_Argument_dataType_set(swigCPtr, this, UA_NodeId.getCPtr(value), value);
  }

  public UA_NodeId getDataType() {
    long cPtr = open62541JNI.UA_Argument_dataType_get(swigCPtr, this);
    return (cPtr == 0) ? null : new UA_NodeId(cPtr, false);
  }

  public void setValueRank(int value) {
    open62541JNI.UA_Argument_valueRank_set(swigCPtr, this, value);
  }

  public int getValueRank() {
    return open62541JNI.UA_Argument_valueRank_get(swigCPtr, this);
  }

  public void setArrayDimensionsSize(long value) {
    open62541JNI.UA_Argument_arrayDimensionsSize_set(swigCPtr, this, value);
  }

  public long getArrayDimensionsSize() {
    return open62541JNI.UA_Argument_arrayDimensionsSize_get(swigCPtr, this);
  }

  public void setArrayDimensions(SWIGTYPE_p_int value) {
    open62541JNI.UA_Argument_arrayDimensions_set(swigCPtr, this, SWIGTYPE_p_int.getCPtr(value));
  }

  public SWIGTYPE_p_int getArrayDimensions() {
    long cPtr = open62541JNI.UA_Argument_arrayDimensions_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_int(cPtr, false);
  }

  public void setDescription(UA_LocalizedText value) {
    open62541JNI.UA_Argument_description_set(swigCPtr, this, UA_LocalizedText.getCPtr(value), value);
  }

  public UA_LocalizedText getDescription() {
    long cPtr = open62541JNI.UA_Argument_description_get(swigCPtr, this);
    return (cPtr == 0) ? null : new UA_LocalizedText(cPtr, false);
  }

  public UA_Argument() {
    this(open62541JNI.new_UA_Argument(), true);
  }

}
