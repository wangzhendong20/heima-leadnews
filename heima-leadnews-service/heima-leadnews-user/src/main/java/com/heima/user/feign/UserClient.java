package com.heima.user.feign;

import com.heima.apis.user.IUserClient;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

public class UserClient implements IUserClient {

    @Autowired
    private ApUserService apUserService;

    @GetMapping("/api/v1/user/{id}")
    @Override
    public ApUser findUserById(Integer id) {
        return apUserService.getById(id);
    }
}
