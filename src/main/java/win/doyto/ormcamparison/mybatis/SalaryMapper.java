package win.doyto.ormcamparison.mybatis;

import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SalaryMapper {
    List<SalaryEntity> selectPage(@Param("query") SalaryQuery query);

    long count(@Param("query") SalaryQuery query);
}