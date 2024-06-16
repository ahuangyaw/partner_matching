package asia.huangzhitao.utils;

import java.util.Random;

import static asia.huangzhitao.constants.SystemConstants.MAXIMUM_VERIFICATION_CODE_NUM;
import static asia.huangzhitao.constants.SystemConstants.MINIMUM_VERIFICATION_CODE_NUM;

/**
 * 验证码生成工具
 *
 * @author huang
 * @date 2024/01/22
 */
public final class ValidateCodeUtils {
    private ValidateCodeUtils() {
    }

    /**
     * 生成验证代码
     *
     * @return {@link Integer}
     */
    public static Integer generateValidateCode() {
        int code = new Random().nextInt(MAXIMUM_VERIFICATION_CODE_NUM); //生成随机数，最大为999999
        if (code < MINIMUM_VERIFICATION_CODE_NUM) {
            code = code + MINIMUM_VERIFICATION_CODE_NUM; //保证随机数为6位数字
        }
        return code;
    }
}
