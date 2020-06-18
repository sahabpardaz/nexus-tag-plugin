package ir.sahab.nexus.plugin.tag.internal.dto;

import ir.sahab.nexus.plugin.tag.internal.validation.ComponentExists;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Represent a component (artifact) which a tag can be associated with.
 */
@ComponentExists
public class AssociatedComponent {

    @NotNull(message = "Components repository can't be null.")
    @NotBlank(message = "Repository can't be blank.")
    private String repository;

    private String group;

    @NotNull(message = "Components name can't be null.")
    @NotBlank
    private String name;

    private String version;

    public AssociatedComponent() {
        // Used by Jackson
    }

    public AssociatedComponent(AssociatedComponent component) {
        this(component.repository, component.group, component.name, component.version);
    }

    public AssociatedComponent(String repository, String group, String name, String version) {
        this.repository = repository;
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AssociatedComponent that = (AssociatedComponent) o;
        return repository.equals(that.repository) &&
               group.equals(that.group) &&
               name.equals(that.name) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, group, name, version);
    }

    @Override
    public String toString() {
        return repository + ":" + group + ":" + name + ":" + version;
    }
}
