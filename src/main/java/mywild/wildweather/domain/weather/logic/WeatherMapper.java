package mywild.wildweather.domain.weather.logic;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import mywild.wildweather.domain.weather.data.WeatherEntity;
import mywild.wildweather.domain.weather.web.WeatherDto;

@Mapper
public interface WeatherMapper {

    WeatherMapper INSTANCE = Mappers.getMapper(WeatherMapper.class);

    @Mapping(target = "id", ignore = true)
    public WeatherEntity dtoToEntity(WeatherDto dto);

    @Mapping(target = "id", ignore = true)
    public List<WeatherDto> entityToDtoAsList(Iterable<WeatherEntity> entityList);

}
