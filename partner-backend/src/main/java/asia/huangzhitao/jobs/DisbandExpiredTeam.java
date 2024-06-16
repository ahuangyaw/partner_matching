package asia.huangzhitao.jobs;

import lombok.extern.log4j.Log4j2;
import asia.huangzhitao.model.domain.Team;
import asia.huangzhitao.service.TeamService;
import org.quartz.JobExecutionContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.quartz.QuartzJobBean;
import reactor.util.annotation.NonNull;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static asia.huangzhitao.constants.RedissonConstant.DEFAULT_LEASE_TIME;
import static asia.huangzhitao.constants.RedissonConstant.DEFAULT_WAIT_TIME;
import static asia.huangzhitao.constants.RedissonConstant.DISBAND_EXPIRED_TEAM_LOCK;

/**
 * 解散过期团队
 *
 * @author huang
 * @date 2024/01/28
 */
@Log4j2
public class DisbandExpiredTeam extends QuartzJobBean {
    /**
     * redisson客户
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 团队服务
     */
    @Resource
    private TeamService teamService;

    /**
     * 执行内部
     *
     * @param context 上下文
     */
    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) {
        RLock lock = redissonClient.getLock(DISBAND_EXPIRED_TEAM_LOCK);
        try {
            if (lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.MICROSECONDS)) {
                log.info("开始删除过期部落");
                long begin = System.currentTimeMillis();
                List<Team> teamList = teamService.list();
                ZoneId zoneId = ZoneId.systemDefault();
                for (Team team : teamList) {
                    Date time = team.getExpireTime();
                    if (time == null) {
                        continue;
                    }
                    Instant instant = time.toInstant();
                    LocalDateTime expireTime = LocalDateTime.ofInstant(instant, zoneId);
                    if (LocalDateTime.now().isAfter(expireTime)) {
                        teamService.deleteTeam(team.getId(), null, true);
                    }
                }
                long end = System.currentTimeMillis();
                log.info("删除过期部落结束，耗时" + (end - begin));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}