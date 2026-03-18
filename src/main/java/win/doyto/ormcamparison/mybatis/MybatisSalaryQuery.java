package win.doyto.ormcamparison.mybatis;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MybatisSalaryQuery {
    private Integer pageNumber = 0;
    private Integer pageSize = 10;
    private Integer workYear;
    private String jobTitle;
    private Double salaryInUsdLt;
    private Double salaryInUsdGt;
    private List<MybatisSalaryQuery> orConditions;
    private MybatisSalaryQuery salaryInUsdGt0;
}
