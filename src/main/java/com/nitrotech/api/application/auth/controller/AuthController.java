package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.application.auth.request.LoginRequest;
import com.nitrotech.api.application.auth.request.RegisterRequest;
import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.LoginCommand;
import com.nitrotech.api.domain.auth.dto.RegisterCommand;
import com.nitrotech.api.domain.auth.usecase.LoginUseCase;
import com.nitrotech.api.domain.auth.usecase.RegisterUseCase;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterCommand command = new RegisterCommand(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(registerUseCase.execute(command)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResult>> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(loginUseCase.execute(command)));
    }
}
