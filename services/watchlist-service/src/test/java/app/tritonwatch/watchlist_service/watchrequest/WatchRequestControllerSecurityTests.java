package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.security.SecurityConfig;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchResult;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchRequestController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "auth0.issuer=https://test-tenant.auth0.com/",
        "auth0.audience=https://api.tritonwatch.app",
        "auth0.allowed-origins=http://localhost:5173"
})
class WatchRequestControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatchRequestService watchRequestService;

    @Test
    void rejectsMissingAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/watch-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"CSE 100","term":"FA26"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutRequiredScope() throws Exception {
        mockMvc.perform(post("/api/v1/watch-requests")
                        .with(jwt().jwt(token -> token.subject("auth0|student-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"CSE 100","term":"FA26"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void usesAuthenticatedSubjectAsUserId() throws Exception {
        UUID watchRequestId = UUID.randomUUID();
        when(watchRequestService.create(eq("auth0|student-123"), any()))
                .thenReturn(new CreateWatchResult(
                        new WatchRequestResponse(watchRequestId, "CSE 100", "FA26", Instant.parse("2026-08-30T12:00:00Z")),
                        true
                ));

        mockMvc.perform(post("/api/v1/watch-requests")
                        .with(jwt()
                                .jwt(token -> token.subject("auth0|student-123"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_create:watch-requests")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"CSE 100","term":"FA26"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(watchRequestId.toString()));

        verify(watchRequestService).create(eq("auth0|student-123"), any());
    }
}
