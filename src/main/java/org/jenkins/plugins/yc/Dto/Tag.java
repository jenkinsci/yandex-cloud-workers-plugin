package org.jenkins.plugins.yc.Dto;
import java.io.Serializable;

public class Tag implements Serializable, Cloneable {
    private String key;
    private String value;

    public Tag() {
    }

    public Tag(String key) {
        this.setKey(key);
    }

    public Tag(String key, String value) {
        this.setKey(key);
        this.setValue(value);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public Tag withKey(String key) {
        this.setKey(key);
        return this;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public Tag withValue(String value) {
        this.setValue(value);
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (this.getKey() != null) {
            sb.append("Key: ").append(this.getKey()).append(",");
        }

        if (this.getValue() != null) {
            sb.append("Value: ").append(this.getValue());
        }

        sb.append("}");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Tag)) {
            return false;
        } else {
            Tag other = (Tag)obj;
            if (other.getKey() == null ^ this.getKey() == null) {
                return false;
            } else if (other.getKey() != null && !other.getKey().equals(this.getKey())) {
                return false;
            } else if (other.getValue() == null ^ this.getValue() == null) {
                return false;
            } else {
                return other.getValue() == null || other.getValue().equals(this.getValue());
            }
        }
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + (this.getKey() == null ? 0 : this.getKey().hashCode());
        hashCode = 31 * hashCode + (this.getValue() == null ? 0 : this.getValue().hashCode());
        return hashCode;
    }

    public Tag clone() {
        try {
            return (Tag)super.clone();
        } catch (CloneNotSupportedException var2) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() even though we're Cloneable!", var2);
        }
    }
}
