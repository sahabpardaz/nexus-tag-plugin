package ir.sahab.nexus.plugin.tag;

import com.github.dockerjava.api.model.Bind;
import com.google.common.base.Preconditions;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

/**
 * Contains utility method for working with nexus container.
 */
public class NexusContainerUtil {
    public static final int PORT = 8081;

    /**
     * Creates test nexus container.
     * @param version version of nexus
     * @param extraBinds extra custom binds which should be added to container
     */
    public static GenericContainer<?> createTestContainer(String version, Bind... extraBinds) {
        Validate.notEmpty(version);
        String pluginVersion = System.getProperty("pluginVersion");
        Preconditions.checkState(pluginVersion != null);
        String pluginJarName = String.format("nexus-tag-plugin-%s.jar", pluginVersion);
        String pluginJarPath;
        try {
            pluginJarPath = Paths.get(NexusContainerUtil.class.getResource("/").toURI()).toFile().getPath();
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
        GenericContainer<?> container = new GenericContainer<>("sonatype/nexus3:" + version)
                .withFileSystemBind(pluginJarPath + "/../" + pluginJarName,
                        "/opt/sonatype/nexus/deploy/" + pluginJarName, BindMode.READ_ONLY)
                .withClasspathResourceMapping("/logback-overrides.xml",
                        "/opt/sonatype/sonatype-work/nexus3/etc/logback/logback-overrides.xml", BindMode.READ_ONLY)
                // Nexus >= 3.17.0 uses this file as default admin password
                .withClasspathResourceMapping("/admin.password", "/nexus-data/admin.password", BindMode.READ_ONLY)
                // Enable script execution for new nexus versions
                .withClasspathResourceMapping("/nexus.properties", "/nexus-data/etc/nexus.properties",
                        BindMode.READ_ONLY)
                .withExposedPorts(PORT)
                .waitingFor(new HttpWaitStrategy().forPort(PORT).withStartupTimeout(Duration.ofMinutes(10)))
                .withLogConsumer(outputFrame -> System.err.println(outputFrame.getUtf8String()));

        if (extraBinds.length > 0) {
            container = container.withCreateContainerCmdModifier(cmd -> {
                Bind[] existingBinds = cmd.getHostConfig().getBinds();
                Bind[] binds = Arrays.copyOf(existingBinds, existingBinds.length + existingBinds.length);
                System.arraycopy(extraBinds, 0, binds, existingBinds.length, extraBinds.length);
                cmd.getHostConfig().withBinds(binds);
            });
        }
        return container;
    }

    public static String getNexusBaseUrl(GenericContainer<?> container) {
        return "http://localhost:" + container.getMappedPort(PORT);
    }
}
