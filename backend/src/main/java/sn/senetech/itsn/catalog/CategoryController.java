package sn.senetech.itsn.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.senetech.itsn.common.BusinessException;
import sn.senetech.itsn.common.NotFoundException;
import sn.senetech.itsn.domain.Category;
import sn.senetech.itsn.repository.CategoryRepository;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    public record CategoryRequest(@NotBlank @Pattern(regexp = "[A-Z_]{2,30}") String code, @NotBlank @Size(max = 80) String name, String description, Boolean active) {}
    public record CategoryResponse(Long id, String code, String name, String description, boolean active) {
        static CategoryResponse from(Category c) { return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(), c.isActive()); }
    }

    private final CategoryRepository categories;

    public CategoryController(CategoryRepository categories) { this.categories = categories; }

    @GetMapping
    public List<CategoryResponse> list(@RequestParam(defaultValue = "true") boolean activeOnly) {
        List<Category> l = activeOnly ? categories.findByActiveTrueOrderByName() : categories.findAll();
        return l.stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest req) {
        categories.findByCode(req.code()).ifPresent(c -> { throw new BusinessException("Code de catégorie déjà utilisé"); });
        Category c = new Category();
        c.setCode(req.code()); c.setName(req.name()); c.setDescription(req.description());
        if (req.active() != null) c.setActive(req.active());
        return CategoryResponse.from(categories.save(c));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Catégorie", id));
        c.setName(req.name()); c.setDescription(req.description());
        if (req.active() != null) c.setActive(req.active());
        return CategoryResponse.from(categories.save(c));
    }
}
