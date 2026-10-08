package eu.hohenegger.accesscontrol.domain;

/**
 * How someone belongs to an association: owners are open participants (they see the member list and vote),
 * tenants are silent participants (they receive notices only).
 */
public enum Role {
    OWNER("Owner"),
    TENANT("Tenant");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
