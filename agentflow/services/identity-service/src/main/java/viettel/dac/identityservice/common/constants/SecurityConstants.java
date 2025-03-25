package viettel.dac.identityservice.common.constants;

/**
 * Constants related to security configuration and JWT tokens
 */
public final class SecurityConstants {

    // JWT Token constants
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String AUTHORITIES_CLAIM = "authorities";
    public static final String USER_ID_CLAIM = "userId";
    public static final String TOKEN_TYPE_CLAIM = "tokenType";
    public static final String EMAIL_CLAIM = "email";
    public static final String USERNAME_CLAIM = "username";
    public static final String TOKEN_ID_CLAIM = "jti";
    public static final String SCOPES_CLAIM = "scopes";

    // Token types
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";
    public static final String MFA_TOKEN_TYPE = "mfa";

    // Authentication endpoints
    public static final String AUTH_ENDPOINT = "/auth";
    public static final String LOGIN_ENDPOINT = AUTH_ENDPOINT + "/login";
    public static final String REGISTER_ENDPOINT = AUTH_ENDPOINT + "/register";
    public static final String REFRESH_TOKEN_ENDPOINT = AUTH_ENDPOINT + "/refresh";
    public static final String LOGOUT_ENDPOINT = AUTH_ENDPOINT + "/logout";
    public static final String VERIFY_EMAIL_ENDPOINT = AUTH_ENDPOINT + "/verify-email/**";
    public static final String FORGOT_PASSWORD_ENDPOINT = AUTH_ENDPOINT + "/forgot-password";
    public static final String RESET_PASSWORD_ENDPOINT = AUTH_ENDPOINT + "/reset-password";
    public static final String VERIFY_MFA_ENDPOINT = AUTH_ENDPOINT + "/verify-mfa";

    // Public endpoints
    public static final String[] PUBLIC_ENDPOINTS = {
            LOGIN_ENDPOINT,
            REGISTER_ENDPOINT,
            REFRESH_TOKEN_ENDPOINT,
            VERIFY_EMAIL_ENDPOINT,
            FORGOT_PASSWORD_ENDPOINT,
            RESET_PASSWORD_ENDPOINT,
            VERIFY_MFA_ENDPOINT,
            "/actuator/health",
            "/actuator/info",
            "/api-docs/**",
            "/swagger-ui/**"
    };

    // Password policy
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_ATTEMPT_LOCKOUT_MINUTES = 15;

    // Token signing algorithm
    public static final String TOKEN_SIGNING_ALGORITHM = "HS512";

    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }
}