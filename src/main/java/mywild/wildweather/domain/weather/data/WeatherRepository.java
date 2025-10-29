package mywild.wildweather.domain.weather.data;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;

@Repository
public interface WeatherRepository extends CrudRepository<WeatherEntity, Long> {

    @Query("""
        SELECT DISTINCT w.station
        FROM "weather" w
        """)
    List<String> findStations();

    @Query("""
        SELECT *
        FROM "weather" w
        WHERE
            (:category IS NULL OR w.category = :category)
            AND (:station IS NULL OR w.station = :station)
            AND (:startDate IS NULL OR w.date >= :startDate)
            AND (:endDate IS NULL OR w.date <= :endDate)
            AND (:startMonth IS NULL OR MONTH(w.date) >= :startMonth)
            AND (:endMonth IS NULL OR MONTH(w.date) <= :endMonth)
        ORDER BY w.date ASC, w.station ASC, w.category ASC
        """)
    List<WeatherEntity> searchWeather(
        @Param("category") WeatherCategory category,
        @Param("station") String station,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("startMonth") Integer startMonth,
        @Param("endMonth") Integer endMonth);

    List<WeatherEntity> findAllByStationOrderByDateAscCategoryAsc(
        String station);
        
    List<WeatherEntity> findAllByDateAndStationOrderByDateAscCategoryAsc(
        LocalDate date,
        String station);

    WeatherEntity findByDateAndStationAndCategory(
        LocalDate date,
        String station,
        WeatherCategory category);
    
    @Query("SELECT w.date FROM \"weather\" w WHERE w.station = :station ORDER BY date DESC LIMIT 1")
    LocalDate findTopDateByStation(
        @Param("station") String station);

}
