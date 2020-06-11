package ir.sahab.nexus.plugin.tag.internal.validation;

import ir.sahab.nexus.plugin.tag.internal.dto.AssociatedComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintValidatorContext;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentStore;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

@Named
@Singleton
public class ComponentExistsValidator extends ConstraintValidatorSupport<ComponentExists, AssociatedComponent> {

    private final RepositoryManager repositoryManager;
    private final ComponentStore componentStore;

    @Inject
    public ComponentExistsValidator(RepositoryManager repositoryManager, ComponentStore componentStore) {
        this.repositoryManager = repositoryManager;
        this.componentStore = componentStore;
    }

    @Override
    public boolean isValid(AssociatedComponent value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        Repository repository = repositoryManager.get(value.getRepository());
        if (repository == null) {
            log.info("Component {} is invalid as repository does not exists.", value);
            return false;
        }
        Map<String, String> versionAttribute = new HashMap<>();
        if (value.getVersion() != null && !value.getVersion().isEmpty()) {
            versionAttribute.put("version", value.getVersion());
        }
        List<Component> founds = componentStore.getAllMatchingComponents(repository, value.getGroup(), value.getName(),
                versionAttribute);
        log.debug("Components found for {}: {}", value, founds);
        return !founds.isEmpty();
    }
}
