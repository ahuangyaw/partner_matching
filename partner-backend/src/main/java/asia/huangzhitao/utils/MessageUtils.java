package asia.huangzhitao.utils;

import lombok.extern.log4j.Log4j2;
import asia.huangzhitao.properties.SuperProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 消息utils
 *
 * @author huang
 * @date 2024/01/25
 */
@Log4j2
@Component
public class MessageUtils {
    private static SuperProperties hwangProperties;

    @Resource
    private SuperProperties tempProperties;


    /**
     * 发送消息
     *
     * @param phoneNum 电话号码
     * @param code     密码
     */
    public static void sendMessage(String phoneNum, String code) {
        if (hwangProperties.isUseShortMessagingService()) {
            SMSUtils.sendMessage(phoneNum, code);
        } else {
            log.info("验证码: " + code);
        }
    }

    /**
     * init属性
     */
    @PostConstruct
    public void initProperties() {
        hwangProperties = tempProperties;
    }
}
