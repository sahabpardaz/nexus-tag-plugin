package ir.sahab.nexus.plugin.tag.internal.dto;

/**
 * Holds result of importing tag operation.
 */
public class ImportResult {
    private int total;
    private int created;

    // Used by jackson
    public ImportResult() {
    }

    public ImportResult(int total, int created) {
        this.total = total;
        this.created = created;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

}
