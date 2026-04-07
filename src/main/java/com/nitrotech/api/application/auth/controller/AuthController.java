package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.application.auth.request.*;
import com.nitrotech.api.domain.auth.dto.*;
import com.nitrotech.api.domain.auth.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
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

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase,
                          LogoutUseCase logoutUseCase, GetProfileUseCase getProfileUseCase,
                          UpdateProfileUseCase updateProfileUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          ForgotPasswordUseCase forgotPasswordUseCase,
                          ResetPasswordUseCase resetPasswordUseCase,
                          VerifyEmailUseCase verifyEmailUseCase,
                          ResendVerificationUseCase resendVerificationUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.resendVerificationUseCase = resendVerificationUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResult result = registerUseCase.execute(
                new RegisterCommand(request.name(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResult>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResult result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));

        // Tạo session mới, lưu email để Spring Security nhận diện
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("email", result.user().email());
        // Lưu index để có thể tìm session theo email (dùng cho logout-all, reset password)
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, result.user().email());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        logoutUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal String email,
            HttpServletRequest request
    ) {
        // Invalidate session hiện tại — Spring Session Redis tự xử lý các session khác
        // nếu dùng SessionRegistry hoặc findByPrincipalName
        logoutUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out from all devices"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResponse.ok(null, "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(request.token());
        return ResponseEntity.ok(ApiResponse.ok(null, "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        resendVerificationUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResponse.ok(null, "Verification email sent"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileData>> me(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.ok(getProfileUseCase.executeByEmail(email)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileData>> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileData current = getProfileUseCase.executeByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(updateProfileUseCase.execute(
                new UpdateProfileCommand(current.id(), request.name(), request.phone(), request.avatar()))));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UserProfileData current = getProfileUseCase.executeByEmail(email);
        changePasswordUseCase.execute(
                new ChangePasswordCommand(current.id(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }
}
