package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.application.auth.request.*;
import com.nitrotech.api.domain.auth.dto.*;
import com.nitrotech.api.domain.auth.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth", description = "Authentication and account management APIs")
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

    @Operation(summary = "Register", description = "Create a new user account. A verification email will be sent after registration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResult<AuthResult>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResult result = registerUseCase.execute(
                new RegisterCommand(request.name(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(result));
    }

    @Operation(summary = "Login", description = "Authenticate with email and password. Returns user info and creates a session.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResult<AuthResult>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResult result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));

        UserPrincipal principal = new UserPrincipal(result.user().id(), result.user().email(), result.user().name());
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

    @Operation(summary = "Logout", description = "Invalidate the current session.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request) {
        logoutUseCase.execute(request);
        return ResponseEntity.ok(ApiResult.ok(null, "Logged out successfully"));
    }

    @Operation(summary = "Logout all devices", description = "Invalidate all active sessions for the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out from all devices"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResult<Void>> logoutAll(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        logoutUseCase.executeAll(principal.email(), request);
        return ResponseEntity.ok(ApiResult.ok(null, "Logged out from all devices"));
    }

    @Operation(summary = "Forgot password", description = "Send a password reset link to the given email. Always returns 200 to prevent email enumeration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reset link sent if email exists"),
            @ApiResponse(responseCode = "400", description = "Invalid email format", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResult<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResult.ok(null, "If the email exists, a reset link has been sent"));
    }

    @Operation(summary = "Reset password", description = "Reset the user's password using a valid reset token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResult<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResult.ok(null, "Password reset successfully"));
    }

    @Operation(summary = "Verify email", description = "Verify the user's email address using the token sent during registration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResult<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(request.token());
        return ResponseEntity.ok(ApiResult.ok(null, "Email verified successfully"));
    }

    @Operation(summary = "Resend verification email", description = "Resend the email verification link. Always returns 200 to prevent email enumeration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email sent if account exists and is unverified"),
            @ApiResponse(responseCode = "400", description = "Invalid email format", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResult<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResult.ok(null, "Verification email sent"));
    }

    @Operation(summary = "Get current user", description = "Retrieve the profile of the currently authenticated user. Returns null if not authenticated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully or null if not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserProfileData>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResult.ok(null));
        }
        return ResponseEntity.ok(ApiResult.ok(getProfileUseCase.execute(principal.id())));
    }

    @Operation(summary = "Update profile", description = "Update the current user's name, phone, and avatar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/profile")
    public ResponseEntity<ApiResult<UserProfileData>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateProfileUseCase.execute(
                new UpdateProfileCommand(principal.id(), request.name(), request.phone(), request.avatar()))));
    }

    @Operation(summary = "Change password", description = "Change the current user's password. Requires the current password for verification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or current password is incorrect", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/change-password")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.execute(
                new ChangePasswordCommand(principal.id(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.ok(ApiResult.ok(null, "Password changed successfully"));
    }
}
