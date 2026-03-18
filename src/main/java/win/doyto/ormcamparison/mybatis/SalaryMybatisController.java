package win.doyto.ormcamparison.mybatis;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mybatis/salary")
public class SalaryMybatisController {

    @Autowired
    private SalaryMapper salaryMapper;

    @GetMapping("/")
    public PageList<SalaryEntity> page(MybatisSalaryQuery query) {
        List<SalaryEntity> list = salaryMapper.selectPage(query);
        long total = salaryMapper.count(query);
        return new PageList<>(list, total);
    }
}