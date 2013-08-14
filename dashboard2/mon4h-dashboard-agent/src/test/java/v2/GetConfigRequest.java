/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package v2;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;

import java.util.*;

public class GetConfigRequest implements org.apache.thrift.TBase<GetConfigRequest, GetConfigRequest._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("GetConfigRequest");

  private static final org.apache.thrift.protocol.TField APP_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("appId", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField HOST_IP_FIELD_DESC = new org.apache.thrift.protocol.TField("hostIp", org.apache.thrift.protocol.TType.STRING, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new GetConfigRequestStandardSchemeFactory());
    schemes.put(TupleScheme.class, new GetConfigRequestTupleSchemeFactory());
  }

  private String appId; // required
  private String hostIp; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    APP_ID((short)1, "appId"),
    HOST_IP((short)2, "hostIp");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // APP_ID
          return APP_ID;
        case 2: // HOST_IP
          return HOST_IP;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.APP_ID, new org.apache.thrift.meta_data.FieldMetaData("appId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.HOST_IP, new org.apache.thrift.meta_data.FieldMetaData("hostIp", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(GetConfigRequest.class, metaDataMap);
  }

  public GetConfigRequest() {
  }

  public GetConfigRequest(
    String appId,
    String hostIp)
  {
    this();
    this.appId = appId;
    this.hostIp = hostIp;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public GetConfigRequest(GetConfigRequest other) {
    if (other.isSetAppId()) {
      this.appId = other.appId;
    }
    if (other.isSetHostIp()) {
      this.hostIp = other.hostIp;
    }
  }

  public GetConfigRequest deepCopy() {
    return new GetConfigRequest(this);
  }

  @Override
  public void clear() {
    this.appId = null;
    this.hostIp = null;
  }

  public String getAppId() {
    return this.appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void unsetAppId() {
    this.appId = null;
  }

  /** Returns true if field appId is set (has been assigned a value) and false otherwise */
  public boolean isSetAppId() {
    return this.appId != null;
  }

  public void setAppIdIsSet(boolean value) {
    if (!value) {
      this.appId = null;
    }
  }

  public String getHostIp() {
    return this.hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public void unsetHostIp() {
    this.hostIp = null;
  }

  /** Returns true if field hostIp is set (has been assigned a value) and false otherwise */
  public boolean isSetHostIp() {
    return this.hostIp != null;
  }

  public void setHostIpIsSet(boolean value) {
    if (!value) {
      this.hostIp = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case APP_ID:
      if (value == null) {
        unsetAppId();
      } else {
        setAppId((String)value);
      }
      break;

    case HOST_IP:
      if (value == null) {
        unsetHostIp();
      } else {
        setHostIp((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case APP_ID:
      return getAppId();

    case HOST_IP:
      return getHostIp();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case APP_ID:
      return isSetAppId();
    case HOST_IP:
      return isSetHostIp();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof GetConfigRequest)
      return this.equals((GetConfigRequest)that);
    return false;
  }

  public boolean equals(GetConfigRequest that) {
    if (that == null)
      return false;

    boolean this_present_appId = true && this.isSetAppId();
    boolean that_present_appId = true && that.isSetAppId();
    if (this_present_appId || that_present_appId) {
      if (!(this_present_appId && that_present_appId))
        return false;
      if (!this.appId.equals(that.appId))
        return false;
    }

    boolean this_present_hostIp = true && this.isSetHostIp();
    boolean that_present_hostIp = true && that.isSetHostIp();
    if (this_present_hostIp || that_present_hostIp) {
      if (!(this_present_hostIp && that_present_hostIp))
        return false;
      if (!this.hostIp.equals(that.hostIp))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_appId = true && (isSetAppId());
    builder.append(present_appId);
    if (present_appId)
      builder.append(appId);

    boolean present_hostIp = true && (isSetHostIp());
    builder.append(present_hostIp);
    if (present_hostIp)
      builder.append(hostIp);

    return builder.toHashCode();
  }

  public int compareTo(GetConfigRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    GetConfigRequest typedOther = (GetConfigRequest)other;

    lastComparison = Boolean.valueOf(isSetAppId()).compareTo(typedOther.isSetAppId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAppId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.appId, typedOther.appId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetHostIp()).compareTo(typedOther.isSetHostIp());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetHostIp()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.hostIp, typedOther.hostIp);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GetConfigRequest(");
    boolean first = true;

    sb.append("appId:");
    if (this.appId == null) {
      sb.append("null");
    } else {
      sb.append(this.appId);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("hostIp:");
    if (this.hostIp == null) {
      sb.append("null");
    } else {
      sb.append(this.hostIp);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class GetConfigRequestStandardSchemeFactory implements SchemeFactory {
    public GetConfigRequestStandardScheme getScheme() {
      return new GetConfigRequestStandardScheme();
    }
  }

  private static class GetConfigRequestStandardScheme extends StandardScheme<GetConfigRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, GetConfigRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // APP_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.appId = iprot.readString();
              struct.setAppIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // HOST_IP
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.hostIp = iprot.readString();
              struct.setHostIpIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, GetConfigRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.appId != null) {
        oprot.writeFieldBegin(APP_ID_FIELD_DESC);
        oprot.writeString(struct.appId);
        oprot.writeFieldEnd();
      }
      if (struct.hostIp != null) {
        oprot.writeFieldBegin(HOST_IP_FIELD_DESC);
        oprot.writeString(struct.hostIp);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class GetConfigRequestTupleSchemeFactory implements SchemeFactory {
    public GetConfigRequestTupleScheme getScheme() {
      return new GetConfigRequestTupleScheme();
    }
  }

  private static class GetConfigRequestTupleScheme extends TupleScheme<GetConfigRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, GetConfigRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetAppId()) {
        optionals.set(0);
      }
      if (struct.isSetHostIp()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetAppId()) {
        oprot.writeString(struct.appId);
      }
      if (struct.isSetHostIp()) {
        oprot.writeString(struct.hostIp);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, GetConfigRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.appId = iprot.readString();
        struct.setAppIdIsSet(true);
      }
      if (incoming.get(1)) {
        struct.hostIp = iprot.readString();
        struct.setHostIpIsSet(true);
      }
    }
  }

}

