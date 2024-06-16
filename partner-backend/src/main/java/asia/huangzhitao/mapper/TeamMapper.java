package asia.huangzhitao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import asia.huangzhitao.model.domain.Team;
import org.apache.ibatis.annotations.Mapper;

/**
* @author huang
* @description 针对表【team(部落)】的数据库操作Mapper
* @createDate 2024/01-12 19:33:37
* @Entity asia.huangzhitao.model.domain.Team
 */
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

}




