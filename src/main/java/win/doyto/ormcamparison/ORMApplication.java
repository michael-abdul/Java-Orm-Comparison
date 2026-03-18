package win.doyto.ormcamparison;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * ORMApplication
 *
 * @author f0rb on 2023/2/23
 */
@SpringBootApplication
@EnableCaching
public class ORMApplication {
    public static void main(String[] args) {
        SpringApplication.run(ORMApplication.class);
    }
}
