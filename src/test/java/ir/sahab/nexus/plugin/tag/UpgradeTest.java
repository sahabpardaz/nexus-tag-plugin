package ir.sahab.nexus.plugin.tag;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import ir.sahab.nexus.plugin.tag.internal.dto.TagDefinition;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.rest.APIConstants;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

public class UpgradeTest {
    private static Logger logger = LoggerFactory.getLogger(UpgradeTest.class);

    private static final int PORT = 8081;
    private static final String BASE_URI_FORMAT = "http://localhost:%d/service/rest";

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin123";
    private static final String VOLUME_NAME = "upgrade_nexus_data";

    private Client client;
    private GenericContainer<?> container;

    @Before
    public void setup() {
        client = ClientBuilder.newClient().register(new BasicAuthentication(USERNAME, PASSWORD));
        recreateVolume();
    }

    @Test
    public void testUpgrade() throws Exception {
        // Start old version
        startNexus("3.15.1");

        // Add Tags
        String tagName = "test-tag";
        TagDefinition tag =
                new TagDefinition(tagName, singletonMap("attr1", "value1"), Collections.emptyList());
        Response response = tagTarget().request().post(Entity.entity(tag, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();

        // Upgrade nexus
        startNexus("3.27.0");

        // Check if tag already exists.
        response = tagTarget().path(tagName).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    private WebTarget tagTarget() {
        String baseUri = String.format(BASE_URI_FORMAT, container.getMappedPort(PORT));
        return client.target(baseUri).path(APIConstants.V1_API_PREFIX).path("tags");
    }


    private void startNexus(String version) throws Exception {
        logger.info("Starting nexus {}", version);
        if (this.container != null) {
            this.container.stop();
        }
        GenericContainer<?> container =
                NexusContainerUtil.createTestContainer(version, new Bind(VOLUME_NAME, new Volume("/nexus-data")));
        container.start();
        this.container = container;
    }

    private static void recreateVolume() {
        DockerClient client = DockerClientFactory.instance().client();
        ListVolumesResponse response = client.listVolumesCmd().exec();
        if (response.getVolumes() != null
                && response.getVolumes().stream().map(InspectVolumeResponse::getName).anyMatch(VOLUME_NAME::equals)) {
            logger.info("Removing already existing volume.");

            // API does not support removing volume with force, so we should remove owner containers first.
            List<Container> containers =
                    client.listContainersCmd().withVolumeFilter(singleton(VOLUME_NAME)).withShowAll(true).exec();
            logger.info("Removing volume owner containers: {}", containers);
            for (Container container : containers) {
                client.removeContainerCmd(container.getId()).withForce(true).exec();
            }
            client.removeVolumeCmd(VOLUME_NAME).exec();
            logger.info("Volume removed");
        }
        client.createVolumeCmd().withName(VOLUME_NAME).exec();
        logger.info("Volume (re)created.");
    }

    @After
    public void tearDown() {
        client.close();
    }
}
