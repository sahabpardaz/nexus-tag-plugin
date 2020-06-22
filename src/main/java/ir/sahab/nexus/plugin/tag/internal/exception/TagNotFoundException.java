package ir.sahab.nexus.plugin.tag.internal.exception;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This exception is thrown whenever target tag does not exists.
 */
public class TagNotFoundException extends NotFoundException {

    public TagNotFoundException() {
        super(Response.status(Status.NOT_FOUND).entity(ErrorResponse.of("Tag not found")).build());
    }
}
