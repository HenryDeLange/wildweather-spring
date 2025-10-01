package mywild.wildweather.user.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Tokens {

    @NotNull
    private long userId;

    @NotNull
    @NotBlank
    private String accessToken;

    @NotNull
    @NotBlank
    private String refreshToken;

}
