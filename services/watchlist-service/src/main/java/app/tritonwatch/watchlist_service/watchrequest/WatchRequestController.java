package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchRequest;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchResult;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watch-requests")
@RequiredArgsConstructor
public class WatchRequestController {

    private final WatchRequestService watchRequestService;

    @PostMapping
    public ResponseEntity<WatchRequestResponse> createWatchRequest(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody @Valid CreateWatchRequest request
    ) {
        CreateWatchResult result = watchRequestService.create(userId, request);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        
        return ResponseEntity.status(status).body(result.watchRequest());
    }

}
