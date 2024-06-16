package asia.huangzhitao.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author huang
 */
@Data
@ConfigurationProperties(prefix = "hwang.qiniu")
@Component
public class QiNiuProperties {

    private String accessKey;

    private String secretKey;

    private String bucket;

    private String url;
}
