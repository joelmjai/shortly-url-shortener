package com.url.url_shortner.service;

import com.url.url_shortner.dto.ClickEventDto;
import com.url.url_shortner.dto.UrlMappingDto;
import com.url.url_shortner.models.ClickEvent;
import com.url.url_shortner.models.UrlMapping;
import com.url.url_shortner.models.User;
import com.url.url_shortner.repository.ClickEventRepository;
import com.url.url_shortner.repository.UrlMappingRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlMappingService {

    private UrlMappingRepository urlMappingRepository;
    private ClickEventRepository clickEventRepository;
    public UrlMappingDto createShortUrl(String originalUrl, User user) {
        String shortUrl=generateShortUrl();
        UrlMapping urlMapping=new UrlMapping();
        urlMapping.setOrginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(java.time.LocalDateTime.now());

        UrlMapping savedUrlMapping= urlMappingRepository.save(urlMapping);
        return convertToDto(savedUrlMapping);
    }
    private UrlMappingDto convertToDto(UrlMapping urlMapping) {
        UrlMappingDto urlMappingDto=new UrlMappingDto();
        urlMappingDto.setId(urlMapping.getId());
        urlMappingDto.setOriginalUrl(urlMapping.getOrginalUrl());
        urlMappingDto.setShortUrl(urlMapping.getShortUrl());
        urlMappingDto.setClickCount(urlMapping.getClickCount());
        urlMappingDto.setCreatedDate(urlMapping.getCreatedDate());
        urlMappingDto.setUsername(urlMapping.getUser().getUserName());
        return urlMappingDto;
    }

    private String generateShortUrl() {
        String characters="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random=new Random();
        String shortUrl;
        // Regenerate on the (very rare) chance of a collision so short codes stay unique.
        do {
            StringBuilder sb=new StringBuilder(8);
            for(int i=0;i<8;i++){
                sb.append(characters.charAt(random.nextInt(characters.length())));
            }
            shortUrl=sb.toString();
        } while (urlMappingRepository.findByShortUrl(shortUrl)!=null);
        return shortUrl;
    }

    public List<UrlMappingDto> getUrlsByUser(User user) {
        List<UrlMapping> urlMappings=urlMappingRepository.findByUser(user);
        return urlMappings.stream().map(this::convertToDto).toList();
    }

    public List<ClickEventDto> getClickEventsByDate(String shortUrl, LocalDateTime start, LocalDateTime end) {
       UrlMapping urlMapping=urlMappingRepository.findByShortUrl(shortUrl);
       if(urlMapping!=null){
            return clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping,start,end).stream().collect(Collectors.groupingBy(click->click.getClickDate().toLocalDate(), Collectors.counting()))
                    .entrySet().stream().map(entry-> {
                        ClickEventDto clickEventDto = new ClickEventDto();
                        clickEventDto.setClickDate(entry.getKey());
                        clickEventDto.setCount(entry.getValue());
                        return clickEventDto;
                    }).collect(Collectors.toList());
       }
       return null;
    }

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings=urlMappingRepository.findByUser(user);
        return urlMappings.stream().flatMap(urlMapping-> clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping,start.atStartOfDay(),end.plusDays(1).atStartOfDay()).stream())
                .collect(Collectors.groupingBy(click->click.getClickDate().toLocalDate(), Collectors.counting()));
    }

    public UrlMapping getOriginalUrl(String shortUrl, String referrer) {
        UrlMapping urlMapping=urlMappingRepository.findByShortUrl(shortUrl);
        if(urlMapping!=null){
            urlMapping.setClickCount(urlMapping.getClickCount()+1);
            urlMappingRepository.save(urlMapping);

            ClickEvent clickEvent=new ClickEvent();
            clickEvent.setClickDate(java.time.LocalDateTime.now());
            clickEvent.setUrlMapping(urlMapping);
            clickEvent.setReferrer(referrer);
            clickEventRepository.save(clickEvent);
        }
        return urlMapping;
    }

    public Map<String, Long> getReferrerStats(String shortUrl) {
        UrlMapping urlMapping=urlMappingRepository.findByShortUrl(shortUrl);
        if(urlMapping==null){
            return Map.of();
        }
        return clickEventRepository.findByUrlMapping(urlMapping).stream()
                .collect(Collectors.groupingBy(
                        click->click.getReferrer()==null || click.getReferrer().isBlank() ? "direct" : click.getReferrer(),
                        Collectors.counting()));
    }
}
