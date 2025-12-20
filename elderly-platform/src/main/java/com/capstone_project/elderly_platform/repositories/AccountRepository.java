package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
       Account getAccountByEmail(String email);

       Optional<Account> findByAccountIdAndDeletedIsFalse(UUID id);
}
