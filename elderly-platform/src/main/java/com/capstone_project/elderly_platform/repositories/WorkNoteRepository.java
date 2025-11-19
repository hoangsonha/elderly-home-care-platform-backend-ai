package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.pojos.WorkNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkNoteRepository extends JpaRepository<WorkNote, UUID> {
    Optional<WorkNote> findByWorkNoteIdAndDeletedIsFalse(UUID id);
}

















