package mywild.wildweather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication(scanBasePackageClasses = SpringBootApp.class, exclude = { HibernateJpaAutoConfiguration.class,
        RedisAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class, MongoAutoConfiguration.class,
        FlywayAutoConfiguration.class, JmxAutoConfiguration.class, MailSenderAutoConfiguration.class,
        RabbitAutoConfiguration.class, MongoReactiveAutoConfiguration.class, CassandraAutoConfiguration.class,
        Neo4jAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
@EnableScheduling
@EnableAsync
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

    @Component
    public class ApplicationLoader implements ApplicationRunner, SmartLifecycle {

        @Value("${mywild.app.dev-mode}")
        private boolean devMode;

        @Value("${server.port:8080}")
        private int port;

        @Value("${server.servlet.context-path:}")
        private String contextPath;

        @Value("${spring.h2.console.enabled}")
        private boolean h2ConsoleEnabled;

        @Value("${spring.h2.console.path}")
        private String h2Console;

        private boolean isRunning = false;

        @Override
        public void run(ApplicationArguments args) throws Exception {
            if (isRunning) {
                log.info("------>>> READY <<<------");
                if (devMode) {
                    log.info("http://localhost:{}{}", port, contextPath);
                    log.info("http://localhost:{}{}/swagger-ui/index.html", port, contextPath);
                    if (h2ConsoleEnabled) {
                        log.info("http://localhost:{}{}", port, h2Console);
                    }
                }
            }
        }

        @Override
        public void start() {
            log.info(">>> Starting the application >>>");
            isRunning = true;
        }

        @Override
        public void stop() {
            log.info("<<< Stopping the application <<<");
            isRunning = false;
        }

        @Override
        public boolean isRunning() {
            return isRunning;
        }

        @Override
        public void stop(@NonNull Runnable callback) {
            stop();
            callback.run();
        }

    }

}
