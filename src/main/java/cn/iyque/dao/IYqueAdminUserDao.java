package cn.iyque.dao;

import cn.iyque.entity.IYqueAdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IYqueAdminUserDao extends JpaRepository<IYqueAdminUser, Long> {

    /**
     * 按用户名精确查找 (只返回未软删除的记录)。
     */
    Optional<IYqueAdminUser> findByUsernameAndDelFlag(String username, Integer delFlag);
}
