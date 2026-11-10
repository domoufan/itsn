package sn.senetech.itsn.ticket;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sn.senetech.itsn.domain.Priority;
import sn.senetech.itsn.domain.TicketStatus;
import sn.senetech.itsn.domain.TicketType;
import sn.senetech.itsn.ticket.TicketDtos.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public Page<TicketSummary> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean openOnly,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] s = sort.split(",");
        Sort.Direction dir = s.length > 1 && s[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pr = PageRequest.of(page, Math.min(size, 100), Sort.by(dir, s[0]));
        TicketFilter filter = new TicketFilter(status, priority, type, assigneeId, requesterId, siteId, categoryId, openOnly, slaBreached, search);
        return service.search(filter, pr);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return service.assign(id, request);
    }

    @PostMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return service.changeStatus(id, request);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id) {
        return service.comments(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return service.comment(id, request);
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping("/{id}/satisfaction")
    public TicketResponse rate(@PathVariable Long id, @Valid @RequestBody SatisfactionRequest request) {
        return service.rate(id, request);
    }
}
