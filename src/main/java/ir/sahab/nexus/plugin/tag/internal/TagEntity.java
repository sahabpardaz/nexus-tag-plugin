package ir.sahab.nexus.plugin.tag.internal;

import ir.sahab.nexus.plugin.tag.internal.dto.AssociatedComponent;
import ir.sahab.nexus.plugin.tag.internal.dto.Tag;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonatype.nexus.common.entity.AbstractEntity;


/**
 * Entity class used to persist tag instances.
 */
class TagEntity extends AbstractEntity {

    private String name;

    private Date firstCreated;

    private Date lastUpdated;

    private Map<String, String> attributes;

    private List<AssociatedComponent> components;

    TagEntity() {
    }

    TagEntity(TagEntity entity) {
        name = entity.name;
        attributes = new HashMap<>(entity.attributes);
        components = entity.components.stream().map(AssociatedComponent::new).collect(Collectors.toList());
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFirstCreated(Date firstCreated) {
        this.firstCreated = firstCreated;
    }

    public Date getFirstCreated() {
        return firstCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<AssociatedComponent> getComponents() {
        return components;
    }

    public void setComponents(List<AssociatedComponent> components) {
        this.components = components;
    }

    public Tag toDto() {
        return new Tag(name, attributes, components, firstCreated, lastUpdated);
    }

    /**
     * @return true if criteria matches with associated components of this tag, otherwise false.
     */
    public boolean matches(Collection<ComponentSearchCriterion> componentVersionCriteria) {
        for (ComponentSearchCriterion criterion : componentVersionCriteria) {
            if (components.stream().noneMatch(criterion::matches)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Tag{name='" + name + ", firstCreated='" + firstCreated + ", lastUpdated='" + lastUpdated
                + "', attributes=" + attributes + ", metadata=" + getEntityMetadata() + '}';
    }
}
