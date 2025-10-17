package mywild.wildweather.base.version.logic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.base.version.web.AppVersion;

@Slf4j
@Validated
@Service
public class VersionService {

    @Value("${mywild.app.version}")
    private String appVersion;

    @Value("${git.branch}")
    private String branch;

    @Value("${git.commit.id.abbrev}")
    private String commitId;

    @Value("${git.commit.time}")
    private String commitTime;

    @Value("${git.build.time}")
    private String buildTime;

    public @Valid AppVersion getServerVersion() {
        return AppVersion.builder()
            .appVersion(appVersion)
            .branch(branch)
            .commitId(commitId)
            .commitTime(commitTime)
            .buildTime(buildTime)
            .build();
    }

}
