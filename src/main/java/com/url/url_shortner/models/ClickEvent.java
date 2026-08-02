package com.url.url_shortner.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Entity
@Data
public class ClickEvent {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime clickDate;

    private String referrer;

    @ManyToOne
    @JoinColumn(name="url_mapping_id")
    private UrlMapping urlMapping;
}
