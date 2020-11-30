package com.colleful.server.domain.user;

import com.colleful.server.domain.emailverification.EmailVerification;
import com.colleful.server.global.exception.AlreadyExistResourceException;
import com.colleful.server.global.exception.InvalidCodeException;
import com.colleful.server.global.exception.NotFoundResourceException;
import com.colleful.server.global.exception.NotMatchedPasswordException;
import com.colleful.server.global.exception.NotSentEmailException;
import com.colleful.server.global.exception.NotVerifiedEmailException;
import com.colleful.server.global.security.JwtProvider;
import com.colleful.server.domain.department.DepartmentService;
import com.colleful.server.domain.emailverification.EmailVerificationService;
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
    public UserDto.Response join(@RequestBody UserDto.Request request) {
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

        return new UserDto.Response(user);
    }

    @PostMapping("/login")
    public UserDto.LoginResponse login(@RequestBody UserDto.LoginRequest request) {
        User user = userService.getUserInfoByEmail(request.getEmail())
            .orElseThrow(() -> new NotFoundResourceException("가입되지 않은 유저입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new NotMatchedPasswordException("비밀번호가 일치하지 않습니다.");
        }

        String token = provider.createToken(user.getEmail(), user.getId(), user.getRoles());
        return new UserDto.LoginResponse(token);
    }

    @PostMapping("/join/email")
    public ResponseEntity<?> sendEmailForRegister(@RequestBody UserDto.EmailRequest request) {
        if (userService.isExist(request.getEmail())) {
            throw new AlreadyExistResourceException("이미 가입된 유저입니다.");
        }

        if (!emailVerificationService.sendEmail(request.getEmail())) {
            throw new NotSentEmailException("이메일이 발송되지 않았습니다.");
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @PostMapping("/password/email")
    public ResponseEntity<?> sendEmailForPassword(@RequestBody UserDto.EmailRequest request) {
        if (!userService.isExist(request.getEmail())) {
            throw new NotFoundResourceException("가입되지 않은 유저입니다.");
        }

        if (!emailVerificationService.sendEmail(request.getEmail())) {
            throw new NotSentEmailException("이메일이 발송되지 않았습니다.");
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody UserDto.LoginRequest request) {
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
    public ResponseEntity<?> check(@RequestBody UserDto.EmailRequest request) {
        if (!emailVerificationService.check(request.getEmail(), request.getCode())) {
            throw new InvalidCodeException("인증번호가 다릅니다.");
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}
