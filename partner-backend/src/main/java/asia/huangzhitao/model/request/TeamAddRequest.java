package asia.huangzhitao.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 团队添加请求
 *
 * @author huang
 * @date 2024/01/22
 */
@Data
@ApiModel(value = "添加部落请求")
public class TeamAddRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 部落名称
     */
    @ApiModelProperty(value = "部落名称")
    private String name;

    /**
     * 描述
     */
    @ApiModelProperty(value = "描述")
    private String description;

    /**
     * 最大人数
     */
    @ApiModelProperty(value = "最大人数")
    private Integer maxNum;

    /**
     * 过期时间
     */
    @ApiModelProperty(value = "过期时间")
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    @ApiModelProperty(value = "状态0 - 公开，1 - 私有，2 - 加密")
    private Integer status;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码")
    private String password;

}
