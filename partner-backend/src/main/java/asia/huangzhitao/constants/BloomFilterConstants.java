package asia.huangzhitao.constants;


/**
 * 布隆过滤器常量
 *
 * @author huang
 * @date 2024/01/28
 */
public final class BloomFilterConstants {
    private BloomFilterConstants() {
    }

    /**
     * 用户布隆前缀
     */
    public static final String USER_BLOOM_PREFIX = "hwang:user:id:";
    /**
     * 部落布隆前缀
     */
    public static final String TEAM_BLOOM_PREFIX = "hwang:team:id:";
    /**
     * 博客布隆前缀
     */
    public static final String BLOG_BLOOM_PREFIX = "hwang:blog:id:";

    /**
     * 预先打开的最大包含记录
     */
    public static final int PRE_OPENED_MAXIMUM_INCLUSION_RECORD = 2000;

    /**
     * 预期包含记录
     */
    public static final int EXPECTED_INCLUSION_RECORD = 1000;

    /**
     * 散列函数数
     */
    public static final int HASH_FUNCTION_NUMBER = 2;
}
