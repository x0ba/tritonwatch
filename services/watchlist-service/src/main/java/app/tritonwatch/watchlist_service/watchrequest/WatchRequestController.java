package app.tritonwatch.watchlist_service.watchrequest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/watch-requests")
@RequiredArgsConstructor
public class WatchRequestController {

    private final WatchRequestService watchRequestService;

    @PostMapping
    public ResponseEntity<WatchRequest> createWatchRequest() {
    }

}
