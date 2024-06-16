package asia.huangzhitao.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 团队查询请求
 *
 * @author huang
 * @date 2024/01/22
 */
@Data
@ApiModel(value = "部落搜索请求")
public class TeamQueryRequest implements Serializable {
    private static final long serialVersionUID = 9111600376030432964L;
    /**
     * id
     */
    @ApiModelProperty(value = "部落id")
    private Long id;

    /**
     * id 列表
     */
    @ApiModelProperty(value = "id列表")
    private List<Long> idList;

    /**
     * 搜索关键词（同时对部落名称和描述搜索）
     */
    @ApiModelProperty(value = "搜索关键词")
    private String searchText;

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
     * 用户id
     */
    @ApiModelProperty(value = "队长id")
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    @ApiModelProperty(value = "状态0 - 公开，1 - 私有，2 - 加密")
    private Integer status;

}
