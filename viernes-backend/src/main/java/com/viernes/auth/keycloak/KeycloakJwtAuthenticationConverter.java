package com.viernes.auth.keycloak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convierte un JWT emitido por Keycloak en un AuthenticationToken de Spring Security.
 * Extrae los roles de realm_access.roles y los mapea a GrantedAuthority con prefijo 'ROLE_'.
 * 
 * Ejemplo de claim Keycloak:
 * {
 *   "realm_access": {
 *     "roles": ["ADMIN", "default-roles-viernes-realm"]
 *   },
 *   "preferred_username": "admin@viernes.com"
 * }
 */
@Component
@Profile("keycloak")
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String PREFERRED_USERNAME = "preferred_username";

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalClaimValue = jwt.getClaimAsString(PREFERRED_USERNAME);
        if (principalClaimValue == null || principalClaimValue.isBlank()) {
            principalClaimValue = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess == null || !realmAccess.containsKey(ROLES)) {
            return Collections.emptyList();
        }

        Object rolesObj = realmAccess.get(ROLES);
        if (!(rolesObj instanceof List<?> rolesList)) {
            return Collections.emptyList();
        }

        return rolesList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                // Ignorar roles por defecto internos de Keycloak
                .filter(role -> !role.startsWith("default-roles") && !role.equals("offline_access") && !role.equals("uma_authorization"))
                .map(role -> {
                    String roleName = role.toUpperCase();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toSet());
    }
}
