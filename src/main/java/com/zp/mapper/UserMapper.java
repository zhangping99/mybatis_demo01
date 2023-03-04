package com.zp.mapper;

import com.zp.domain.User;

import java.util.List;

public interface UserMapper {
    List<User> findAll(User user);
    List<User> findByIds(List<Integer> ids);
    void save(User user);
}
