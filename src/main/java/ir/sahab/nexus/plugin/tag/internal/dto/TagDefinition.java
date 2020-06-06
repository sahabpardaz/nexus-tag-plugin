package ir.sahab.nexus.plugin.tag.internal.dto;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Holds fields used to create a new tag.
 */
public class TagDefinition {

    @NotNull(message = "Tag name can't be null.")
    @NotBlank(message = "Tag name can't be blank/empty.")
    protected String name;

    @NotNull(message = "Tag attributes can't be null.")
    protected Map<String, String> attributes;

    @NotNull(message = "Tag components can't be null.")
    @Valid
    protected List<AssociatedComponent> components;

    public TagDefinition() {
    }

    public TagDefinition(String name, Map<String, String> attributes, List<AssociatedComponent> components) {
        this.name = name;
        this.attributes = attributes;
        this.components = components;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "TagDefinition{name='" + name + "', attributes=" + attributes + '}';
    }

}
