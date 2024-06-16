package asia.huangzhitao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 应用程序
 *
 * @author huang
 * @date 2024/01/25
 */
@SpringBootApplication
@EnableRedisHttpSession
@EnableAspectJAutoProxy
public class PartnerBackendApplication {
    protected PartnerBackendApplication() {
    }

    /**
     * 程序入口
     *
     * @param args args
     */
    public static void main(String[] args) {
        SpringApplication.run(PartnerBackendApplication.class, args);
    }
}

