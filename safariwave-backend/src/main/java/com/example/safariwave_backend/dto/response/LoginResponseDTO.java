package com.example.safariwave_backend.dto.response;

public class LoginResponseDTO {

    private String token;
    private Integer userId;
    private String email;
    private String roleName;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, Integer userId, String email, String roleName) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.roleName = roleName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}