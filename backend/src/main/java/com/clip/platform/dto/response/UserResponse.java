package com.clip.platform.dto.response;

import com.clip.platform.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String plan;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setNickname(user.getNickname());
        resp.setRole(user.getRole());
        resp.setPlan(user.getPlan());
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }
}
