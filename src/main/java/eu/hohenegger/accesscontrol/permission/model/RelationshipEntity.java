package eu.hohenegger.accesscontrol.permission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A stored {@link eu.hohenegger.accesscontrol.permission.Relationship}: the role half of a permission. */
@Entity
@Table(name = "policy_relationship")
public class RelationshipEntity {

    @Id
    @Column(length = 100)
    private String name;

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    @Column(nullable = false, length = 1000)
    private String description;

    protected RelationshipEntity() {
    }

    public RelationshipEntity(String name, String targetType, String description) {
        this.name = name;
        this.targetType = targetType;
        this.description = description;
    }

    public String name() {
        return name;
    }

    public String targetType() {
        return targetType;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
