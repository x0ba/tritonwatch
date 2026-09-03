package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchRequest;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchResult;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestListResponse;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/watch-requests")
@RequiredArgsConstructor
public class WatchRequestController {

    private final WatchRequestService watchRequestService;

    @PostMapping
    public ResponseEntity<WatchRequestResponse> createWatchRequest(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestBody @Valid CreateWatchRequest request
    ) {
        CreateWatchResult result = watchRequestService.create(accessToken.getSubject(), request);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        
        return ResponseEntity.status(status).body(result.watchRequest());
    }

    @GetMapping
    public WatchRequestListResponse listWatchRequests(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestParam(required = false) String term
    ) {
        return new WatchRequestListResponse(watchRequestService.list(accessToken.getSubject(), term));
    }

}
