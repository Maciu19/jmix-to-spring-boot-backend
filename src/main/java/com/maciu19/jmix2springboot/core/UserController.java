package com.maciu19.jmix2springboot.core;

import com.maciu19.jmix2springboot.core.entity.User;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UserRepository extends ListCrudRepository<User, UUID> {
}

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public void createUser() {
        userRepository.save(new User());
    }

    @PatchMapping("/users/{userId}")
    public void updateUser(@PathVariable("userId") UUID id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            user.get().setLastModifiedBy("system_updated");
            userRepository.save(user.get());
        }
    }

    @DeleteMapping("/users/{userId}")
    public void deleteUser(@PathVariable("userId") UUID id) {
        userRepository.deleteById(id);
    }
}
