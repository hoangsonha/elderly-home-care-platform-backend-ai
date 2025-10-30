package com.capstone_project.elderly_platform.pojos;

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
    UUID accountId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    String email;

    @Column(name = "password", length = 60)
    String password;

    @Column(name = "enabled", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    Boolean enabled = false;

    @Column(name = "non_locked", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    Boolean nonLocked = true;

    @Column(name = "access_token", columnDefinition = "TEXT")
    String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    String refreshToken;

    @Column(length = 6)
    String codeVerify;

    @ManyToOne
    @JoinColumn(name = "role_id")
    Role role;

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "seat_map", columnDefinition = "jsonb")
//    Map<String, Object> seatMap;

}
