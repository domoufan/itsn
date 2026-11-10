package sn.senetech.itsn.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.senetech.itsn.domain.KnowledgeArticle;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    @Query("select a from KnowledgeArticle a where (:publishedOnly = false or a.published = true) and (:term is null or lower(a.title) like lower(concat('%', :term, '%')) or lower(a.content) like lower(concat('%', :term, '%')) or lower(a.tags) like lower(concat('%', :term, '%'))) order by a.updatedAt desc")
    Page<KnowledgeArticle> search(@Param("term") String term, @Param("publishedOnly") boolean publishedOnly, Pageable pageable);
}
