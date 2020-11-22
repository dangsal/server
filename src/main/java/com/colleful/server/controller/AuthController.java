package com.colleful.server.controller;

import com.colleful.server.domain.EmailVerification;
import com.colleful.server.domain.User;
import com.colleful.server.dto.UserDto.*;
import com.colleful.server.exception.AlreadyExistResourceException;
import com.colleful.server.exception.InvalidCodeException;
import com.colleful.server.exception.NotFoundResourceException;
import com.colleful.server.exception.NotMatchedPasswordException;
import com.colleful.server.exception.NotSentEmailException;
import com.colleful.server.exception.NotVerifiedEmailException;
import com.colleful.server.security.JwtProvider;
import com.colleful.server.service.DepartmentService;
import com.colleful.server.service.EmailVerificationService;
import com.colleful.server.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final EmailVerificationService emailVerificationService;
    private final JwtProvider provider;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService,
        DepartmentService departmentService,
        EmailVerificationService emailVerificationService,
        JwtProvider provider,
        PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.emailVerificationService = emailVerificationService;
        this.provider = provider;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/join")
    public Response join(@RequestBody Request request) {
        User user = request.toEntity(passwordEncoder, departmentService);
        EmailVerification emailVerification =
            emailVerificationService.getEmailVerificationInfo(request.getEmail())
                .orElseThrow(() -> new NotVerifiedEmailException("인증되지 않은 이메일입니다."));

        if (!emailVerification.getIsChecked()) {
            throw new NotVerifiedEmailException("인증되지 않은 이메일입니다.");
        }

        if (userService.isExist(request.getEmail())) {
            throw new AlreadyExistResourceException("중복된 이메일입니다.");
        }

        if (!emailVerificationService.deleteVerificationInfo(emailVerification.getId())) {
            throw new RuntimeException("다시 시도해 주세요.");
        }

        if (!userService.join(user)) {
            throw new RuntimeException("회원가입에 실패했습니다.");
        }

        return new Response(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = userService.getUserInfoByEmail(request.getEmail())
            .orElseThrow(() -> new NotFoundResourceException("가입되지 않은 유저입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new NotMatchedPasswordException("비밀번호가 일치하지 않습니다.");
        }

        String token = provider.createToken(user.getEmail(), user.getId(), user.getRoles());
        return new LoginResponse(token);
    }

    @PostMapping("/join/email")
    public ResponseEntity<?> sendEmailForRegister(@RequestBody EmailRequest request) {
        if (userService.isExist(request.getEmail())) {
            throw new AlreadyExistResourceException("이미 가입된 유저입니다.");
        }

        if (!emailVerificationService.sendEmail(request.getEmail())) {
            throw new NotSentEmailException("이메일이 발송되지 않았습니다.");
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @PostMapping("/password/email")
    public ResponseEntity<?> sendEmailForPassword(@RequestBody EmailRequest request) {
        if (!userService.isExist(request.getEmail())) {
            throw new NotFoundResourceException("가입되지 않은 유저입니다.");
        }

        if (!emailVerificationService.sendEmail(request.getEmail())) {
            throw new NotSentEmailException("이메일이 발송되지 않았습니다.");
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody LoginRequest request) {
        User user = userService.getUserInfoByEmail(request.getEmail())
            .orElseThrow(() -> new NotFoundResourceException("가입되지 않은 유저입니다."));
        EmailVerification emailVerification =
            emailVerificationService.getEmailVerificationInfo(request.getEmail())
                .orElseThrow(() -> new NotVerifiedEmailException("인증되지 않은 이메일입니다."));

        if (!emailVerification.getIsChecked()) {
            throw new NotVerifiedEmailException("인증되지 않은 이메일입니다.");
        }

        if (!emailVerificationService.deleteVerificationInfo(emailVerification.getId())) {
            throw new RuntimeException("다시 시도해 주세요.");
        }

        if (!userService.changePassword(user, passwordEncoder.encode(request.getPassword()))) {
            throw new RuntimeException("비밀번호 변경에 실패했습니다.");
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @PatchMapping("/check")
    public ResponseEntity<?> check(@RequestBody EmailRequest request) {
        if (!emailVerificationService.check(request.getEmail(), request.getCode())) {
            throw new InvalidCodeException("인증번호가 다릅니다.");
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}