package ir.sahab.nexus.plugin.tag.internal;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import ir.sahab.nexus.plugin.tag.internal.dto.AssociatedComponent;

/**
 * Provides persisting/reading tags from database.
 */
public class TagEntityAdapter extends IterableEntityAdapter<TagEntity> {

    private static final String TYPE_NAME = "tag";

    private static final String DB_CLASS = new OClassNameBuilder().type(TYPE_NAME).build();

    private static final String NAME_FIELD = "name";
    private static final String ATTRIBUTES_FIELD = "attributes";
    private static final String COMPONENTS_FIELD = "components";
    private static final String FIRST_CREATED_FIELD = "firstCreated";
    private static final String LAST_UPDATED_FIELD = "lastUpdated";

    private static final String COMPONENT_REPOSITORY_FIELD = "repository";
    private static final String COMPONENT_GROUP_FIELD = "group";
    private static final String COMPONENT_NAME_FIELD = "name";
    private static final String COMPONENT_VERSION_FIELD = "version";

    private static final String NAME_INDEX = new OIndexNameBuilder().type(DB_CLASS).property("name").build();

    public TagEntityAdapter() {
        super(TYPE_NAME);
    }

    @Override
    protected void defineType(OClass type) {
        type.createProperty(NAME_FIELD, OType.STRING).setMandatory(true).setNotNull(true);
        type.createProperty(FIRST_CREATED_FIELD, OType.DATETIME).setMandatory(true).setNotNull(true);
        type.createProperty(LAST_UPDATED_FIELD, OType.DATETIME).setMandatory(true).setNotNull(true);
        type.createProperty(ATTRIBUTES_FIELD, OType.EMBEDDEDMAP).setMandatory(true).setNotNull(true);
        type.createProperty(COMPONENTS_FIELD, OType.EMBEDDEDLIST).setMandatory(true).setNotNull(true);

        type.createIndex(NAME_INDEX, INDEX_TYPE.UNIQUE, NAME_FIELD);
        type.createIndex(LAST_UPDATED_FIELD, INDEX_TYPE.NOTUNIQUE, LAST_UPDATED_FIELD);
    }

    @Override
    protected TagEntity newEntity() {
        return new TagEntity();
    }

    @Override
    protected void readFields(ODocument oDocument, TagEntity tag) {
        tag.setName(oDocument.field(NAME_FIELD));
        tag.setFirstCreated(oDocument.field(FIRST_CREATED_FIELD));
        tag.setLastUpdated(oDocument.field(LAST_UPDATED_FIELD));
        tag.setAttributes(oDocument.field(ATTRIBUTES_FIELD));
        tag.setComponents(oDocument.field(COMPONENTS_FIELD));

        List<ODocument> componentDocuments = oDocument.field(COMPONENTS_FIELD);
        List<AssociatedComponent> components = componentDocuments.stream()
                .map(TagEntityAdapter::toComponent)
                .collect(Collectors.toList());
        tag.setComponents(components);
    }

    @Override
    protected void writeFields(ODocument oDocument, TagEntity tag) {
        oDocument.field(NAME_FIELD, tag.getName());
        oDocument.field(FIRST_CREATED_FIELD, tag.getFirstCreated());
        oDocument.field(LAST_UPDATED_FIELD, tag.getLastUpdated());
        oDocument.field(ATTRIBUTES_FIELD, tag.getAttributes());
        List<ODocument> componentDocuments = tag.getComponents().stream()
                .map(TagEntityAdapter::toDocument)
                .collect(Collectors.toList());
        oDocument.field(COMPONENTS_FIELD, componentDocuments);
    }

    private static ODocument toDocument(AssociatedComponent component) {
        ODocument document = new ODocument();
        document.field(COMPONENT_REPOSITORY_FIELD, component.getRepository());
        document.field(COMPONENT_GROUP_FIELD, component.getGroup());
        document.field(COMPONENT_NAME_FIELD, component.getName());
        document.field(COMPONENT_VERSION_FIELD, component.getVersion());
        return document;
    }

    private static AssociatedComponent toComponent(ODocument document) {
        AssociatedComponent component = new AssociatedComponent();
        component.setRepository(document.field(COMPONENT_REPOSITORY_FIELD));
        component.setGroup(document.field(COMPONENT_GROUP_FIELD));
        component.setName(document.field(COMPONENT_NAME_FIELD));
        component.setVersion(document.field(COMPONENT_VERSION_FIELD));
        return component;
    }

    public Optional<TagEntity> findByName(ODatabaseDocumentTx tx, String name) {
        OIndex<?> nameIndex = tx.getMetadata().getIndexManager().getIndex(NAME_INDEX);
        OIdentifiable identifiable = (OIdentifiable) nameIndex.get(name);
        if (identifiable == null) {
            return Optional.empty();
        }
        return Optional.of(transformEntity(identifiable.getRecord()));
    }

    public Iterable<TagEntity> search(ODatabaseDocumentTx tx, Map<String, String> attributes) {
        List<QueryPredicate> predicates = new ArrayList<>();
        for (Entry<String, String> entry : attributes.entrySet()) {
            String field = ATTRIBUTES_FIELD + "[" + stringLiteral(entry.getKey()) + "]";
            predicates.add(new QueryPredicate(field, "=", stringLiteral(entry.getValue())));
        }

        String query = buildQuery(predicates);
        log.info("Searching for tags with query={}", query);
        List<ODocument> documents = tx.query(new OSQLSynchQuery<>(query));
        return transform(documents);
    }

    /**
     * @return SQL string literal for given value (enclosed in single quotes)
     */
    private static String stringLiteral(String value) {
        return "'" + value + "'";
    }

    private static String buildQuery(List<QueryPredicate> predicates) {
        StringBuilder query = new StringBuilder("select * from ").append(DB_CLASS);
        Iterator<QueryPredicate> it = predicates.iterator();
        if (it.hasNext()) {
            query.append(" where");
        }
        while (it.hasNext()) {
            QueryPredicate predicate = it.next();
            query.append(' ').append(predicate.field)
                    .append(' ')
                    .append(predicate.operator)
                    .append(' ')
                    .append(predicate.value);
            if (it.hasNext()) {
                query.append(" and");
            }
        }
        query.append(" order by ").append(LAST_UPDATED_FIELD).append(" DESC");
        return query.toString();
    }

    private static class QueryPredicate {
        private final String field;
        private final String operator;
        private  final String value;

        public QueryPredicate(String field, String operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
    }
}
