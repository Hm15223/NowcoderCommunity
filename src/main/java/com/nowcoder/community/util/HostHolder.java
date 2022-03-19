package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于代替session对象
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();    //threadlocal是线程隔离的，以线程为key存取值

    public void setUser(User user){
        users.set(user);
    }

    public User getUser(){
            return users.get();
    }

    //清理用户数据，线程相关
    public void clear(){
        users.remove();
    }
}
