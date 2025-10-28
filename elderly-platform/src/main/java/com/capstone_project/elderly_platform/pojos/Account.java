package com.capstone_project.elderly_platform.pojos;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "accounts")
public class Account extends BaseEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "account_id")
    UUID accountID;

    @Column(name = "first_name", length = 25)
    String firstName;

    @Column(name = "last_name", length = 25)
    String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    String email;

    @Column(name = "password", length = 60)
    String password;

    @Column(name = "phone", unique = true, length = 12)
    String phone;

    @Column(name = "dob", columnDefinition = "DATE")
    LocalDate dob;

    @Column(name = "enabled", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    Boolean enabled = false;

    @Column(name = "non_locked", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    Boolean nonLocked = true;

    @Column(name = "access_token", columnDefinition = "TEXT")
    String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    String refreshToken;

    @Column(name = "is_google_account", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    Boolean isGoogleAccount = false;

    @ManyToMany
    @JoinTable(name = "account_roles", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    List<Role> roles;

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "seat_map", columnDefinition = "jsonb")
//    Map<String, Object> seatMap;

}
