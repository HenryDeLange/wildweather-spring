package mywild.wildweather.user.logic;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import mywild.wildweather.user.data.UserEntity;
import mywild.wildweather.user.web.UserInfo;
import mywild.wildweather.user.web.UserRegister;
import mywild.wildweather.user.web.UserUpdate;

@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", ignore = true)
    UserEntity dtoToEntity(UserRegister dto);

    UserInfo entityToDto(UserEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    UserEntity dtoToExistingEntity(@MappingTarget UserEntity entity, UserUpdate dto);

}
