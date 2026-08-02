package com.url.url_shortner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests over the real HTTP layer (security, JWT, JPA) against an
 * in-memory H2 database. Each test registers + logs in a fresh user so the
 * JWT-protected /api/url endpoints are exercised exactly as a client would.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    // unique usernames so tests don't collide on the users table
    private static final AtomicInteger SEQ = new AtomicInteger();

    private String registerAndLogin() throws Exception {
        String username = "user" + SEQ.incrementAndGet() + "_" + System.nanoTime();
        mvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@test.com",
                                "password", "Pass1234"))))
                .andExpect(status().isOk());

        MvcResult login = mvc.perform(post("/api/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "Pass1234"))))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    private String shorten(String token, String url) throws Exception {
        MvcResult result = mvc.perform(post("/api/url/shorten")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("originalUrl", url))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("shortUrl").asText();
    }

    @Test
    void shorten_validUrl_returns201WithCode() throws Exception {
        String token = registerAndLogin();
        mvc.perform(post("/api/url/shorten")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("originalUrl", "https://www.wikipedia.org/page"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl").value("https://www.wikipedia.org/page"));
    }

    @Test
    void shorten_invalidUrl_returns400() throws Exception {
        String token = registerAndLogin();
        mvc.perform(post("/api/url/shorten")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("originalUrl", "not-a-real-url"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void redirect_existingCode_returns302WithLocation() throws Exception {
        String token = registerAndLogin();
        String code = shorten(token, "https://example.com/target");

        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"));
    }

    @Test
    void redirect_missingCode_returns404() throws Exception {
        mvc.perform(get("/doesNotExist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void referrers_afterClickWithReferer_returnsGroupedCounts() throws Exception {
        String token = registerAndLogin();
        String code = shorten(token, "https://example.com/ref");

        mvc.perform(get("/" + code).header("Referer", "https://twitter.com/"))
                .andExpect(status().isFound());

        mvc.perform(get("/api/url/referrers/" + code)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['https://twitter.com/']").value(1));
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        // exercises the global exception handler (was a 500 before)
        String username = "nopass_" + System.nanoTime();
        mvc.perform(post("/api/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@test.com"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analytics_missingDateParams_returns400() throws Exception {
        // exercises the global exception handler for a missing required query param
        String token = registerAndLogin();
        mvc.perform(get("/api/url/totalClicks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
