package com.example.minicom.controller;

import com.example.minicom.model.User;
import com.example.minicom.repo.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepo;
    public UserController(UserRepository userRepo) { this.userRepo = userRepo; }

    @PostMapping
    public User createUser(@RequestBody User u) { return userRepo.save(u); }

    @GetMapping
    public List<User> listUsers() { return userRepo.findAll(); }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) { return userRepo.findById(id).orElse(null); }
}
