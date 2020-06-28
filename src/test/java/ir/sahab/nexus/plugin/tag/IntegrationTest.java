package ir.sahab.nexus.plugin.tag;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import ir.sahab.dockercomposer.DockerCompose;
import ir.sahab.dockercomposer.WaitFor;
import ir.sahab.nexus.plugin.tag.internal.dto.AssociatedComponent;
import ir.sahab.nexus.plugin.tag.internal.dto.Tag;
import ir.sahab.nexus.plugin.tag.internal.dto.TagCloneRequest;
import ir.sahab.nexus.plugin.tag.internal.dto.TagDefinition;
import java.io.ByteArrayInputStream;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonatype.nexus.rest.APIConstants;

public class IntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin123";
    private static final String REPO_MAVEN_RELEASES = "maven-releases";
    private static final String REPO_TEST_RAW = "test-raw";

    private static final String CHANGE_ID = "Change-Id";
    private static final String STATUS = "Status";

    private static final String NEXUS_VERSION = System.getProperty("nexus.version");
    private static final String SERVICE_NAME = "nexus-" + NEXUS_VERSION;
    private static final String NEXUS_URL = "http://" + SERVICE_NAME + ":8081";

    private static DockerCompose compose = DockerCompose.builder()
            .file("/nexus.yml")
            .projectName("nexus-tag-plugin-test")
            .forceRecreate()
            .forceDown()
            .afterStart(WaitFor.portOpen(SERVICE_NAME, 8081, 1_200_000))
            .build();

    private static RepositoryRule repositoryRule = new RepositoryRule(NEXUS_URL, USERNAME, PASSWORD, REPO_TEST_RAW);

    @ClassRule
    public static RuleChain ruleChain = RuleChain.outerRule(compose).around(repositoryRule);

    private static Client client;
    private static WebTarget target;

    private static AssociatedComponent component1;
    private static AssociatedComponent component2;
    private static AssociatedComponent component3;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient().register(new BasicAuthentication(USERNAME, PASSWORD));
        target = client.target(NEXUS_URL).path("/service/rest" + APIConstants.V1_API_PREFIX);
        component1 = new AssociatedComponent(REPO_MAVEN_RELEASES, randomAlphabetic(5), "comp1", "3");
        uploadMavenComponent(component1);
        component2 = new AssociatedComponent(REPO_MAVEN_RELEASES, randomAlphabetic(5), "comp2", "1.1.1");
        uploadMavenComponent(component2);
        component3 = new AssociatedComponent(REPO_TEST_RAW, "/test", "test/" + randomAlphabetic(5), null);
        uploadRawComponent(component3);
    }

    private static void uploadMavenComponent(AssociatedComponent component) {
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        output.addFormData("maven2.groupId", component.getGroup(), MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("maven2.artifactId", component.getName(), MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("maven2.version", component.getVersion(), MediaType.TEXT_PLAIN_TYPE);
        output.addFormData("maven2.asset1.extension", "jar", MediaType.TEXT_PLAIN_TYPE);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(randomAlphabetic(5).getBytes());
        output.addFormData("maven2.asset1", inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                "test-maven-artifact.jar");

        Response response = target.path("components").queryParam("repository", component.getRepository())
                .request()
                .post(Entity.entity(output, MediaType.MULTIPART_FORM_DATA_TYPE));
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        response.close();
    }

    private static void uploadRawComponent(AssociatedComponent component) {
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        output.addFormData("raw.directory", component.getGroup(), MediaType.TEXT_PLAIN_TYPE);
        String fileName = component.getName().substring(component.getGroup().length());
        output.addFormData("raw.asset1.filename", fileName, MediaType.TEXT_PLAIN_TYPE);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(randomAlphabetic(10).getBytes());
        output.addFormData("raw.asset1", inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE, component.getName());

        Response response = target.path("components").queryParam("repository", component.getRepository())
                .request()
                .post(Entity.entity(output, MediaType.MULTIPART_FORM_DATA_TYPE));
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        response.close();
    }

    @Test
    public void testCrud() {
        // Add Tags
        TagDefinition tagDef1 =
                new TagDefinition(randomAlphanumeric(5), createAttributes(), Arrays.asList(component1, component3));
        Tag createdTag1 = addTagAndAssert(tagDef1);

        TagDefinition tagDef2 = new TagDefinition(randomAlphanumeric(5), createAttributes(),
                Arrays.asList(component1, component2));
        addTagAndAssert(tagDef2);

        // Test Get by name
        Tag retrieved = target.path("tags/" + tagDef1.getName()).request().get(Tag.class);
        assertDefinitionEquals(tagDef1, retrieved);

        // Test search by attribute and component
        String component1Criterion = String.format("%s:%s:%s = %s", component1.getRepository(), component1.getGroup(),
                component1.getName(), component1.getVersion());
        String component3Criterion =
                String.format("%s:%s:%s", component3.getRepository(), component3.getGroup(), component3.getName());
        List<Tag> result = target.path("tags")
                .queryParam("attribute", CHANGE_ID + ":" + tagDef1.getAttributes().get(CHANGE_ID))
                .queryParam("attribute", STATUS + ":" + tagDef1.getAttributes().get(STATUS))
                .queryParam("associatedComponent", component1Criterion)
                .queryParam("associatedComponent", component3Criterion)
                .request()
                .get(new GenericType<List<Tag>>() {});
        assertEquals(1, result.size());
        assertDefinitionEquals(tagDef1, result.get(0));

        String component2Criterion = String.format("%s:%s:%s > %s", component2.getRepository(), component2.getGroup(),
                component2.getName(), "1.1.0");
        result = target.path("tags")
                .queryParam("attribute", CHANGE_ID + ":" + tagDef2.getAttributes().get(CHANGE_ID))
                .queryParam("attribute", STATUS + ":" + tagDef2.getAttributes().get(STATUS))
                .queryParam("associatedComponent", component1Criterion)
                .queryParam("associatedComponent", component2Criterion)
                .request()
                .get(new GenericType<List<Tag>>() {});
        assertEquals(1, result.size());
        assertDefinitionEquals(tagDef2, result.get(0));

        result = target.path("tags")
                .queryParam("attribute", CHANGE_ID + ":" + "notExists")
                .request()
                .get(new GenericType<List<Tag>>() {});
        assertTrue(result.isEmpty());

        // Update tag
        tagDef1.getAttributes().put(STATUS, "successful");
        tagDef1.setComponents(Arrays.asList(component1, component2));
        Tag putResponseTag = updateTag(tagDef1);
        assertEquals(createdTag1.getFirstCreated(), putResponseTag.getFirstCreated());
        assertFalse(new Date().before(putResponseTag.getLastUpdated()));
        assertFalse(putResponseTag.getLastUpdated().before(putResponseTag.getFirstCreated()));
        assertDefinitionEquals(tagDef1, putResponseTag);

        // Test deleting tag
        Response response = target.path("tags/" + tagDef1.getName()).request().delete();
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        response.close();
        response = target.path("tags/" + tagDef1.getName()).request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response.close();
    }


    private Map<String, String> createAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CHANGE_ID, randomAlphanumeric(20));
        attributes.put(STATUS, "failed");
        attributes.put("Commit-Id", randomAlphanumeric(20));
        return attributes;
    }

    @Test
    public void testClone() {
        TagDefinition tag = new TagDefinition(randomAlphanumeric(5), singletonMap("attr1", "val1"),
                Arrays.asList(component1, component2));
        // Add Tag
        addTagAndAssert(tag);

        // Clone Tag
        String newName = tag.getName() + "-cloned";
        TagCloneRequest request = new TagCloneRequest(tag.getName(), singletonMap("attr2", "val2"));
        Response response =
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
                new AssociatedComponent("not-existing-repo", null, "not-exist-artifact", "some-version");
        TagDefinition withNonExistingComponent =
                new TagDefinition("name", emptyMap(), singletonList(notExistingRepoComponent));
        response = addTag(withNonExistingComponent);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();
    }

    private Tag addTagAndAssert(TagDefinition tagDefinition) {
        Response response = addTag(tagDefinition);
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        Tag createdTag = response.readEntity(Tag.class);
        assertFalse(new Date().before(createdTag.getFirstCreated()));
        assertEquals(createdTag.getFirstCreated(), createdTag.getLastUpdated());
        assertDefinitionEquals(tagDefinition, createdTag);
        return createdTag;
    }

    private Response addTag(TagDefinition tag) {
        return target.path("tags").request().post(Entity.entity(tag, MediaType.APPLICATION_JSON_TYPE));
    }

    private Tag updateTag(TagDefinition tag) {
        Response response =
                target.path("tags/" + tag.getName()).request().put(Entity.entity(tag, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        return response.readEntity(Tag.class);
    }

    private void assertDefinitionEquals(TagDefinition expected, Tag actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getAttributes(), actual.getAttributes());
        assertEquals(expected.getComponents(), actual.getComponents());
    }

    @AfterClass
    public static void tearDown() {
        repositoryRule.after();
        client.close();
    }
}
