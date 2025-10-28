package com.capstone_project.elderly_platform.pojos;

import com.capstone_project.elderly_platform.enums.EnumRoleType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "role_id")
    UUID roleID;

    @Enumerated(EnumType.STRING)
    EnumRoleType roleName;

    @Column(name = "description", length = 500)
    String description;

    @ManyToMany(mappedBy = "roles")
    List<Account> accounts;
}
