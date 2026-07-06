package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.UserStatus;
import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.OAuthTokenResponse;
import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;
import com.nitrotech.api.domain.auth.exception.AccountNotActiveException;
import com.nitrotech.api.domain.auth.exception.OAuthAccountAlreadyLinkedException;
import com.nitrotech.api.domain.auth.exception.OAuthCodeRequiredException;
import com.nitrotech.api.domain.auth.exception.OAuthEmailNotVerifiedException;
import com.nitrotech.api.domain.auth.exception.OAuthEmailRequiredException;
import com.nitrotech.api.domain.auth.exception.OAuthExternalIdRequiredException;
import com.nitrotech.api.domain.auth.exception.UserNotFoundException;
import com.nitrotech.api.domain.auth.provider.OAuthProvider;
import com.nitrotech.api.domain.auth.provider.OAuthProviderResolver;
import com.nitrotech.api.domain.auth.repository.OAuthAccountRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuthCallbackUseCase {

    private final OAuthProviderResolver oauthProviderResolver;
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResult execute(String providerName, String code) {
        if (code == null || code.isBlank()) {
            throw new OAuthCodeRequiredException();
        }

        OAuthProvider provider = oauthProviderResolver.getProvider(providerName);
        OAuthTokenResponse tokenResponse = provider.exchangeAuthorizationCode(code);
        OAuthUserInfo userInfo = provider.fetchUserInfo(tokenResponse);

        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new OAuthEmailRequiredException();
        }
        if (!userInfo.emailVerified()) {
            throw new OAuthEmailNotVerifiedException();
        }
        if (userInfo.externalId() == null || userInfo.externalId().isBlank()) {
            throw new OAuthExternalIdRequiredException();
        }

        String resolvedProvider = provider.getProviderName();
        UserRepository.UserAuthAccount user = oauthAccountRepository
                .findByProviderAndExternalId(resolvedProvider, userInfo.externalId())
                .map(link -> {
                    UserRepository.UserAuthAccount linkedUser = userRepository.findAuthAccountById(link.userId())
                            .orElseThrow(UserNotFoundException::new);
                    activateIfNeeded(linkedUser);
                    oauthAccountRepository.saveOrUpdate(link.userId(), resolvedProvider, userInfo);
                    return linkedUser;
                })
                .orElseGet(() -> findOrCreateUser(resolvedProvider, userInfo));

        UserRepository.UserAuthorities authorities = userRepository.findAuthoritiesByUserId(user.id());

        return AuthResult.ofUser(new AuthResult.UserData(
                user.id(),
                user.name(),
                user.email(),
                authorities.roles(),
                authorities.permissions()
        ));
    }

    private UserRepository.UserAuthAccount findOrCreateUser(String provider, OAuthUserInfo userInfo) {
        return userRepository.findAuthAccountByEmail(userInfo.email())
                .map(user -> linkExistingUser(user, provider, userInfo))
                .orElseGet(() -> createUser(provider, userInfo));
    }

    private UserRepository.UserAuthAccount linkExistingUser(
            UserRepository.UserAuthAccount user,
            String provider,
            OAuthUserInfo userInfo
    ) {
        oauthAccountRepository.findByProviderAndUserId(provider, user.id())
                .filter(link -> !link.externalId().equals(userInfo.externalId()))
                .ifPresent(link -> {
                    throw new OAuthAccountAlreadyLinkedException(provider);
                });

        activateIfNeeded(user);
        oauthAccountRepository.saveOrUpdate(user.id(), provider, userInfo);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.OAUTH_ACCOUNT_LINKED,
                AuditResourceType.USER,
                user.id(),
                null,
                Map.of("provider", provider, "email", userInfo.email()),
                Map.of("provider", provider, "externalId", userInfo.externalId())
        ));
        return user;
    }

    private UserRepository.UserAuthAccount createUser(String provider, OAuthUserInfo userInfo) {
        UserRepository.UserAuthAccount created = userRepository.saveOAuthUser(
                resolveDisplayName(userInfo),
                userInfo.email(),
                userInfo.avatar(),
                provider,
                userInfo.externalId()
        );
        oauthAccountRepository.saveOrUpdate(created.id(), provider, userInfo);
        return created;
    }

    private void activateIfNeeded(UserRepository.UserAuthAccount user) {
        UserStatus status = UserStatus.fromValue(user.status());
        if (status == UserStatus.inactive) {
            userRepository.activateUser(user.id());
            return;
        }
        if (status != UserStatus.active) {
            throw new AccountNotActiveException(user.status());
        }
    }

    private String resolveDisplayName(OAuthUserInfo userInfo) {
        if (userInfo.name() != null && !userInfo.name().isBlank()) {
            return userInfo.name();
        }
        return userInfo.email();
    }
}
