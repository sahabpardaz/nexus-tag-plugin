package ir.sahab.nexus.plugin.tag.internal.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This exception is thrown whenever target tag already exists.
 */
public class TagAlreadyExistsException extends BadRequestException {

    public TagAlreadyExistsException() {
        super(Response.status(Status.BAD_REQUEST).entity(ErrorResponse.of("Tag already exists")).build());
    }
}
