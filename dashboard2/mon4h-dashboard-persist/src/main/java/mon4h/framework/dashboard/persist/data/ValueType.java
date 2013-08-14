package mon4h.framework.dashboard.persist.data;

import mon4h.framework.dashboard.common.util.Bytes;

/**
 * User: huang_jie
 * Date: 7/3/13
 * Time: 5:40 PM
 */
public class ValueType {
    public static final byte SINGLE = (byte) 1;
    public static final byte PERCENT = (byte) 2;
    
    public static interface DataPointValue{
    	public abstract int serializeLength();
		public abstract byte[] serialize(byte offset);
	}
    
    public static DataPointValue unserialize(byte[] value){
    	return unserialize(value,0);
    }
    
    public static DataPointValue unserialize(byte[] value,int offset){
    	DataPointValue rt = null;
    	if(SINGLE == value[offset+1]){
    		long bits = Bytes.toLong(value, offset+2, 8);
    		rt = new Single(); 
    		((Single)rt).value = Double.longBitsToDouble(bits);
    	}else if(PERCENT == value[offset+1]){
    		long mbits = Bytes.toLong(value, offset+2, 8);
    		long dbits = Bytes.toLong(value, offset+10, 8);
    		rt = new Percent(); 
    		((Percent)rt).member = Double.longBitsToDouble(mbits);
    		((Percent)rt).denominator = Double.longBitsToDouble(dbits);
    	}
    	return rt;
    }
	
	public static class Single implements DataPointValue{
		public Double value;

		@Override
		public int serializeLength() {
			return 8;
		}

		@Override
		public byte[] serialize(byte offset) {
			if(value == null){
				return null;
			}
			byte[] rt = new byte[10];
			rt[0] = offset;
			rt[1] = SINGLE;
			long bits = Double.doubleToLongBits(value);
			Bytes.toBytes(rt, 2, bits,8);
			return rt;
		}
	}
	
	public static class Percent implements DataPointValue{
		public Double member;
		public Double denominator;
		@Override
		public int serializeLength() {
			return 16;
		}
		
		public Double getValue(){
			if(member == null || denominator == null || denominator.doubleValue() == 0){
				return null;
			}
			return member/denominator;
		}
		
		@Override
		public byte[] serialize(byte offset) {
			if(member == null || denominator == null){
				return null;
			}
			byte[] rt = new byte[18];
			rt[0] = offset;
			rt[1] = PERCENT;
			long mbits = Double.doubleToLongBits(member);
			long dbits = Double.doubleToLongBits(denominator);
			Bytes.toBytes(rt, 2, mbits,8);
			Bytes.toBytes(rt, 10, dbits,8);
			return rt;
		}
	}
}
