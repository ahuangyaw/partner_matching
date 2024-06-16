package asia.huangzhitao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import asia.huangzhitao.mapper.UserTeamMapper;
import asia.huangzhitao.model.domain.UserTeam;
import asia.huangzhitao.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
 * @author huang
 * @description 针对表【user_team(用户部落关系)】的数据库操作Service实现
 * @createDate 2024/01-14 11:45:06
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

}




