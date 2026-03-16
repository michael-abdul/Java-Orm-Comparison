package win.doyto.ormcamparison.mybatisplus.salary;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mp/salary")
public class SalaryMpController {
    @Autowired
    private SalaryMpMapper salaryMapper;

    @GetMapping("/")
    public Page<SalaryEntity> page(SalaryQuery query) {
        QueryWrapper<SalaryEntity> wrapper = buildWrapper(query);
        Page<SalaryEntity> page = new Page<>(query.getPageNumber(), query.getPageSize());
        return salaryMapper.selectPage(page, wrapper);
    }

    private QueryWrapper<SalaryEntity> buildWrapper(SalaryQuery query) {
        QueryWrapper<SalaryEntity> wrapper = new QueryWrapper<>();
        if (query.getWorkYear() != null) {
            wrapper.eq("work_year", query.getWorkYear());
        }
        if (query.getJobTitle() != null) {
            wrapper.eq("job_title", query.getJobTitle());
        }
        if (query.getSalaryInUsdLt() != null) {
            wrapper.lt("salary_in_usd", query.getSalaryInUsdLt());
        }
        if (query.getSalaryInUsdGt() != null) {
            wrapper.gt("salary_in_usd", query.getSalaryInUsdGt());
        }
        if (query.getOr() != null) {
            SalaryQuery orQuery = query.getOr();
            wrapper.and(w -> {
                if (orQuery.getSalaryInUsdGt() != null) {
                    w.or().gt("salary_in_usd", orQuery.getSalaryInUsdGt());
                }
                if (orQuery.getSalaryInUsdLt() != null) {
                    w.or().lt("salary_in_usd", orQuery.getSalaryInUsdLt());
                }
            });
        }
        if (query.getSalaryInUsdGt0() != null) {
            wrapper.gtSql("salary_in_usd", "SELECT max(salary_in_usd) FROM salary WHERE work_year = "
                    + query.getSalaryInUsdGt0().getWorkYear());
        }
        return wrapper;
    }
}
