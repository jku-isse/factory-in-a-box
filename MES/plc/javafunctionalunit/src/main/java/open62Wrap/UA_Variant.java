/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package open62Wrap;

public class UA_Variant {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected UA_Variant(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(UA_Variant obj) {
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
        open62541JNI.delete_UA_Variant(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setType(UA_DataType value) {
    open62541JNI.UA_Variant_type_set(swigCPtr, this, UA_DataType.getCPtr(value), value);
  }

  public UA_DataType getType() {
    long cPtr = open62541JNI.UA_Variant_type_get(swigCPtr, this);
    return (cPtr == 0) ? null : new UA_DataType(cPtr, false);
  }

  public void setStorageType(UA_VariantStorageType value) {
    open62541JNI.UA_Variant_storageType_set(swigCPtr, this, value.swigValue());
  }

  public UA_VariantStorageType getStorageType() {
    return UA_VariantStorageType.swigToEnum(open62541JNI.UA_Variant_storageType_get(swigCPtr, this));
  }

  public void setArrayLength(long value) {
    open62541JNI.UA_Variant_arrayLength_set(swigCPtr, this, value);
  }

  public long getArrayLength() {
    return open62541JNI.UA_Variant_arrayLength_get(swigCPtr, this);
  }

  public void setData(java.lang.Object value) {
   // open62541JNI.UA_Variant_data_set(swigCPtr, this, SWIGTYPE_p_void.getCPtr(value));
  }

  public java.lang.Object getData() {
  long cPtr = open62541JNI.UA_Variant_data_get(swigCPtr, this);
   Object result = null;
   if (open62541.IsVariantType_Int(this)) {
    result = open62541.void2int(cPtr);
  }
 else if (open62541.IsVariantType_String(this)) {
    result = open62541.void2str(cPtr);
  }

   return result;
}

  public void setArrayDimensionsSize(long value) {
    open62541JNI.UA_Variant_arrayDimensionsSize_set(swigCPtr, this, value);
  }

  public long getArrayDimensionsSize() {
    return open62541JNI.UA_Variant_arrayDimensionsSize_get(swigCPtr, this);
  }

  public void setArrayDimensions(SWIGTYPE_p_int value) {
    open62541JNI.UA_Variant_arrayDimensions_set(swigCPtr, this, SWIGTYPE_p_int.getCPtr(value));
  }

  public SWIGTYPE_p_int getArrayDimensions() {
    long cPtr = open62541JNI.UA_Variant_arrayDimensions_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_int(cPtr, false);
  }

  public UA_Variant() {
    this(open62541JNI.new_UA_Variant(), true);
  }

}
