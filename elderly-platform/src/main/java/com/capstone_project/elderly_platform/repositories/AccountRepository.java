package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Account getAccountByEmail(String email);
    Optional<Account> findByAccountIdAndDeletedIsFalse(UUID id);
//    Optional<AircraftType> findByModelAndManufacturer(String model, String manufacturer);
//    Page<AircraftType> findAllByDeletedFalse(Pageable pageable);
//    List<AircraftType> findByDeletedIsFalse();
//    AircraftType findAircraftTypeById(UUID id);
//    AircraftType findAircraftTypeByModel(String model);
//    AircraftType findAircraftTypeByManufacturer(String manufacturer);
//    Page<AircraftType> findAll(Specification<AircraftType> spec, Pageable pageable);
}
