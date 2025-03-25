package viettel.dac.identityservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.entity.Permission;
import viettel.dac.identityservice.entity.User;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.service.AuthorizationService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {
        // First try to load by ID
        User user = userRepository.findById(usernameOrId)
                .orElse(null);

        // If not found by ID, try by username
        if (user == null) {
            user = userRepository.findByUsername(usernameOrId)
                    .orElse(null);
        }

        // If still not found, try by email
        if (user == null) {
            user = userRepository.findByEmail(usernameOrId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username/email/id: " + usernameOrId));
        }

        // Get permissions for this user from roles
        List<Permission> permissions = authorizationService.getUserPermissions(user.getId(), null, null);
        List<String> permissionNames = permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toList());

        return UserPrincipal.create(user, permissionNames);
    }
}