package eu.hohenegger.accesscontrol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @Column(length = 50)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** Read-only inverse side of {@link Membership#resident()}. */
    @OneToMany(mappedBy = "resident")
    @OrderBy("id")
    private List<Membership> memberships = new ArrayList<>();

    protected User() {
    }

    User(String username, String displayName) {
        this.username = Objects.requireNonNull(username);
        this.displayName = Objects.requireNonNull(displayName);
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    List<Membership> memberships() {
        return memberships;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User user && user.username.equals(username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return username;
    }
}
