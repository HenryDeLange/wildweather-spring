package mywild.wildweather.base.version.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersion {

    @NotNull
    @NotBlank
    private String appVersion;

    @NotNull
    @NotBlank
    private String branch;

    @NotNull
    @NotBlank
    private String commitId;

    @NotNull
    @NotBlank
    private String commitTime;

    @NotNull
    @NotBlank
    private String buildTime;

}
