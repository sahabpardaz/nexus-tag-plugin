package ir.sahab.nexus.plugin.tag.internal.validation;

import ir.sahab.nexus.plugin.tag.internal.dto.AssociatedComponent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintValidatorContext;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

@Named
@Singleton
public class ComponentExistsValidator extends ConstraintValidatorSupport<ComponentExists, AssociatedComponent> {

    private final RepositoryManager repositoryManager;

    @Inject
    public ComponentExistsValidator(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
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
        try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
            storageTx.begin();
            boolean exists =
                    storageTx.componentExists(value.getGroup(), value.getName(), value.getVersion(), repository);
            if (!exists) {
                log.info("Component {} not found.", value);
            }
            return exists;
        }
    }
}
