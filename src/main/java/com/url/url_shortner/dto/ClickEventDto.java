package com.url.url_shortner.dto;

import lombok.Data;

import java.time.LocalDate;


@Data

public class ClickEventDto {

    private LocalDate clickDate;
    private Long count;
}
