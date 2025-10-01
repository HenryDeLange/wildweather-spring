package mywild.wildweather.domain.weather.data;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;


@Repository
public interface WeatherRepository extends CrudRepository<WeatherEntity, Long> {

    List<WeatherEntity> findAllByDate(LocalDate date);

    List<WeatherEntity> findAllByDateAndStation(LocalDate date, String station);

    WeatherEntity findByDateAndStationAndCategory(LocalDate date, String station, WeatherCategory category);

}
