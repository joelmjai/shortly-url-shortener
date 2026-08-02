package com.url.url_shortner.controller;

import com.url.url_shortner.models.UrlMapping;
import com.url.url_shortner.service.UrlMappingService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class RedirectController {

    private UrlMappingService urlMappingService;
    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirect(@PathVariable String shortUrl,
                                         @RequestHeader(value = "Referer", required = false) String referrer) {
        UrlMapping urlMapping = urlMappingService.getOriginalUrl(shortUrl, referrer);
        if(urlMapping!=null){
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location",urlMapping.getOrginalUrl());
            return ResponseEntity.status(302).headers(headers).build();
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }
}
