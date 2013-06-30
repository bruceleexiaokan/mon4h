package mon4h.common.domain.models.sub;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4);
    private int code;

    private LogLevel(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static LogLevel fromCode(int code) {
        switch (code) {
            case 0:
                return DEBUG;
            case 1:
                return INFO;
            case 2:
                return WARN;
            case 3:
                return ERROR;
            case 4:
                return FATAL;
            default:
                return null;
        }
    }
}
