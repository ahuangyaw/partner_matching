package asia.huangzhitao.mapper;

import asia.huangzhitao.model.domain.Friends;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author huang
* @description 针对表【friends(好友申请管理表)】的数据库操作Mapper
* @createDate 2024/01-18 14:10:45
* @Entity asia.huangzhitao.model.domain.Friends
*/
@Mapper
public interface FriendsMapper extends BaseMapper<Friends> {

}




