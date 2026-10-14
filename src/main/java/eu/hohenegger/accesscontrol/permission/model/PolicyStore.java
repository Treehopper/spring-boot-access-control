package eu.hohenegger.accesscontrol.permission.model;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Keeps the stored policy in sync with the one the {@link eu.hohenegger.accesscontrol.permission.PolicyCustomizer}
 * beans define. The code is the source of truth: at startup the stored policy is replaced as a whole, so the tables
 * never contain a permission that was removed from the code.
 */
@Component
public class PolicyStore {

    private final PermissionRepository permissions;
    private final RelationshipRepository relationships;
    private final ActionRepository actions;
    private final EntityManager entityManager;

    PolicyStore(PermissionRepository permissions, RelationshipRepository relationships, ActionRepository actions,
                EntityManager entityManager) {
        this.permissions = permissions;
        this.relationships = relationships;
        this.actions = actions;
        this.entityManager = entityManager;
    }

    /** Replaces the stored policy and returns it as read back from the database. */
    @Transactional
    public List<PermissionEntity> replaceWith(List<PermissionEntity> policy) {
        permissions.deleteAllInBatch();
        relationships.deleteAllInBatch();
        actions.deleteAllInBatch();
        permissions.saveAll(policy);
        permissions.flush();
        entityManager.clear();
        return load();
    }

    @Transactional(readOnly = true)
    public List<PermissionEntity> load() {
        return permissions.findAllInPolicyOrder();
    }
}
