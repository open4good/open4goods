package org.open4goods.b2bapi.repository;

import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for OIDC-provisioned users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByOidcProviderAndOidcSubject(OidcProvider oidcProvider, String oidcSubject);
}
