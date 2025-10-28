package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
//    Page<Aircraft> findAllByDeletedFalse(Pageable pageable);
//    List<Aircraft> findByDeletedIsFalse();
//    Aircraft findAircraftById(UUID id);
//    Aircraft findAircraftByCode(String code);
//    Page<Aircraft> findAll(Specification<Aircraft> spec, Pageable pageable);
}
