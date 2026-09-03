package app.tritonwatch.ingestion_service.coursecatalog;

import app.tritonwatch.ingestion_service.ucsd.UcsdCatalogClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceTests {

    @Mock
    private CourseCatalogRepository courseCatalogRepository;

    @Mock
    private UcsdCatalogClient ucsdCatalogClient;

    @InjectMocks
    private CourseCatalogService courseCatalogService;

    @Test
    void findByIdsReturnsThe101stRequestedCourse() {
        List<String> ids = IntStream.rangeClosed(0, 100)
                .mapToObj(i -> "CSE " + i)
                .toList();

        when(courseCatalogRepository.findByTermAndCourseIdIn(eq("FA26"), anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<String> requested = invocation.getArgument(1);
                    return requested.stream()
                            .map(id -> entry(id, id.equals("CSE 100") ? 5 : 0))
                            .toList();
                });

        List<CourseCatalogEntry> result = courseCatalogService.findByIds("FA26", ids);

        assertEquals(101, result.size());
        CourseCatalogEntry last = result.stream()
                .filter(entry -> "CSE 100".equals(entry.getCourseId()))
                .findFirst()
                .orElseThrow();
        assertEquals(5, last.getOpenSeatCount());
        verify(courseCatalogRepository).findByTermAndCourseIdIn(eq("FA26"), anyCollection());
    }

    private static CourseCatalogEntry entry(String courseId, int openSeats) {
        CourseCatalogEntry entry = new CourseCatalogEntry();
        entry.setId(UUID.randomUUID());
        entry.setTerm("FA26");
        entry.setCourseId(courseId);
        entry.setTitle(courseId);
        entry.setOpenSeatCount(openSeats);
        entry.setWaitlistCount(0);
        return entry;
    }
}
