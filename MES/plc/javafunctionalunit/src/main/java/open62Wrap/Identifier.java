/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package open62Wrap;

public class Identifier {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected Identifier(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Identifier obj) {
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
        open62541JNI.delete_Identifier(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setNumeric(int value) {
    open62541JNI.Identifier_numeric_set(swigCPtr, this, value);
  }

  public int getNumeric() {
    return open62541JNI.Identifier_numeric_get(swigCPtr, this);
  }

  public void setString(String value) {
    open62541JNI.Identifier_string_set(swigCPtr, this, value);
  }

  public String getString() {
    return open62541JNI.Identifier_string_get(swigCPtr, this);
  }

  public void setGuid(UA_Guid value) {
    open62541JNI.Identifier_guid_set(swigCPtr, this, UA_Guid.getCPtr(value), value);
  }

  public UA_Guid getGuid() {
    long cPtr = open62541JNI.Identifier_guid_get(swigCPtr, this);
    return (cPtr == 0) ? null : new UA_Guid(cPtr, false);
  }

  public void setByteString(String value) {
    open62541JNI.Identifier_byteString_set(swigCPtr, this, value);
  }

  public String getByteString() {
    return open62541JNI.Identifier_byteString_get(swigCPtr, this);
  }

  public Identifier() {
    this(open62541JNI.new_Identifier(), true);
  }

}
