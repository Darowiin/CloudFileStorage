package ru.entity;

import org.mapstruct.Mapper;
import ru.dto.UserForm;
import ru.dto.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    User toEntity(UserForm form);
}
