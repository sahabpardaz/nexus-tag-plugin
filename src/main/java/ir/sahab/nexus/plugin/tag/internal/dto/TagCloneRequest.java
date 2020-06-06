package ir.sahab.nexus.plugin.tag.internal.dto;

import java.util.Map;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Holds fields used to create a new tag.
 */
public class TagCloneRequest {

    @NotNull(message = "Source tag name can't be null.")
    @NotBlank(message = "Source tag name can't be blank/empty.")
    protected String sourceName;

    @NotNull(message = "Appending attributes can't be null.")
    protected Map<String, String> appendingAttributes;

    public TagCloneRequest() {
    }

    public TagCloneRequest(String sourceName, Map<String, String> appendingAttributes) {
        this.sourceName = sourceName;
        this.appendingAttributes = appendingAttributes;
    }

    /**
     * @return name of source tag to clone
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * @param sourceName name of source to clone
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Map<String, String> getAppendingAttributes() {
        return appendingAttributes;
    }

    public void setAppendingAttributes(Map<String, String> appendingAttributes) {
        this.appendingAttributes = appendingAttributes;
    }


    @Override
    public String toString() {
        return "CloneTagRequest{name='" + sourceName + "', attributes=" + appendingAttributes + '}';
    }

}
