package mon4h.common.domain.models.sub;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"key", "value"})
public class Tag implements Serializable {
	private static final long serialVersionUID = 316596124300380499L;
	private String key;
    private String value;

    public Tag() {}

    public Tag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
    
	@Override
	public int hashCode() {
		return key.hashCode() ^ value.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		boolean result = o != null && o.getClass().equals(Tag.class);
		Tag tag = (Tag)o;
		result = result && (key == null ? tag.key == null : key.equals(tag.key));
		result = result && (value == null ? tag.value == null : value.equals(tag.value));
		return result;
	}
}
