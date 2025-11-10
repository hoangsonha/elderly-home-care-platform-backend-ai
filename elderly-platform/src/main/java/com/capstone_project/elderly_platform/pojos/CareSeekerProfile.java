package com.capstone_project.elderly_platform.pojos;

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
@Table(name = "care_seeker_profiles")
public class CareSeekerProfile extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "care_seeker_profile_id")
    UUID careSeekerProfileId;

    String fullName;

    String phoneNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "location", columnDefinition = "jsonb")
    String location;

    LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    EnumGenderType gender;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_data", columnDefinition = "jsonb")
    String profileData;

    @OneToOne
    @JoinColumn(name = "account_id")
    Account account;

    @OneToMany(mappedBy = "careSeekerProfile")
    List<ElderlyProfile> elderlyProfiles;

    @OneToMany(mappedBy = "careSeekerProfile")
    List<CareService> careServices;

    @OneToMany(mappedBy = "seekerProfile")
    List<Payment> payments;

}
