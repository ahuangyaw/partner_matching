package asia.huangzhitao.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 团队加入请求
 *
 * @author huang
 * @date 2024/01/22
 */
@Data
@ApiModel(value = "加入部落请求")
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = -3755024144750907374L;
    /**
     * id
     */
    @ApiModelProperty(value = "部落id", required = true)
    private Long teamId;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码")
    private String password;

}
