package com.capstone_project.elderly_platform.configurations;

import com.capstone_project.elderly_platform.pojos.Account;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Getter
@Setter
@Builder
public class CustomAccountDetail implements UserDetails {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String accessToken;
    private String refreshToken;
    private Boolean nonLocked;
    private Boolean enabled;
    private Collection<GrantedAuthority> grantedAuthorities;

    public static CustomAccountDetail mapAccountToAccountDetail(Account account) {

        List<GrantedAuthority> roles = account.getRoles().stream().map(
                role -> new SimpleGrantedAuthority(role.getRoleName().name())
                ).collect(Collectors.toList());

        return CustomAccountDetail.builder()
                .id(account.getAccountID())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .password(account.getPassword())
                .phone(account.getPhone())
                .nonLocked(account.getNonLocked())
                .enabled(account.getEnabled())
                .accessToken(account.getAccessToken())
                .refreshToken(account.getRefreshToken())
                .grantedAuthorities(roles)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return nonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
