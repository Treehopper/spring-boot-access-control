package eu.hohenegger.accesscontrol.permission.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One line of the policy, {@code allow(relationship).to(action)}. Relationships and actions are shared between
 * permissions and are persisted together with the first permission that uses them.
 */
@Entity
@Table(name = "policy_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"relationship_name", "action_name"}))
public class PermissionEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "relationship_name")
    private RelationshipEntity relationship;

    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "action_name")
    private ActionEntity action;

    /** Position in the policy; decisions try the allowed relationships in this order. */
    @Column(nullable = false)
    private int ordinal;

    /** The bean that contributed this permission, e.g. {@code associationPermissions}. */
    @Column(name = "defined_by", length = 200)
    private String definedBy;

    protected PermissionEntity() {
    }

    public PermissionEntity(RelationshipEntity relationship, ActionEntity action, int ordinal, String definedBy) {
        this.relationship = relationship;
        this.action = action;
        this.ordinal = ordinal;
        this.definedBy = definedBy;
    }

    public Long id() {
        return id;
    }

    public RelationshipEntity relationship() {
        return relationship;
    }

    public ActionEntity action() {
        return action;
    }

    public int ordinal() {
        return ordinal;
    }

    public String definedBy() {
        return definedBy;
    }

    @Override
    public String toString() {
        return "allow(" + relationship + ").to(" + action + ")";
    }
}
