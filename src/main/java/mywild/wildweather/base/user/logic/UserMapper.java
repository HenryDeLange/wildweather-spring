package mywild.wildweather.base.user.logic;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import mywild.wildweather.base.user.data.entity.UserEntity;
import mywild.wildweather.base.user.web.dto.UserInfo;
import mywild.wildweather.base.user.web.dto.UserRegister;
import mywild.wildweather.base.user.web.dto.UserUpdate;

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
