package com.url.url_shortner.controller;

import com.url.url_shortner.dto.ClickEventDto;
import com.url.url_shortner.dto.UrlMappingDto;
import com.url.url_shortner.models.User;
import com.url.url_shortner.service.UrlMappingService;
import com.url.url_shortner.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/url")
@AllArgsConstructor
public class UrlMappingController {
    private UrlMappingService urlMappingService;



    private UserService userService;
    //{"originalUrl":"https://example.com"}
    //https://abc.com/CflPwBQH-->https://example.com
    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createShortUrl(@RequestBody Map<String, String> request, Principal principal) {
        String originalUrl = request.get("originalUrl");
        if (!isValidUrl(originalUrl)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid URL. Provide a valid http:// or https:// URL."));
        }
        User user= userService.findByUserName(principal.getName());
        //call service
        UrlMappingDto urlMappingDto=urlMappingService.createShortUrl(originalUrl,user);
        return ResponseEntity.status(HttpStatus.CREATED).body(urlMappingDto);

    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            return uri.isAbsolute()
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }
    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingDto>> getUserUrls(Principal principal) {
        User user= userService.findByUserName(principal.getName());
        List<UrlMappingDto> urls= urlMappingService.getUrlsByUser(user);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickEventDto>> getUserUrlAnalytics(@PathVariable String shortUrl, @RequestParam("startDate")String startDate,
                                                                   @RequestParam("endDate")String endDate) {
        DateTimeFormatter formatter=DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start=LocalDateTime.parse(startDate, formatter);
        LocalDateTime end=LocalDateTime.parse(endDate, formatter);
        List<ClickEventDto> clickEventDtos=urlMappingService.getClickEventsByDate(shortUrl,start,end);
        return ResponseEntity.ok(clickEventDtos);


    }

    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDate,Long>> getTotalClicksByDate(Principal principal, @RequestParam("startDate")String startDate,
                                                   @RequestParam("endDate")String endDate) {
        User user= userService.findByUserName(principal.getName());
        DateTimeFormatter formatter=DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate start=LocalDate.parse(startDate, formatter);
        LocalDate end=LocalDate.parse(endDate, formatter);
        Map<LocalDate,Long> totalClicks=urlMappingService.getTotalClicksByUserAndDate(user,start,end);
        return ResponseEntity.ok(totalClicks);
    }

    @GetMapping("/referrers/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String,Long>> getReferrers(@PathVariable String shortUrl) {
        Map<String,Long> referrers=urlMappingService.getReferrerStats(shortUrl);
        return ResponseEntity.ok(referrers);
    }


}
