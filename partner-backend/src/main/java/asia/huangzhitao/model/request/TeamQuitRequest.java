package asia.huangzhitao.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 团队辞职请求
 *
 * @author huang
 * @date 2024/01/22
 */
@Data
@ApiModel(value = "退出部落请求")
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = 1473299551300760408L;
    /**
     * id
     */
    @ApiModelProperty(value = "部落id")
    private Long teamId;

}
