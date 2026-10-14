package eu.hohenegger.accesscontrol.permission.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    @Query("select p from PermissionEntity p join fetch p.relationship join fetch p.action order by p.ordinal")
    List<PermissionEntity> findAllInPolicyOrder();
}
