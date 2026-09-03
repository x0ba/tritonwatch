package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.security.SecurityConfig;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchResult;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchRequestController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "clerk.issuer=https://test.clerk.accounts.dev",
        "clerk.authorized-parties=http://localhost:5173",
        "app.cors.allowed-origins=http://localhost:5173"
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
    void usesAuthenticatedSubjectAsUserId() throws Exception {
        UUID watchRequestId = UUID.randomUUID();
        when(watchRequestService.create(eq("user_student123"), any()))
                .thenReturn(new CreateWatchResult(
                        new WatchRequestResponse(watchRequestId, "CSE 100", "FA26", Instant.parse("2026-08-30T12:00:00Z")),
                        true
                ));

        mockMvc.perform(post("/api/v1/watch-requests")
                        .with(jwt()
                                .jwt(token -> token.subject("user_student123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"CSE 100","term":"FA26"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(watchRequestId.toString()));

        verify(watchRequestService).create(eq("user_student123"), any());
    }

    @Test
    void listRejectsMissingAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/watch-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsesAuthenticatedSubjectAsUserId() throws Exception {
        UUID watchRequestId = UUID.randomUUID();
        when(watchRequestService.list(eq("user_student123"), isNull()))
                .thenReturn(List.of(
                        new WatchRequestResponse(
                                watchRequestId,
                                "CSE 100",
                                "FA26",
                                Instant.parse("2026-08-30T12:00:00Z")
                        )
                ));

        mockMvc.perform(get("/api/v1/watch-requests")
                        .with(jwt()
                                .jwt(token -> token.subject("user_student123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watches[0].id").value(watchRequestId.toString()))
                .andExpect(jsonPath("$.watches[0].courseId").value("CSE 100"));

        verify(watchRequestService).list(eq("user_student123"), isNull());
    }

    @Test
    void listPassesTermFilter() throws Exception {
        when(watchRequestService.list(eq("user_student123"), eq("FA26")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/watch-requests")
                        .param("term", "FA26")
                        .with(jwt()
                                .jwt(token -> token.subject("user_student123"))))
                .andExpect(status().isOk());

        verify(watchRequestService).list(eq("user_student123"), eq("FA26"));
    }
}
