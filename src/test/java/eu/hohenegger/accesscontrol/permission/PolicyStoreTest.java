package eu.hohenegger.accesscontrol.permission;

import eu.hohenegger.accesscontrol.permission.model.ActionEntity;
import eu.hohenegger.accesscontrol.permission.model.PermissionEntity;
import eu.hohenegger.accesscontrol.permission.model.PermissionRepository;
import eu.hohenegger.accesscontrol.permission.model.PolicyStore;
import eu.hohenegger.accesscontrol.permission.model.RelationshipEntity;
import eu.hohenegger.accesscontrol.permission.model.RelationshipRepository;
import eu.hohenegger.accesscontrol.web.PolicyRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static eu.hohenegger.accesscontrol.permission.Action.VIEW_DUES;
import static eu.hohenegger.accesscontrol.permission.Action.VIEW_POLLS;
import static eu.hohenegger.accesscontrol.permission.Action.VOTE;
import static eu.hohenegger.accesscontrol.permission.Relationships.LANDLORDS;
import static eu.hohenegger.accesscontrol.permission.Relationships.OWNERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.tuple;

/** The fluent builder produces a JPA model; the application stores it and decides with what was stored. */
@DataJpaTest
@Import(PolicyRules.class)
class PolicyStoreTest {

    @Autowired
    Policy policy;

    @Autowired
    PolicyStore store;

    @Autowired
    PermissionRepository permissions;

    @Autowired
    RelationshipRepository relationships;

    @Test
    void theStoredPolicyIsThePolicyInUse() {
        List<PermissionEntity> stored = store.load();

        assertThat(stored).extracting(entity -> entity.relationship().name(), entity -> entity.action().name())
                .containsExactlyElementsOf(policy.permissions().stream()
                        .map(permission -> tuple(permission.relationship().name(), permission.action().name()))
                        .toList());
    }

    @Test
    void storesTheDescriptionsAndWhichBeanDefinedEachPermission() {
        PermissionEntity landlordsViewTenant = store.load().stream()
                .filter(entity -> entity.relationship().name().equals("landlords"))
                .findFirst().orElseThrow();

        assertThat(landlordsViewTenant.action().name()).isEqualTo("VIEW_TENANT");
        assertThat(landlordsViewTenant.definedBy()).isEqualTo("associationPermissions");
        assertThat(relationships.findById("landlords")).get()
                .extracting(RelationshipEntity::targetType, RelationshipEntity::description)
                .containsExactly("Membership", LANDLORDS.description());
    }

    @Test
    void relationshipsAndActionsUsedByManyPermissionsAreStoredOnce() {
        assertThat(relationships.count()).isEqualTo(Relationship.all().size());
        assertThat(permissions.count()).isEqualTo(policy.permissions().size());
    }

    @Test
    void theBuilderStoresARepeatedRuleOnce() {
        List<PermissionEntity> model = Policy.define()
                .allow(OWNERS).to(VIEW_POLLS, VOTE)
                .allow(OWNERS).to(VOTE)
                .build();

        assertThat(Policy.of(store.replaceWith(model)).describe()).containsExactly(
                Map.entry("owners", List.of("VIEW_POLLS", "VOTE")));
    }

    @Test
    void aStoredPermissionThatTheCodeCannotJudgeIsRejected() {
        // landlords relate to tenancies; the compiler prevents allow(LANDLORDS).to(VIEW_DUES), the database doesn't
        PermissionEntity tampered = new PermissionEntity(
                new RelationshipEntity(LANDLORDS.name(), "Membership", LANDLORDS.description()),
                new ActionEntity(VIEW_DUES.name(), "DuesAccount", VIEW_DUES.description()), 0, "sql console");

        assertThatIllegalStateException().isThrownBy(() -> Policy.of(List.of(tampered)))
                .withMessageContaining("landlords relate to Membership, but VIEW_DUES is done to DuesAccount");
    }

    @Test
    void aStoredPermissionForAnUnknownActionIsRejected() {
        PermissionEntity unknown = new PermissionEntity(
                new RelationshipEntity(OWNERS.name(), "AssociationScoped", OWNERS.description()),
                new ActionEntity("PAINT_THE_FENCE", "Association", "paint the fence"), 0, null);

        assertThatIllegalStateException().isThrownBy(() -> Policy.of(List.of(unknown)))
                .withMessageContaining("Unknown action 'PAINT_THE_FENCE'");
    }
}
