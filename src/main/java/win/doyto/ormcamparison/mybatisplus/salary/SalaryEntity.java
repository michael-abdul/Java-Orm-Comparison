package win.doyto.ormcamparison.mybatisplus.salary;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`salary`")
public class SalaryEntity {
    private Integer id;
    private Integer workYear;
    private String experienceLevel;
    private String employmentType;
    private String jobTitle;
    private Double salary;
    private String salaryCurrency;
    private Double salaryInUsd;
    private String employeeResidence;
    private Integer remoteRatio;
    private String companyLocation;
    private String companySize;
}
