package mywild.wildweather.base.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString(callSuper = true)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserLogin {

    @NotNull
    @NotBlank
    @Size(min = 4, message = "user.username.too-short")
    private String username;

    @NotNull
    @NotBlank
    @Size(min = 4, message = "user.password.too-short")
    private String password;

}
