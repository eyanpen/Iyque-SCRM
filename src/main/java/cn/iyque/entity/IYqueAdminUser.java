package cn.iyque.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 后台管理用户 (与 iyque_user / 企微员工 不同)。
 *
 * <p>护城河 SCRM 原始设计只支持 conf.yaml 里的单个 admin 账号。为演示或多人协作，
 * 允许在 DB 里追加更多管理员，共享同一份 admin 权限（当前无 role 区分）。
 *
 * <p>关注点：
 * <ul>
 *   <li>登录仍走 {@link cn.iyque.controller.IYqueLoginController#login} 一个端点</li>
 *   <li>Controller 首先比对 conf.yaml admin，未命中再查本表</li>
 *   <li>密码目前以明文存储（与 conf.yaml 风格一致；生产建议后续改 BCrypt）</li>
 * </ul>
 */
@Entity(name = "iyque_admin_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IYqueAdminUser {

    @Id
    @GeneratedValue(generator = "snowflakeIdGenerator")
    @GenericGenerator(
            name = "snowflakeIdGenerator",
            strategy = "cn.iyque.utils.SnowFlakeUtils"
    )
    private Long id;

    /** 登录名（唯一）。 */
    @Column(unique = true, nullable = false, length = 64)
    private String username;

    /** 明文密码。 */
    @Column(nullable = false, length = 128)
    private String password;

    /** 显示名 / 备注。 */
    @Column(length = 64)
    private String displayName;

    private Date createTime;

    /** 逻辑删除标识: 0=有效, 1=已删除。 */
    private Integer delFlag;
}
