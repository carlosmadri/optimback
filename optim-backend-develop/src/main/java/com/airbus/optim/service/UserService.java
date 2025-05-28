package com.airbus.optim.service;

import com.airbus.optim.dto.UserPropertiesDTO;
import com.airbus.optim.entity.User;
import com.airbus.optim.repository.UserRepository;
import com.airbus.optim.utils.Utils;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Lazy
    @Autowired
    Utils utils;

    @Autowired
    UserRepository userRepository;

    public UserPropertiesDTO getUserProperties(String userSelected) {

        Optional<User> userOpt = userRepository.findOneByEmailIgnoreCase(userSelected);

        return new UserPropertiesDTO(
                userOpt.get().getRoles(),
                userOpt.get().getSiglum(),
                utils.getSiglumVisibilityList(userSelected)
        );
    }
}
