package ir.sahab.nexus.plugin.tag.internal.dto;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.ImmutableMap;
import ir.sahab.dockercomposer.DockerCompose;
import ir.sahab.dockercomposer.WaitFor;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import org.apache.commons.codec.binary.Base64;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonatype.nexus.rest.APIConstants;

public class IntegrationTest {

    private static final String CHANGE_ID = "Change-Id";
    private static final String STATUS = "Status";
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "admin123";
    private static final String REPO_MAVEN_RELEASES = "maven-releases";
    private static final String REPO_NUGET_HOSTED = "nuget-hosted";

    @ClassRule
    public static DockerCompose compose = DockerCompose.builder()
            .file("/nexus.yml")
            .projectName("nexus-tag-plugin-test")
            .forceRecreate()
            .afterStart(WaitFor.portOpen("nexus", 8081, 1_200_000))
            .build();

    private Client client;
    private WebTarget target;

    private AssociatedComponent component1;
    private AssociatedComponent component2;

    @Before
    public void setup() {
        client = ClientBuilder.newClient();
        target = client.target("http://nexus:8081/service/rest" + APIConstants.V1_API_PREFIX);
        component1 = new AssociatedComponent(REPO_MAVEN_RELEASES, "gr1", "comp1", randomAlphanumeric(5));
        uploadMavenComponent(component1);
        component2 = new AssociatedComponent(REPO_MAVEN_RELEASES, "gr2", "comp2", randomAlphanumeric(5));
        uploadMavenComponent(component2);
        //TODO: Test non-maven artifacts which does not have group/version
    }


    @After
    public void tearDown() {
        client.close();
    }

    private void uploadMavenComponent(AssociatedComponent component) {
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        output.addFormData("maven2.groupId", component.getGroup(), MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("maven2.artifactId", component.getName(), MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("maven2.version", component.getVersion(), MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("maven2.asset1.extension", "jar", MediaType.TEXT_PLAIN_TYPE);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(randomAlphanumeric(100).getBytes());
        output.addFormData("maven2.asset1", inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                "test-maven-artifact.jar");

        Response response = target.path("components").queryParam("repository", component.getRepository())
                .request()
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .post(Entity.entity(output, MediaType.MULTIPART_FORM_DATA_TYPE));
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
    }

    /**
     * @return authorization header of basic authentication
     */
    private static String authorizationHeader() {
        String auth = USERNAME + ":" + PASSWORD;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }

    @Test
    public void testCrud() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CHANGE_ID, randomAlphanumeric(20));
        attributes.put(STATUS, "failed");
        attributes.put("Commit-Id", randomAlphanumeric(20));

        TagDefinition tag = new TagDefinition(randomAlphanumeric(5), attributes, Arrays.asList(component1));

        // Add Tag
        Response response = addTag(tag);
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        Tag postResponseTag = response.readEntity(Tag.class);
        assertFalse(new Date().before(postResponseTag.getFirstCreated()));
        assertEquals(postResponseTag.getFirstCreated(), postResponseTag.getLastUpdated());
        assertDefinitionEquals(tag, postResponseTag);

        // Test Get by name
        Tag retrieved = target.path("tags/" + tag.getName()).request().get(Tag.class);
        assertDefinitionEquals(tag, retrieved);

        // Test search by attribute
        List<Tag> result = target.path("tags")
                .queryParam("attributes", CHANGE_ID + "=" + tag.getAttributes().get(CHANGE_ID))
                .queryParam("attributes", STATUS + "=" + tag.getAttributes().get(STATUS))
                .request()
                .get(new GenericType<List<Tag>>() {});
        assertEquals(1, result.size());
        assertDefinitionEquals(tag, result.get(0));

        // Update tag
        tag.getAttributes().put(STATUS, "successful");
        tag.setComponents(Arrays.asList(component1, component2));
        response = updateTag(tag);
        Tag putResponseTag = response.readEntity(Tag.class);
        assertEquals(postResponseTag.getFirstCreated(), putResponseTag.getFirstCreated());
        assertFalse(new Date().before(putResponseTag.getLastUpdated()));
        assertDefinitionEquals(tag, putResponseTag);

        // Test deleting tag
        response = target.path("tags/" + tag.getName()).request().delete();
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        response = target.path("tags/" + tag.getName()).request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testClone() {
        TagDefinition tag = new TagDefinition(randomAlphanumeric(5), singletonMap("attr1", "val1"),
                Arrays.asList(component1, component2));
        // Add Tag
        Response response = addTag(tag);
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        response.close();

        // Clone Tag
        String newName = tag.getName() + "-cloned";
        TagCloneRequest request = new TagCloneRequest(tag.getName(), singletonMap("attr2", "val2"));
        response =
                target.path("tags/" + newName).request().post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        Tag cloned = response.readEntity(Tag.class);
        assertEquals(newName, cloned.getName());
        assertEquals(ImmutableMap.of("attr1", "val1", "attr2", "val2"), cloned.getAttributes());
        assertEquals(tag.getComponents(), cloned.getComponents());

        // Check tag is actually created
        Tag retrieved = target.path("tags/" + newName).request().get(Tag.class);
        assertDefinitionEquals(cloned, retrieved);
    }

    @Test
    public void testValidation() {
        TagDefinition nullName = new TagDefinition(null, emptyMap(), emptyList());
        Response response = addTag(nullName);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();

        TagDefinition emptyName = new TagDefinition("", emptyMap(), emptyList());
        response = addTag(emptyName);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();

        TagDefinition nullAttribute = new TagDefinition("name", null, emptyList());
        response = addTag(nullAttribute);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();

        AssociatedComponent notExistingRepoComponent =
                new AssociatedComponent("not-existing-repo", "", "not-exist-artifact", "some-version");
        TagDefinition withNonExistingComponent =
                new TagDefinition("name", emptyMap(), singletonList(notExistingRepoComponent));
        response = addTag(withNonExistingComponent);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();

        AssociatedComponent notExistingComponent =
                new AssociatedComponent(REPO_MAVEN_RELEASES, "", "not-exist-artifact", "some-version");
        withNonExistingComponent = new TagDefinition("name", emptyMap(), singletonList(notExistingComponent));
        response = addTag(withNonExistingComponent);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();
    }

    private Response addTag(TagDefinition tag) {
        return target.path("tags").request().post(Entity.entity(tag, MediaType.APPLICATION_JSON_TYPE));
    }

    private Response updateTag(TagDefinition tag) {
        return target.path("tags/" + tag.getName()).request().put(Entity.entity(tag, MediaType.APPLICATION_JSON_TYPE));
    }

    private void assertDefinitionEquals(TagDefinition expected, Tag actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getAttributes(), actual.getAttributes());
        assertEquals(expected.getComponents(), actual.getComponents());
    }
}
