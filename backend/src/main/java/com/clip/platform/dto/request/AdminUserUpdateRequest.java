package com.clip.platform.dto.request;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    private String role;   // USER / ADMIN
    private String plan;   // FREE / PRO
}
