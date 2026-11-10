package sn.senetech.itsn.kb;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import sn.senetech.itsn.auth.CurrentUserService;
import sn.senetech.itsn.common.NotFoundException;
import sn.senetech.itsn.domain.KnowledgeArticle;
import sn.senetech.itsn.repository.CategoryRepository;
import sn.senetech.itsn.repository.KnowledgeArticleRepository;

import java.time.Instant;

/** Base de connaissances : procédures et solutions réutilisables, consultables par tous les utilisateurs. */
@RestController
@RequestMapping("/api/kb/articles")
public class KnowledgeController {

    public record ArticleRequest(@NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 20000) String content,
                                 Long categoryId, @Size(max = 255) String tags, boolean published) {}

    public record ArticleResponse(Long id, String title, String content, Long categoryId, String categoryName, String tags,
                                  String authorName, boolean published, int views, Instant createdAt, Instant updatedAt) {
        static ArticleResponse from(KnowledgeArticle a) {
            return new ArticleResponse(a.getId(), a.getTitle(), a.getContent(),
                a.getCategory() == null ? null : a.getCategory().getId(), a.getCategory() == null ? null : a.getCategory().getName(),
                a.getTags(), a.getAuthor() == null ? null : a.getAuthor().getFullName(), a.isPublished(), a.getViews(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    private final KnowledgeArticleRepository articles;
    private final CategoryRepository categories;
    private final CurrentUserService currentUser;

    public KnowledgeController(KnowledgeArticleRepository articles, CategoryRepository categories, CurrentUserService currentUser) {
        this.articles = articles; this.categories = categories; this.currentUser = currentUser;
    }

    @GetMapping
    public Page<ArticleResponse> search(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        boolean publishedOnly = !currentUser.isStaff();
        String term = (q == null || q.isBlank()) ? null : q.trim();
        return articles.search(term, publishedOnly, PageRequest.of(page, Math.min(size, 50))).map(ArticleResponse::from);
    }

    @GetMapping("/{id}")
    @Transactional
    public ArticleResponse get(@PathVariable Long id) {
        KnowledgeArticle a = articles.findById(id).orElseThrow(() -> new NotFoundException("Article", id));
        if (!a.isPublished() && !currentUser.isStaff()) throw new AccessDeniedException("Article non publié");
        a.incrementViews();
        return ArticleResponse.from(a);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public ArticleResponse create(@Valid @RequestBody ArticleRequest req) {
        KnowledgeArticle a = new KnowledgeArticle();
        a.setAuthor(currentUser.currentUser());
        apply(a, req);
        return ArticleResponse.from(articles.save(a));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public ArticleResponse update(@PathVariable Long id, @Valid @RequestBody ArticleRequest req) {
        KnowledgeArticle a = articles.findById(id).orElseThrow(() -> new NotFoundException("Article", id));
        apply(a, req);
        return ArticleResponse.from(articles.save(a));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void delete(@PathVariable Long id) {
        if (!articles.existsById(id)) throw new NotFoundException("Article", id);
        articles.deleteById(id);
    }

    private void apply(KnowledgeArticle a, ArticleRequest r) {
        a.setTitle(r.title().trim()); a.setContent(r.content()); a.setTags(r.tags()); a.setPublished(r.published());
        a.setCategory(r.categoryId() == null ? null : categories.findById(r.categoryId()).orElseThrow(() -> new NotFoundException("Catégorie", r.categoryId())));
    }
}
