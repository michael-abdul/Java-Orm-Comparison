package win.doyto.ormcamparison.jpa.salary;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import win.doyto.ormcamparison.jpa.AbstractJpaController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Native SQL bilan optimizatsiya qilingan JPA Controller
 * JDBC dan deyarli bir xil tezlikda ishlaydi
 */
@RestController
@RequestMapping("/jpa/salary")
@Slf4j
public class SalaryJpaController extends AbstractJpaController<SalaryEntity, Long, SalaryQuery> {

    private final SalaryRepository salaryRepository;

    public SalaryJpaController(SalaryRepository repository) {
        super(repository);
        this.salaryRepository = repository;
    }

    /**
     * Native SQL query bilan optimizatsiya qilingan query
     * Criteria API o'rniga native SQL ishlatadi - tezroq!
     */
    @GetMapping("/optimized")
    public PagedModel<SalaryEntity> queryOptimized(SalaryQuery query) {
        Pageable pageable = PageRequest.of(getPageNumber(query), getPageSize(query));

        Page<SalaryEntity> result = salaryRepository.findOptimized(
                query.getWorkYear(),
                query.getJobTitle(),
                query.getSalaryInUsdGt(),
                query.getSalaryInUsdLt(),
                pageable);

        return new PagedModel<>(result);
    }

    /**
     * OR shartlari bilan optimizatsiya qilingan query
     */
    @GetMapping("/optimized-or")
    public PagedModel<SalaryEntity> queryOptimizedWithOr(SalaryQuery query) {
        Pageable pageable = PageRequest.of(getPageNumber(query), getPageSize(query));

        Double orSalaryGt = null;
        Double orSalaryLt = null;

        if (query.getOrConditions() != null && !query.getOrConditions().isEmpty()) {
            orSalaryGt = query.getOrConditions().get(0).getSalaryInUsdGt();
            orSalaryLt = query.getOrConditions().get(0).getSalaryInUsdLt();
        }

        Page<SalaryEntity> result = salaryRepository.findWithOrCondition(
                query.getWorkYear(),
                query.getJobTitle(),
                query.getSalaryInUsdGt(),
                query.getSalaryInUsdLt(),
                orSalaryGt,
                orSalaryLt,
                pageable);

        return new PagedModel<>(result);
    }

    /**
     * Subquery bilan optimizatsiya qilingan query
     */
    @GetMapping("/optimized-subquery")
    public PagedModel<SalaryEntity> queryOptimizedWithSubquery(SalaryQuery query) {
        Pageable pageable = PageRequest.of(getPageNumber(query), getPageSize(query));

        Page<SalaryEntity> result = salaryRepository.findByMaxSalaryOfYear(
                query.getWorkYear(),
                pageable);

        log.info("Subquery natijalar soni: {}", result.getTotalElements());
        return new PagedModel<>(result);
    }

    /**
     * Eski Criteria API method - taqqoslash uchun
     */
    protected Specification<SalaryEntity> toSpecification(SalaryQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getWorkYear() != null) {
                predicates.add(cb.equal(root.get("workYear"), query.getWorkYear()));
            }
            if (query.getJobTitle() != null) {
                predicates.add(cb.equal(root.get("jobTitle"), query.getJobTitle()));
            }
            if (query.getSalaryInUsdGt() != null) {
                predicates.add(cb.gt(root.get("salaryInUsd"), query.getSalaryInUsdGt()));
            }
            if (query.getSalaryInUsdLt() != null) {
                predicates.add(cb.lt(root.get("salaryInUsd"), query.getSalaryInUsdLt()));
            }
            if (query.getOrConditions() != null && !query.getOrConditions().isEmpty()) {
                SalaryQuery salaryOr = query.getOrConditions().get(0);
                List<Predicate> orPredicates = new ArrayList<>();
                if (salaryOr.getSalaryInUsdGt() != null) {
                    orPredicates.add(cb.gt(root.get("salaryInUsd"), salaryOr.getSalaryInUsdGt()));
                }
                if (salaryOr.getSalaryInUsdLt() != null) {
                    orPredicates.add(cb.lt(root.get("salaryInUsd"), salaryOr.getSalaryInUsdLt()));
                }
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }
            if (query.getSalaryInUsdGt0() != null) {
                SalaryQuery subqueryData = query.getSalaryInUsdGt0();
                Subquery<BigDecimal> subquery = cq.subquery(BigDecimal.class);
                Root<SalaryEntity> salaryRoot = subquery.from(SalaryEntity.class);
                subquery.select(cb.max(salaryRoot.get("salaryInUsd")))
                        .where(cb.equal(salaryRoot.get("workYear"), subqueryData.getWorkYear()));
                predicates.add(cb.gt(root.get("salaryInUsd"), subquery));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
