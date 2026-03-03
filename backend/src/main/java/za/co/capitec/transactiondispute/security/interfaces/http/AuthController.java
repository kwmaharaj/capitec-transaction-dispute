package za.co.capitec.transactiondispute.security.interfaces.http;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import za.co.capitec.transactiondispute.security.application.AuthService;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.shared.interfaces.http.ApiResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public Mono<ApiResponse<LoginResponse>> login(@RequestBody Mono<LoginRequest> body) {
        return body
                .flatMap(req -> auth.login(req.username(), req.password()))
                .map(res -> ApiResponse.ok(new LoginResponse(res.accessToken(), res.userId().toString(), res.roles())));
    }

    @PostMapping("/register")
    public Mono<ApiResponse<RegisterResponse>> register(@RequestBody Mono<RegisterRequest> body) {
        return body
                .flatMap(req -> auth.register(req.username(), req.password()))
                .map(res -> ApiResponse.ok(new RegisterResponse(res.userId().toString(), res.username(), res.roles())));
    }

    @PostMapping("/logout")
    public Mono<ApiResponse<Void>> logout(@AuthenticationPrincipal Jwt jwt) {
        // jwt.getId() maps to the "jti" claim , ie JwtClaimsSet.id
        return auth.logout(jwt.getId(), jwt.getExpiresAt())
                .thenReturn(ApiResponse.ok(null));
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String accessToken, String userId, java.util.List<String> roles) {}

    public record RegisterRequest(String username, String password) {}
    public record RegisterResponse(String userId, String username, java.util.List<String> roles) {}
}