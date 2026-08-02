package com.url.url_shortner.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="users")


public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String userName;

    private String password;

    private String role="ROLE_USER";
}
