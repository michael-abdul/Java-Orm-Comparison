package win.doyto.ormcamparison.dq.salary;

import lombok.Data;
import lombok.EqualsAndHashCode;
import win.doyto.query.entity.AbstractPersistable;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Salaries1Entity extends AbstractPersistable<Integer> {
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
