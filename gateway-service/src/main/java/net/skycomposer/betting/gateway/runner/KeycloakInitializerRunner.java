package net.skycomposer.betting.gateway.runner;

import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.gateway.security.WebSecurityConfig;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class KeycloakInitializerRunner implements CommandLineRunner {

    @Autowired
    private Keycloak keycloakAdmin;

    @Value("${spring.keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${betting-app.redirect-url}")
    private String bettingAppRedirectUrl;

    @Override
    public void run(String... args) {
        log.info("Initializing '{}' realm in Keycloak ...", BETTING_REALM_NAME);

        Optional<RealmRepresentation> representationOptional = keycloakAdmin.realms()
                .findAll()
                .stream()
                .filter(r -> r.getRealm().equals(BETTING_REALM_NAME))
                .findAny();
        if (representationOptional.isPresent()) {
            log.info("Removing already pre-configured '{}' realm", BETTING_REALM_NAME);
            keycloakAdmin.realm(BETTING_REALM_NAME).remove();
        }

        // Realm
        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRealm(BETTING_REALM_NAME);
        realmRepresentation.setEnabled(true);
        realmRepresentation.setRegistrationAllowed(true);
        realmRepresentation.setResetPasswordAllowed(false);
        realmRepresentation.setRememberMe(true);
        realmRepresentation.setLoginWithEmailAllowed(false);
        realmRepresentation.setDuplicateEmailsAllowed(false);
        realmRepresentation.setVerifyEmail(false);

        // Client
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId(BETTING_APP_CLIENT_ID);
        clientRepresentation.setImplicitFlowEnabled(true);
        clientRepresentation.setDirectAccessGrantsEnabled(true);
        clientRepresentation.setPublicClient(true);
        clientRepresentation.setRedirectUris(List.of(bettingAppRedirectUrl));
        clientRepresentation.setWebOrigins(List.of("*"));
        clientRepresentation.setDefaultRoles(new String[]{WebSecurityConfig.BETTING_USER});
        realmRepresentation.setClients(List.of(clientRepresentation));

        // Users
        List<UserRepresentation> userRepresentations = BETTING_APP_USERS.stream()
                .map(userPass -> {
                    // User Credentials
                    CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
                    credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
                    credentialRepresentation.setValue(userPass.password());

                    // User
                    UserRepresentation userRepresentation = new UserRepresentation();
                    userRepresentation.setUsername(userPass.username());
                    userRepresentation.setEnabled(true);
                    userRepresentation.setCredentials(List.of(credentialRepresentation));
                    userRepresentation.setClientRoles(getClientRoles(userPass));
                    userRepresentation.setEmail(userPass.email());

                    return userRepresentation;
                })
                .toList();
        realmRepresentation.setUsers(userRepresentations);

        // Create Realm
        keycloakAdmin.realms().create(realmRepresentation);

        // Testing
        UserPass admin = BETTING_APP_USERS.get(0);
        log.info("Testing getting token for '{}' ...", admin.username());

        Keycloak keycloakErpApp = KeycloakBuilder.builder().serverUrl(keycloakServerUrl)
                .realm(BETTING_REALM_NAME).username(admin.username()).password(admin.password())
                .clientId(BETTING_APP_CLIENT_ID).build();

        log.info("'{}' token: {}", admin.username(), keycloakErpApp.tokenManager().grantToken().getToken());
        log.info("'{}' initialization completed successfully!", BETTING_REALM_NAME);
    }

    private Map<String, List<String>> getClientRoles(UserPass userPass) {
        List<String> roles = new ArrayList<>();
        roles.add(WebSecurityConfig.BETTING_USER);
        if ("admin".equals(userPass.username())) {
            roles.add(WebSecurityConfig.BETTING_MANAGER);
        }
        return Map.of(BETTING_APP_CLIENT_ID, roles);
    }

    private static final String BETTING_REALM_NAME = "betting-realm";
    private static final String BETTING_APP_CLIENT_ID = "betting-app";
    private static final List<UserPass> BETTING_APP_USERS = Arrays.asList(
            new UserPass("admin", "admin", "admin@example.com"),
            new UserPass("user", "user", "user@example.com"));

    private record UserPass(String username, String password, String email) {
    }
}