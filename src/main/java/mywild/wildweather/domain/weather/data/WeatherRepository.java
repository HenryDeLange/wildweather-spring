package mywild.wildweather.domain.weather.data;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;


@Repository
public interface WeatherRepository extends CrudRepository<WeatherEntity, Long> {

    @Query("""
        SELECT *
        FROM "weather" w
        WHERE (:startDate IS NULL OR w.date >= :startDate)
        AND (:endDate IS NULL OR w.date <= :endDate)
        ORDER BY w.date, w.station, w.category
        """)
    List<WeatherEntity> findAllWithinDateRange(
        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<WeatherEntity> findAllByDateAndStationOrderByDateDescCategoryAsc(LocalDate date, String station);
    
    WeatherEntity findByDateAndStationAndCategory(LocalDate date, String station, WeatherCategory category);

}
