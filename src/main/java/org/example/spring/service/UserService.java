package org.example.spring.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.example.spring.repo.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;

    public void save(User user) {
        userRepo.save(user);
    }

    public User findByUsername(String username) {
        return null; // todo user repo
    }

    public List<User> findAll(){
        return userRepo.findAll();
    }

}
