package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.OAuthTokenResponse;
import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;
import com.nitrotech.api.domain.auth.exception.AccountNotActiveException;
import com.nitrotech.api.domain.auth.exception.UserNotFoundException;
import com.nitrotech.api.domain.auth.provider.OAuthProvider;
import com.nitrotech.api.domain.auth.provider.OAuthProviderResolver;
import com.nitrotech.api.domain.auth.repository.OAuthAccountRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthCallbackUseCase {

    private final OAuthProviderResolver oauthProviderResolver;
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthResult execute(String providerName, String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("OAUTH_CODE_REQUIRED", "Authorization code is required");
        }

        OAuthProvider provider = oauthProviderResolver.getProvider(providerName);
        OAuthTokenResponse tokenResponse = provider.exchangeAuthorizationCode(code);
        OAuthUserInfo userInfo = provider.fetchUserInfo(tokenResponse);

        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new BadRequestException("OAUTH_EMAIL_REQUIRED", "OAuth provider did not return an email address");
        }
        if (!userInfo.emailVerified()) {
            throw new BadRequestException("OAUTH_EMAIL_NOT_VERIFIED", "OAuth provider email must be verified");
        }
        if (userInfo.externalId() == null || userInfo.externalId().isBlank()) {
            throw new BadRequestException("OAUTH_EXTERNAL_ID_REQUIRED", "OAuth provider did not return an external user ID");
        }

        String resolvedProvider = provider.getProviderName();
        UserRepository.UserAuthAccount user = oauthAccountRepository
                .findByProviderAndExternalId(resolvedProvider, userInfo.externalId())
                .map(link -> {
                    oauthAccountRepository.saveOrUpdate(link.userId(), resolvedProvider, userInfo);
                    return userRepository.findAuthAccountById(link.userId())
                            .orElseThrow(UserNotFoundException::new);
                })
                .orElseGet(() -> findOrCreateUser(resolvedProvider, userInfo));

        activateIfNeeded(user);
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
                    throw new BadRequestException(
                            "OAUTH_ACCOUNT_ALREADY_LINKED",
                            "Another " + provider + " account is already linked to this user"
                    );
                });

        oauthAccountRepository.saveOrUpdate(user.id(), provider, userInfo);
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
        if ("inactive".equalsIgnoreCase(user.status())) {
            userRepository.activateUser(user.id());
            return;
        }
        if (!"active".equalsIgnoreCase(user.status())) {
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
