package mywild.wildweather.base.user.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.framework.security.jwt.JwtUtils;
import mywild.wildweather.framework.web.BaseController;
import mywild.wildweather.base.user.logic.UserService;
import mywild.wildweather.base.user.web.dto.Tokens;
import mywild.wildweather.base.user.web.dto.UserInfo;
import mywild.wildweather.base.user.web.dto.UserLogin;
import mywild.wildweather.base.user.web.dto.UserRegister;
import mywild.wildweather.base.user.web.dto.UserUpdate;

@Tag(name = "Users", description = "User authentication and identity management.")
@RestController
public class UserController extends BaseController {

    @Autowired
    private UserService service;

    @Operation(summary = "Create a new User.")
    @PostMapping("/users/register")
    public Tokens registerUser(@RequestBody UserRegister dto) {
        return service.registerUser(dto);
    }

    @Operation(summary = "Check the login credentials of an existing User.")
    @PostMapping("/users/login")
    public Tokens loginUser(@RequestBody UserLogin dto) {
        return service.loginUser(dto);
    }

    @Operation(summary = "Refresh the User's JWT token.")
    @PostMapping("/users/refresh")
    public Tokens refreshUser(JwtAuthenticationToken jwtToken) {
        return service.refreshUser(JwtUtils.getUserIdFromJwt(jwtToken));
    }

    @Operation(summary = "Find User information based on the provided ID.")
    @GetMapping("/users/{userId}")
    public UserInfo getUser(JwtAuthenticationToken jwtToken, @PathVariable long userId) {
        return service.getUser(JwtUtils.getUserIdFromJwt(jwtToken), userId);
    }

    @Operation(summary = "Update User information based on the provided ID.")
    @PutMapping("/users/{userId}")
    public void updateUser(JwtAuthenticationToken jwtToken, @PathVariable long userId, @RequestBody UserUpdate dto) {
        service.updateUser(JwtUtils.getUserIdFromJwt(jwtToken), userId, dto);
    }

    // @Operation(summary = "Reset the password of an existing User.")
    // @PostMapping("/users/reset")
    // public void resetPassword(@RequestBody UserReset dto) {
    //     service.resetPassword(dto);
    // }

}
