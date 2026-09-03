package app.tritonwatch.ingestion_service.coursecatalog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseCatalogRepository extends JpaRepository<CourseCatalogEntry, UUID> {

    long countByTerm(String term);

    void deleteByTerm(String term);

    @Query("""
            SELECT c FROM CourseCatalogEntry c
            WHERE c.term = :term
              AND (
                    UPPER(c.courseId) LIKE UPPER(CONCAT('%', :query, '%'))
                 OR UPPER(c.title) LIKE UPPER(CONCAT('%', :query, '%'))
              )
            ORDER BY c.courseId ASC
            """)
    List<CourseCatalogEntry> search(
            @Param("term") String term,
            @Param("query") String query,
            Pageable pageable
    );
}
