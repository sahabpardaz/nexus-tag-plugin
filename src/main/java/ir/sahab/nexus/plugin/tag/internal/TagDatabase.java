package ir.sahab.nexus.plugin.tag.internal;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseManager;

/**
 * Creates a database to store tags inside it. Note that currently we don't support restoring database using restore
 * mechanism of nexus. TODO: Add restore support
 *
 */
public class TagDatabase {
    public static final String NAME = "tag";

    private TagDatabase() {
    }

    @Named(NAME)
    @Singleton
    public static class ProviderImpl implements Provider<DatabaseInstance> {

        private final DatabaseManager databaseManager;

        @Inject
        public ProviderImpl(DatabaseManager databaseManager) {
            Objects.requireNonNull(databaseManager);
            this.databaseManager = databaseManager;
        }

        @Override
        public DatabaseInstance get() {
            return databaseManager.instance(NAME);
        }
    }
}
