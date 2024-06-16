package asia.huangzhitao.mapper;

import asia.huangzhitao.model.domain.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author huang
* @description 针对表【message】的数据库操作Mapper
* @createDate 2024/01-21 17:39:30
* @Entity asia.huangzhitao.model.domain.Message
*/
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

}




