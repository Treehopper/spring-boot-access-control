package eu.hohenegger.accesscontrol.domain;

/** Who a message is addressed to within its association. */
public enum Audience {
    EVERYONE,
    OWNERS,
    TENANTS,
    /** Everyone who lives in the association: tenants and owners who live in their own unit. */
    OCCUPANTS,
    INDIVIDUAL
}
