package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumActivationStatusType;
import com.capstone_project.elderly_platform.enums.EnumGenderType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "elderly_profiles")
public class ElderlyProfile extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "elderly_profile_id")
    UUID elderlyProfileId;

    String fullName;

    LocalDate birthDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "location", columnDefinition = "jsonb")
    String location;

    @Enumerated(EnumType.STRING)
    EnumGenderType gender;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    String avatarUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_data", columnDefinition = "jsonb")
    String profileData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "care_requirement", columnDefinition = "jsonb")
    String careRequirement;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "health_note", columnDefinition = "TEXT")
    String healthNote;

    @Enumerated(EnumType.STRING)
    EnumActivationStatusType status;

    @ManyToOne
    @JoinColumn(name = "care_seeker_id")
    CareSeekerProfile careSeekerProfile;

    @OneToMany(mappedBy = "elderlyProfile")
    List<CareService> careServices;

}
