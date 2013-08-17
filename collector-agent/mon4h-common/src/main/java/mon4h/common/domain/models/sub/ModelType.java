package mon4h.common.domain.models.sub;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum ModelType {
	
    LOGS("logs"),
    METRICS("metrics");

    private final String type;
    
    private ModelType(String type) {
    	this.type = type;
    }
    
    public final String getType() {
    	return type;
    }
    
    public static final ModelType fromValue(String type) {
    	if ("logs".equals(type)) {
    		return LOGS;
    	} else if ("metrics".equals(type)) {
    		return METRICS;
    	}
    	return null;
    }
}
