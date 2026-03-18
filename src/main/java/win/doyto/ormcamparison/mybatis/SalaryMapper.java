package win.doyto.ormcamparison.mybatis;

import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SalaryMapper {
    List<SalaryEntity> selectPage(@Param("query") MybatisSalaryQuery query);

    long count(@Param("query") MybatisSalaryQuery query);
}