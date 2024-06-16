package asia.huangzhitao.constants;


/**
 * 聊天常量
 *
 * @author huang
 * @date 2024/01/22
 */
public final class ChatConstant {
    private ChatConstant() {
    }

    /**
     * 私聊
     */
    public static final int PRIVATE_CHAT = 1;

    /**
     * 部落群聊
     */

    public static final int TEAM_CHAT = 2;
    /**
     * 大厅聊天
     */
    public static final int HALL_CHAT = 3;

    /**
     * 缓存聊天大厅
     */
    public static final String CACHE_CHAT_HALL = "hwang:chat:chat_records:chat_hall";

    /**
     * 缓存私人聊天
     */
    public static final String CACHE_CHAT_PRIVATE = "hwang:chat:chat_records:chat_private:";

    /**
     * 缓存聊天团队
     */
    public static final String CACHE_CHAT_TEAM = "hwang:chat:chat_records:chat_team:";
}
