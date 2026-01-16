package com.capstone_project.elderly_platform.repositories;

import com.capstone_project.elderly_platform.enums.EnumAttachmentEntityType;
import com.capstone_project.elderly_platform.pojos.AttachmentResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentResourceRepository extends JpaRepository<AttachmentResource, UUID> {

    // Find attachments by entity (feedback, qualification, etc.)
    List<AttachmentResource> findByEntityIdAndEntityTypeAndDeletedIsFalseOrderByOrderIndexAsc(
            UUID entityId, EnumAttachmentEntityType entityType);

    // Find all attachments for an entity
    List<AttachmentResource> findByEntityIdAndEntityTypeAndDeletedIsFalse(UUID entityId, EnumAttachmentEntityType entityType);
}
