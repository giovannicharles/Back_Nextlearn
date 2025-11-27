package com.nextcommunity.nextlearn.repository;


import com.nextcommunity.nextlearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findFirstByEmail(String email);
    boolean existsByEmail(String email);

<<<<<<< HEAD
    User findByUserrole(String admin);
=======
    Optional<User> findByUserrole(String admin);
>>>>>>> 465e66e (update backend NextLearn)
}
