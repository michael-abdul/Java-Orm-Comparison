package win.doyto.ormcamparison.jpa;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
public abstract class AbstractJpaController<E, ID, Q> {
    protected JpaRepositoryImplementation<E, ID> repository;

    @GetMapping("/")
    public PagedModel<E> query(Q query) {
        Pageable pageable = PageRequest.of(getPageNumber(query), getPageSize(query));
        return new PagedModel<>(repository.findAll(toSpecification(query), pageable));
    }

    @DeleteMapping("/")
    public long deleteByQuery(Q query) {
        return repository.delete(toSpecification(query));
    }

    protected abstract Specification<E> toSpecification(Q query);

    protected int getPageNumber(Q query) {
        try {
            return (int) query.getClass().getMethod("getPageNumber").invoke(query);
        } catch (Exception e) {
            return 0;
        }
    }

    protected int getPageSize(Q query) {
        try {
            return (int) query.getClass().getMethod("getPageSize").invoke(query);
        } catch (Exception e) {
            return 10;
        }
    }

    @PostMapping("/")
    public void create(@RequestBody E e) {
        this.repository.save(e);
    }

    @PutMapping("/{id}")
    public void update(@RequestBody E e) {
        this.repository.save(e);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable ID id) {
        this.repository.deleteById(id);
    }

    @GetMapping("/{id}")
    public E get(@PathVariable ID id) {
        return this.repository.getReferenceById(id);
    }

}
