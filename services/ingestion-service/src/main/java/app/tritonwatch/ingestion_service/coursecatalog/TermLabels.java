package app.tritonwatch.ingestion_service.coursecatalog;

import java.util.Locale;
import java.util.Map;

public final class TermLabels {

    private static final Map<String, String> SEASON_NAMES = Map.of(
            "FA", "Fall",
            "WI", "Winter",
            "SP", "Spring",
            "S1", "Summer Session I",
            "S2", "Summer Session II",
            "S3", "Summer Session III"
    );

    private TermLabels() {
    }

    public static String labelFor(String termCode) {
        if (termCode == null || termCode.isBlank()) {
            return "Unknown term";
        }

        String normalized = termCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 3) {
            return normalized;
        }

        String season;
        String yearSuffix;
        if (normalized.startsWith("S1") || normalized.startsWith("S2") || normalized.startsWith("S3")) {
            season = normalized.substring(0, 2);
            yearSuffix = normalized.substring(2);
        } else {
            season = normalized.substring(0, 2);
            yearSuffix = normalized.substring(2);
        }

        String seasonName = SEASON_NAMES.getOrDefault(season, season);
        if (yearSuffix.length() != 2 || !yearSuffix.chars().allMatch(Character::isDigit)) {
            return normalized;
        }

        int year = 2000 + Integer.parseInt(yearSuffix);
        return seasonName + " " + year;
    }
}
