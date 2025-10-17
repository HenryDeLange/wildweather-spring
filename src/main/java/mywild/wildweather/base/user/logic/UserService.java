package mywild.wildweather.base.user.logic;

import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import mywild.wildweather.framework.error.BadRequestException;
import mywild.wildweather.framework.error.ForbiddenException;
import mywild.wildweather.framework.security.jwt.TokenService;
import mywild.wildweather.framework.security.jwt.TokenType;
import mywild.wildweather.base.user.data.UserEntity;
import mywild.wildweather.base.user.data.UserRepository;
import mywild.wildweather.base.user.web.Tokens;
import mywild.wildweather.base.user.web.UserInfo;
import mywild.wildweather.base.user.web.UserLogin;
import mywild.wildweather.base.user.web.UserRegister;
import mywild.wildweather.base.user.web.UserUpdate;

@Validated
@Service
public class UserService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository repo;

    public @Valid Tokens registerUser(@Valid UserRegister dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        UserEntity entity = repo.save(UserMapper.INSTANCE.dtoToEntity(dto));
        var userId = entity.getId();
        return Tokens.builder()
            .userId(userId)
            .accessToken(tokenService.generateToken(TokenType.ACCESS, userId))
            .refreshToken(tokenService.generateToken(TokenType.REFRESH, userId))
            .build();
    }

    public @Valid Tokens loginUser(@Valid UserLogin dto) {
        Optional<UserEntity> foundEntity = repo.findByUsername(dto.getUsername().toLowerCase(Locale.getDefault()));
        if (!foundEntity.isPresent())
            throw new ForbiddenException("user.incorrect");
        if (!passwordEncoder.matches(dto.getPassword(), foundEntity.get().getPassword()))
            throw new ForbiddenException("user.incorrect");
        var userId = foundEntity.get().getId();
        return Tokens.builder()
            .userId(userId)
            .accessToken(tokenService.generateToken(TokenType.ACCESS, userId))
            .refreshToken(tokenService.generateToken(TokenType.REFRESH, userId))
            .build();
    }

    public @Valid Tokens refreshUser(long userId) {
        return Tokens.builder()
            .userId(userId)
            .accessToken(tokenService.generateToken(TokenType.ACCESS, userId))
            .refreshToken(tokenService.generateToken(TokenType.REFRESH, userId))
            .build();
    }

    public @Valid UserInfo getUser(long activeUserId, long userId) {
        Optional<UserEntity> entity = repo.findById(userId);
        if (entity.isPresent()) {
            return UserMapper.INSTANCE.entityToDto(entity.get());
        }
        else throw new BadRequestException("user.not-found");
    }

    public void updateUser(long activeUserId, long userId, @Valid UserUpdate dto) {
        if (activeUserId != userId)
            throw new ForbiddenException("user.not-found");
        Optional<UserEntity> entity = repo.findById(userId);
        if (entity.isPresent()) {
            repo.save(UserMapper.INSTANCE.dtoToExistingEntity(entity.get(), dto));
        }
        else throw new BadRequestException("user.not-found");
    }

    // public void resetPassword(@Valid UserReset dto) {
    //     // Somehow send new password to the user securely (email?)
    // }

}
