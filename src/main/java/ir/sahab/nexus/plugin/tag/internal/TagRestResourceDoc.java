package ir.sahab.nexus.plugin.tag.internal;

import ir.sahab.nexus.plugin.tag.internal.dto.Tag;
import ir.sahab.nexus.plugin.tag.internal.dto.TagCloneRequest;
import ir.sahab.nexus.plugin.tag.internal.dto.TagDefinition;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value = "tag")
public interface TagRestResourceDoc {

    @GET
    @ApiOperation("Get a single tag by name")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Tag does not exists")
    })
    Tag getByName(@ApiParam(value = "name of tag to retrieve", required = true) String name);

    @GET
    @ApiOperation("List tags, results may be filtered by optional attributes")
    List<Tag> list(
            @ApiParam("List of attribute key value pairs to search in format key=value") List<String> attributes);

    @POST
    @ApiOperation("Add a new tag")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request")
    })
    Tag add(TagDefinition definition);

    @PUT
    @ApiOperation("Add a new tag or updates existing one")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid request.")
    })
    Tag addOrUpdate(TagDefinition definition,
            @ApiParam(value = "Name of tag to create or update", required = true) String name);

    @DELETE
    @ApiOperation("Deletes existing tag by name")
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "Tag does not exists")
    })
    void delete(@ApiParam(value = "Name of tag to delete", required = true) String name);

    @POST
    @ApiOperation("Clone an existing tag")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "Tag does not exists")
    })
    Tag clone(@ApiParam(value = "Name of tag to clone", required = true) String name, TagCloneRequest request);
}