package com.capstone_project.elderly_platform.configurations;

import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomAccountDetailService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.getAccountByEmail(username);
        if(account != null) {
            return CustomAccountDetail.mapAccountToAccountDetail(account);
        }
        return null;
    }
}
