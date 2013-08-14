package com.ctrip.framework.dashboard.aggregator;

import com.ctrip.framework.dashboard.aggregator.value.MetricsValue;

public class Aggregator<T extends MetricsValue>{
	private final Slot<T>[] slots;
    private final long storedSeconds;
	private volatile long lastTime;
	
	Aggregator(long storedSeconds){
        this.storedSeconds = Math.max(0, storedSeconds);
		slots = new Slot[(int) this.storedSeconds];
		for(int i=0;i< this.storedSeconds;i++){
	        slots[i]=new Slot();
	    }
	}

    public boolean isOutdated() {
        if (lastTime != 0 && System.currentTimeMillis()-lastTime > storedSeconds*1000) {
            return true;
        } else {
            return false;
        }
    }

	public void put(T value) {
        put(value, System.currentTimeMillis());
	}

	public void put(T value, long timestamp) {
		if(((System.currentTimeMillis() - timestamp)/1000)> storedSeconds){
			return;
		}
		if(timestamp>lastTime){
			lastTime = timestamp;
		}
		timestamp = timestamp/1000;
		int slotIndex = (int)(timestamp% storedSeconds);
		if(timestamp-slots[slotIndex].timestamp>= storedSeconds){
			slots[slotIndex].init(timestamp);
		}else if(slots[slotIndex].timestamp == 0){
			slots[slotIndex].init(timestamp);
		}
		slots[slotIndex].merge(value);
	}

	public T get(long startTimestamp,long endTimestamp) {
        T result = null;
        long start = startTimestamp/1000;
        long end = endTimestamp/1000;
        for(int i=0;i<slots.length;i++){
            if(slots[i].timestamp < start  ||  slots[i].timestamp >= end){
                continue;
            }
            if (slots[i].value == null) {
                continue;
            }
            if (result == null) {
                result = (T)slots[i].value.getCopy();
            } else {
                // merge
                result = (T)result.merge(slots[i].value);
            }
        }
        return result;
	}
	
    int getCount(long startTimestamp,long endTimestamp){
		int rt = 0;
		if(slots != null){
			long start = startTimestamp/1000;
			long end = endTimestamp/1000;
			for(int i=0;i<slots.length;i++){
				if(slots[i].timestamp>=start && slots[i].timestamp<end && slots[i].value != null){
					rt++;
				}
			}
		}
		return rt;
	}
	
	private static class Slot<T extends MetricsValue>{
		volatile long timestamp;//second
        volatile T value;

		public void init(long timestamp){
			this.timestamp = timestamp;
		}

        // unsafe, more protection?
        public void merge(T newValue) {
            if (newValue == null) {
                return;
            }
            if (value == null) {
                value = newValue;
            } else {
                value = (T)value.merge(newValue);
            }
        }

        public T getValue() {
            return value;
        }
	}
	
}
