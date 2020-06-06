package ir.sahab.nexus.plugin.tag.internal.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents a tag which can be created using REST API. This class is meant to
 * be serialized by Jackson and to be used in REST API.
 */
public class Tag extends TagDefinition {

    private Date firstCreated;

    private Date lastUpdated;

    public Tag() {
    }

    public Tag(String name, Map<String, String> attributes, List<AssociatedComponent> components, Date firstCreated,
            Date lastUpdated) {
        super(name, attributes, components);
        this.firstCreated = firstCreated;
        this.lastUpdated = lastUpdated;
    }

    public Date getFirstCreated() {
        return firstCreated;
    }

    public void setFirstCreated(Date firstCreated) {
        this.firstCreated = firstCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Tag{name='" + name + ", firstCreated='" + firstCreated + ", lastUpdated='" + lastUpdated
                + "', attributes=" + attributes + "', components=" + components + '}';
    }


}
