package ru.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.exception.UserAlreadyExistsException;
import ru.entity.User;
import ru.entity.UserMapper;
import ru.dto.UserForm;
import ru.dto.UserResponse;
import ru.repository.UserRepository;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public boolean existsByUsernameAndPassword(String username, String password) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        return user != null && passwordEncoder.matches(password, user.getPassword());
    }

    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    public UserResponse createUser(UserForm form) {
        return Optional.of(form)
                .filter(temp -> !userRepository.existsByUsernameIgnoreCase(form.username()))
                .map(temp ->{
                    User user = userMapper.toEntity(temp);
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    return userRepository.save(user);
                })
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserAlreadyExistsException(
                        "There is already a user with username: " + form.username()
                ));
    }
}
