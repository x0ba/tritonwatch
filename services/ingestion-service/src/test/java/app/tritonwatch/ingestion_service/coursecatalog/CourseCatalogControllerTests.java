package app.tritonwatch.ingestion_service.coursecatalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseCatalogController.class)
class CourseCatalogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseCatalogService courseCatalogService;

    @MockitoBean
    private CourseCatalogSyncService courseCatalogSyncService;

    @Test
    void lookupReturnsMatchingCourses() throws Exception {
        CourseCatalogEntry entry = new CourseCatalogEntry();
        entry.setId(UUID.randomUUID());
        entry.setTerm("FA26");
        entry.setCourseId("CSE 100");
        entry.setTitle("Advanced Data Structures");
        entry.setOpenSeatCount(3);
        entry.setWaitlistCount(12);

        when(courseCatalogService.currentTerm()).thenReturn("FA26");
        when(courseCatalogService.findByIds(eq("FA26"), eq(List.of("CSE 100"))))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/catalog/courses/lookup")
                        .param("term", "FA26")
                        .param("ids", "CSE 100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.term").value("FA26"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.courses[0].courseId").value("CSE 100"))
                .andExpect(jsonPath("$.courses[0].title").value("Advanced Data Structures"))
                .andExpect(jsonPath("$.courses[0].openSeats").value(3))
                .andExpect(jsonPath("$.courses[0].waitlist").value(12));

        verify(courseCatalogService).findByIds(eq("FA26"), eq(List.of("CSE 100")));
    }

    @Test
    void lookupReturnsEmptyListWhenIdsOmitted() throws Exception {
        when(courseCatalogService.currentTerm()).thenReturn("FA26");
        when(courseCatalogService.findByIds(isNull(), eq(List.of()))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/catalog/courses/lookup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.courses").isEmpty());
    }
}
