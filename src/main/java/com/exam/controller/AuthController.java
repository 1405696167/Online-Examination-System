package com.exam.controller;

import com.exam.dto.Requests.LoginRequest;
import com.exam.dto.Responses.LoginResponse;
import com.exam.config.JwtUtil;
import com.exam.model.User;
import com.exam.model.UserRole;
import com.exam.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository users, JwtUtil jwtUtil) {
        this.users = users;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = request.role() == UserRole.STUDENT
                ? users.findFirstByStudentNumberOrderByIdAsc(request.username()).orElseThrow()
                : users.findFirstByUsernameAndRoleOrderByIdAsc(request.username(), request.role()).orElseThrow();
        if (!user.getPassword().equals(request.password()) || user.getRole() != request.role()) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return LoginResponse.from(user, jwtUtil.createToken(user));
    }
}
