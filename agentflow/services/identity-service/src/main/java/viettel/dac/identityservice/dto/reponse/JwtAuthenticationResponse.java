package viettel.dac.identityservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtAuthenticationResponse {
    private String token;
    private String tokenType = "Bearer";

    public JwtAuthenticationResponse(String token) {
        this.token = token;
    }
}