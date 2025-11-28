-- USERS
CREATE TABLE IF NOT EXISTS "users" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    description TEXT,
    UNIQUE(username)
);

-- WEATHER (DAILY)
CREATE TABLE IF NOT EXISTS "weather" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station VARCHAR(64) NOT NULL,           -- The station's name
    date DATE NOT NULL,                     -- CSV Column: Date
    category VARCHAR(1) NOT NULL,           -- Unnamed first column with values: Low, Average, High
    temperature DECFLOAT,                   -- CSV Column: Outdoor Temperature (°C)
    wind_speed DECFLOAT,                    -- CSV Column: Wind Speed (km/hr)
    wind_max DECFLOAT,                      -- CSV Column: Max Daily Gust (km/hr)
    wind_direction VARCHAR(3),              -- CSV Column: Wind Direction (°)
    rain_rate DECFLOAT,                     -- CSV Column: Rain Rate (mm/hr) / Column: Hourly Rain (mm/hr)
    rain_daily DECFLOAT,                    -- CSV Column: Daily Rain (mm)
    pressure DECFLOAT,                      -- CSV Column: Relative Pressure (hPa)
    humidity DECFLOAT,                      -- CSV Column: Humidity (%)
    uv_radiation_index DECFLOAT,            -- CSV Column: Ultra-Violet Radiation Index
    missing DECFLOAT NOT NULL,              -- The percentage of records missing during the day
    UNIQUE (date, station, category)
);

CREATE INDEX IF NOT EXISTS idx_weather_date ON "weather"(date);
