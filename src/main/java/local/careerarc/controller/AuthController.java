package local.careerarc.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import local.careerarc.entity.Account;
import local.careerarc.entity.StudentProfile;
import local.careerarc.repository.AccountRepository;
import local.careerarc.repository.StudentProfileRepository;
import local.careerarc.service.AuthTokenService;
import local.careerarc.service.AuthTokenService.TokenPair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "CAREER_ARC_REFRESH";

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accounts;
    private final StudentProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokens;
    private final boolean secureCookie;

    public AuthController(AuthenticationManager authenticationManager, AccountRepository accounts,
                          StudentProfileRepository profiles, PasswordEncoder passwordEncoder, AuthTokenService tokens,
                          @Value("${app.auth.cookie-secure:false}") boolean secureCookie) {
        this.authenticationManager = authenticationManager;
        this.accounts = accounts;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<SessionView> login(@Valid @RequestBody LoginRequest input,
                                             @CookieValue(name = REFRESH_COOKIE, required = false) String oldRefreshToken) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(input.email().trim().toLowerCase(), input.password()));
        tokens.revoke(oldRefreshToken);
        Account account = accounts.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return session(tokens.create(account));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView register(@Valid @RequestBody RegisterRequest input) {
        String email = input.email().trim().toLowerCase();
        if (accounts.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("An account with this email already exists.");
        Account account = accounts.save(new Account(email, passwordEncoder.encode(input.password()), "STUDENT", input.name().trim()));
        profiles.save(new StudentProfile(account));
        return new AccountView(account.email, account.name, account.role);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        var rotated = tokens.rotate(refreshToken);
        if (rotated.isPresent()) return session(rotated.get());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
            .body(Map.of("status", 401, "error", "Your sign-in expired. Please sign in again."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        tokens.revoke(refreshToken);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
            .build();
    }

    @GetMapping("/me")
    public AccountView me(Authentication authentication) { return current(authentication.getName()); }

    private ResponseEntity<SessionView> session(TokenPair pair) {
        SessionView session = new SessionView(pair.email(), pair.name(), pair.role(), pair.accessToken());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie(pair.refreshToken(), pair.refreshLifetime()).toString())
            .body(session);
    }

    private AccountView current(String email) {
        return accounts.findByEmailIgnoreCase(email).map(account -> new AccountView(account.email, account.name, account.role))
            .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }

    private ResponseCookie refreshCookie(String token, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
            .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/auth").maxAge(maxAge).build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "").httpOnly(true).secure(secureCookie)
            .sameSite("Strict").path("/api/auth").maxAge(Duration.ZERO).build();
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank @Size(max = 120) String name, @Email @NotBlank String email,
                                  @NotBlank @Size(min = 10, max = 72) String password) {}
    public record AccountView(String email, String name, String role) {}
    public record SessionView(String email, String name, String role, String accessToken) {}
}
