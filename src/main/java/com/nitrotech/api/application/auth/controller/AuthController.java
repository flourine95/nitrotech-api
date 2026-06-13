package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.application.auth.request.*;
import com.nitrotech.api.domain.auth.dto.*;
import com.nitrotech.api.domain.auth.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResult<AuthResult>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResult result = registerUseCase.execute(
                new RegisterCommand(request.name(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResult<AuthResult>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResult result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));

        UserPrincipal principal = new UserPrincipal(
                result.user().id(),
                result.user().email(),
                result.user().name(),
                result.user().roles(),
                result.user().permissions()
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, result.user().email());

        return ResponseEntity.ok(ApiResult.ok(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request) {
        logoutUseCase.execute(request);
        return ResponseEntity.ok(ApiResult.ok("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResult<Void>> logoutAll(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        logoutUseCase.executeAll(principal.email(), request);
        return ResponseEntity.ok(ApiResult.ok("Logged out from all devices"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResult<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResult.ok("If the email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResult<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResult.ok("Password reset successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResult<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(request.token());
        return ResponseEntity.ok(ApiResult.ok("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResult<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResult.ok("Verification email sent"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserProfileData>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResult.ok(null, null));
        }
        return ResponseEntity.ok(ApiResult.ok(getProfileUseCase.execute(principal.id())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResult<UserProfileData>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateProfileUseCase.execute(
                new UpdateProfileCommand(principal.id(), request.name(), request.phone(), request.avatar()))));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.execute(
                new ChangePasswordCommand(principal.id(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.ok(ApiResult.ok("Password changed successfully"));
    }
}
