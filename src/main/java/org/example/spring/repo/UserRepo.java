package org.example.spring.repo;

import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    @Query("SELECT u.userBotId FROM User u WHERE u.userBotId IS NOT NULL")
    Set<Long> getAllUserBotIds();
}
