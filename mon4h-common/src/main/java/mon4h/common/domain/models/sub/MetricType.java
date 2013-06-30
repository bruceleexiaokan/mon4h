package mon4h.common.domain.models.sub;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum MetricType {
    DOUBLE_TYPE(0),
    LONG_TYPE(1);

    private final int value;

    private MetricType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MetricType valueOf(int value) {
        switch (value) {
            case 0:
                return DOUBLE_TYPE;
            case 1:
                return LONG_TYPE;
            default:
                return null;
        }
    }

}
