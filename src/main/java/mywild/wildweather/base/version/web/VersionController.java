package mywild.wildweather.base.version.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.framework.web.BaseController;
import mywild.wildweather.base.version.logic.VersionService;

@Tag(name = "Application Information", description = "Version information of the server.")
@RestController
public class VersionController extends BaseController {

    @Autowired
    private VersionService service;

    @Operation(summary = "Get server version and Git information.")
    @GetMapping("/version/server")
    public AppVersion getServerVersion() {
        return service.getServerVersion();
    }

}
