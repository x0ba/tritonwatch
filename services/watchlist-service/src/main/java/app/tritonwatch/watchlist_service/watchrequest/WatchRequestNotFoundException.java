package app.tritonwatch.watchlist_service.watchrequest;

public class WatchRequestNotFoundException extends RuntimeException {

    public WatchRequestNotFoundException() {
        super("Watch request was not found");
    }
}
