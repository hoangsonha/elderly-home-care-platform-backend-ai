package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumAttachmentEntityType;
import com.capstone_project.elderly_platform.enums.EnumAttachmentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

/**
 * <Table name> attachment_resources
 * <Description of the table>
 * This table stores metadata about various types of attachments (videos, images, or other files)
 * that can be linked to different entities within the system. The actual files are stored externally
 * (e.g., Firebase Storage), and this table holds the links and metadata.
 * - Primary keys: attachment_resource_id
 * - Foreign keys: entity_id (references the target entity based on entity_type)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "attachment_resources")
public class AttachmentResource extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "attachment_resource_id")
    UUID attachmentResourceId;

    @Column(name = "entity_id", nullable = false)
    UUID entityId; // ID of the target entity (feedback_id, qualification_id, etc.)

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    EnumAttachmentEntityType entityType; // QUALIFICATION, DISPUTE, COURSE, MODULE, LESSON, AVATAR, FEEDBACK

    @Column(name = "title", length = 255)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    EnumAttachmentType type; // VIDEO, IMAGE, OTHER

    @Column(name = "url", columnDefinition = "TEXT", nullable = false)
    String url; // URL where the actual attachment file is located (e.g., Firebase Storage)

    @Column(name = "description", columnDefinition = "TEXT")
    String description; // Description of the attachment

    @Column(name = "order_index")
    Integer orderIndex; // Display order for attachments associated with an entity
}
