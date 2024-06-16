package asia.huangzhitao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import asia.huangzhitao.mapper.TagMapper;
import asia.huangzhitao.model.domain.Tag;
import asia.huangzhitao.service.TagService;
import org.springframework.stereotype.Service;

/**
 * @author huang
 * @description 针对表【tag】的数据库操作Service实现
 * @createDate 2024/01-07 19:05:01
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
}




