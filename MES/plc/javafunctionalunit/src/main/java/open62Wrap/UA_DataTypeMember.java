/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package open62Wrap;

public class UA_DataTypeMember {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected UA_DataTypeMember(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(UA_DataTypeMember obj) {
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
        open62541JNI.delete_UA_DataTypeMember(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setMemberTypeIndex(int value) {
    open62541JNI.UA_DataTypeMember_memberTypeIndex_set(swigCPtr, this, value);
  }

  public int getMemberTypeIndex() {
    return open62541JNI.UA_DataTypeMember_memberTypeIndex_get(swigCPtr, this);
  }

  public void setPadding(short value) {
    open62541JNI.UA_DataTypeMember_padding_set(swigCPtr, this, value);
  }

  public short getPadding() {
    return open62541JNI.UA_DataTypeMember_padding_get(swigCPtr, this);
  }

  public void setNamespaceZero(boolean value) {
    open62541JNI.UA_DataTypeMember_namespaceZero_set(swigCPtr, this, value);
  }

  public boolean getNamespaceZero() {
    return open62541JNI.UA_DataTypeMember_namespaceZero_get(swigCPtr, this);
  }

  public void setIsArray(boolean value) {
    open62541JNI.UA_DataTypeMember_isArray_set(swigCPtr, this, value);
  }

  public boolean getIsArray() {
    return open62541JNI.UA_DataTypeMember_isArray_get(swigCPtr, this);
  }

  public UA_DataTypeMember() {
    this(open62541JNI.new_UA_DataTypeMember(), true);
  }

}
