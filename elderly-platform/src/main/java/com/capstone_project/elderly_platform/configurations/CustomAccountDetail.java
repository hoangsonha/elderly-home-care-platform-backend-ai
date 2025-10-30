package com.capstone_project.elderly_platform.configurations;

import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.Role;
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

@Getter
@Setter
@Builder
public class CustomAccountDetail implements UserDetails {
    private UUID id;
    private String email;
    private String password;
    private String accessToken;
    private String refreshToken;
    private Boolean nonLocked;
    private Boolean enabled;
    private Collection<GrantedAuthority> grantedAuthorities;

    public static CustomAccountDetail mapAccountToAccountDetail(Account account) {

        Role role = account.getRole();
        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(role.getRoleName().name());
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(simpleGrantedAuthority);

        return CustomAccountDetail.builder()
                .id(account.getAccountId())
                .email(account.getEmail())
                .password(account.getPassword())
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
