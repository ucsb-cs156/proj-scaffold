package edu.ucsb.cs156.example.repositories;

import edu.ucsb.cs156.example.entities.Account;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

/**
 * The AccountRepository is a repository for Account entities, that is, it is the abstraction for
 * the database table for Accounts
 */
@RepositoryRestResource(path = "accounts")
@Repository
public interface AccountRepository extends CrudRepository<Account, String> {
  @Override
  @PreAuthorize("hasRole('ADMIN')")
  void deleteById(String email);

  // Only Admin can see all accounts
  @Override
  @PreAuthorize("hasRole('ADMIN')")
  List<Account> findAll();

  @Override
  @PreAuthorize(
      "hasRole('ADMIN') or (hasRole('USER') and #email == authentication.principal.attributes['email'])")
  java.util.Optional<Account> findById(String email);

  // Only Admins can create/update urls
  @Override
  @PreAuthorize("hasRole('ADMIN')")
  <S extends Account> S save(S entity);
}
