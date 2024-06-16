package asia.huangzhitao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import asia.huangzhitao.model.domain.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户映射器
 *
 * @author huang
 * @description 针对表【user】的数据库操作Mapper
 * @createDate 2024/01-07 19:56:01
 * @Entity asia.huangzhitao.model.domain.User
 * @date 2024/01/25
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 收到随机用户
     *
     * @return {@link List}<{@link User}>
     */
    List<User> getRandomUser();
}




