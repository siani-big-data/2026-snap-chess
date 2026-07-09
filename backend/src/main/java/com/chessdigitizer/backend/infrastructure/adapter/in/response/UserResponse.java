package com.chessdigitizer.backend.infrastructure.adapter.in.response;

import com.chessdigitizer.backend.domain.model.User;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String username;

    public static UserResponse fromDomain(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.id());
        response.setUsername(user.username());
        return response;
    }
}