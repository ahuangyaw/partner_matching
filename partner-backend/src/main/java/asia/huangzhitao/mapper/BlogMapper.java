package asia.huangzhitao.mapper;

import asia.huangzhitao.model.domain.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author huang
* @description 针对表【blog】的数据库操作Mapper
* @createDate 2024/01-03 15:54:34
* @Entity asia.huangzhitao.model.domain.Blog
 */
@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

}




