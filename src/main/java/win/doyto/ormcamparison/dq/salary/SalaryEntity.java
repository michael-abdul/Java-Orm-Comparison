package win.doyto.ormcamparison.dq.salary;

import lombok.Data;
import win.doyto.query.entity.AbstractPersistable;

import java.math.BigDecimal;

@Data
public class SalaryEntity extends AbstractPersistable<Integer> {
    private Integer workYear;
    private String experienceLevel;
    private String employmentType;
    private String jobTitle;
    private BigDecimal salary;
    private String salaryCurrency;
    private BigDecimal salaryInUsd;
    private String employeeResidence;
    private Integer remoteRatio;
    private String companyLocation;
    private String companySize;
}
