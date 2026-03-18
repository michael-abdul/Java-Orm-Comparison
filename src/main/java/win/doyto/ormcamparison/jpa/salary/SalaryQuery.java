package win.doyto.ormcamparison.jpa.salary;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryQuery {
    @Builder.Default
    private Integer pageNumber = 0;
    @Builder.Default
    private Integer pageSize = 10;
    private Integer workYear;
    private String jobTitle;
    private Double salaryInUsdLt;
    private Double salaryInUsdGt;
    private List<SalaryQuery> orConditions;
    private SalaryQuery salaryInUsdGt0;
    private Integer empNo;
    private String empName;
}
