package eu.hohenegger.accesscontrol.permission.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionRepository extends JpaRepository<ActionEntity, String> {
}
