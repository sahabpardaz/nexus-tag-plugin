package ir.sahab.nexus.plugin.tag;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.repository.rest.api.RepositoryXO;
import org.sonatype.nexus.repository.storage.WritePolicy;
import org.sonatype.nexus.rest.APIConstants;
import org.sonatype.nexus.script.ScriptXO;

/**
 * Creates a repository inside nexus repository manager if it does not already exists. This rule does 'not' remove
 * created repository after test finishes.
 */
public class RepositoryRule extends ExternalResource {

    private static final Logger log = LoggerFactory.getLogger(RepositoryRule.class);

    private static final String SCRIPT_API_PATH = "script";
    private static final String SCRIPT_NAME = "create_test_raw_repo";

    private final Client client;
    private final WebTarget apiTarget;
    private final String repositoryName;

    /**
     * @param nexusUrl URL of nexus repository
     * @param username username to use for API authentication
     * @param password password to use for API authentication
     * @param repositoryName name of raw repository to create
     */
    public RepositoryRule(String nexusUrl, String username, String password, String repositoryName) {
        this.repositoryName = repositoryName;
        client = ClientBuilder.newClient()
                .register(ObjectMapperContextResolver.class)
                .register(new BasicAuthentication(username, password));
        apiTarget = client.target(nexusUrl).path("/service/rest" + APIConstants.V1_API_PREFIX);
    }

    @Override
    protected void before() {
        if (repositoryExist()) {
            log.info("Repository {} already exists.", repositoryName);
            return;
        }
        uploadScriptIfNotExists();
        createRepository();
    }

    private boolean repositoryExist() {
        List<RepositoryXO> repositories = apiTarget.path("repositories")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<RepositoryXO>>() {});
        return repositories.stream().anyMatch(repositoryXO -> repositoryName.equals(repositoryXO.getName()));
    }

    private void uploadScriptIfNotExists() {
        Response response = apiTarget.path(SCRIPT_API_PATH).path(SCRIPT_NAME).request().get();
        response.close();
        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            Entity<ScriptXO> entity =
                    Entity.entity(scriptRequest(), MediaType.APPLICATION_JSON);
            Response postResponse =
                    apiTarget.path(SCRIPT_API_PATH).request(MediaType.APPLICATION_JSON_TYPE).post(entity);
            if (postResponse.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new AssertionError("Uploading script failed, response: " + postResponse.readEntity(String.class));
            }
            postResponse.close();
            log.info("{} script uploaded.", SCRIPT_NAME);
        } else {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new AssertionError("Getting scripts failed, response: " + response.readEntity(String.class));
            }
            log.info("{} script already exists.", SCRIPT_NAME);
        }
    }

    private ScriptXO scriptRequest() {
        String blobName = "default";
        boolean strictContentTypeValidation = false;
        String content = String.format("repository.createRawHosted(args, '%s', %b, %s.%s)", blobName,
                strictContentTypeValidation, WritePolicy.class.getName(), WritePolicy.ALLOW.name());
        return new ScriptXO(SCRIPT_NAME, content, "groovy");
    }


    private void createRepository() {
        Response runResponse = apiTarget.path(SCRIPT_API_PATH)
                .path(SCRIPT_NAME)
                .path("run")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(repositoryName, MediaType.TEXT_PLAIN));
        if (runResponse.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
            throw new AssertionError("Running script failed, response: " + runResponse.readEntity(String.class));
        }
        log.info("Repository {} created.", repositoryName);
        runResponse.close();
    }

    @Override
    protected void after() {
        client.close();
    }

    @Provider
    public static class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

        @Override
        public ObjectMapper getContext(Class<?> type) {
            ObjectMapper mapper = new ObjectMapper();
            // RepositoryXO has new properties in newer versions of nexus
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper;
        }
    }
}
