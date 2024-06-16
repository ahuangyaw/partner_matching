package asia.huangzhitao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import asia.huangzhitao.model.domain.UserTeam;
import org.apache.ibatis.annotations.Mapper;

/**
* @author huang
* @description 针对表【user_team(用户部落关系)】的数据库操作Mapper
* @createDate 2024/01-14 11:45:06
* @Entity asia.huangzhitao.domain.UserTeam
*/
@Mapper
public interface UserTeamMapper extends BaseMapper<UserTeam> {

}




