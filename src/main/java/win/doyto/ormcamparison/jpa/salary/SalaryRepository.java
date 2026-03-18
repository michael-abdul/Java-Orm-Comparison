package win.doyto.ormcamparison.jpa.salary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * SalaryRepository
 *
 * @author f0rb on 2025/6/26
 */
@Repository
public interface SalaryRepository extends JpaRepositoryImplementation<SalaryEntity, Long> {

       /**
        * Native SQL bilan optimizatsiya qilingan query
        */
       @Query(value = "SELECT id, work_year, experience_level, employment_type, job_title, " +
                     "salary, salary_currency, salary_in_usd, employee_residence, remote_ratio, " +
                     "company_location, company_size " +
                     "FROM salaries1 " +
                     "WHERE 1=1 " +
                     "AND (:workYear IS NULL OR work_year = :workYear) " +
                     "AND (:jobTitle IS NULL OR job_title = :jobTitle) " +
                     "AND (:salaryInUsdGt IS NULL OR salary_in_usd > :salaryInUsdGt) " +
                     "AND (:salaryInUsdLt IS NULL OR salary_in_usd < :salaryInUsdLt) ", countQuery = "SELECT COUNT(0) FROM salaries1 "
                                   +
                                   "WHERE 1=1 " +
                                   "AND (:workYear IS NULL OR work_year = :workYear) " +
                                   "AND (:jobTitle IS NULL OR job_title = :jobTitle) " +
                                   "AND (:salaryInUsdGt IS NULL OR salary_in_usd > :salaryInUsdGt) " +
                                   "AND (:salaryInUsdLt IS NULL OR salary_in_usd < :salaryInUsdLt) ", nativeQuery = true)
       Page<SalaryEntity> findOptimized(
                     @Param("workYear") Integer workYear,
                     @Param("jobTitle") String jobTitle,
                     @Param("salaryInUsdGt") Double salaryInUsdGt,
                     @Param("salaryInUsdLt") Double salaryInUsdLt,
                     Pageable pageable);

       /**
        * OR shartlari bilan native query
        */
       @Query(value = "SELECT id, work_year, experience_level, employment_type, job_title, " +
                     "salary, salary_currency, salary_in_usd, employee_residence, remote_ratio, " +
                     "company_location, company_size " +
                     "FROM salaries1 " +
                     "WHERE 1=1 " +
                     "AND (:workYear IS NULL OR work_year = :workYear) " +
                     "AND (:jobTitle IS NULL OR job_title = :jobTitle) " +
                     "AND (:salaryInUsdGt IS NULL OR salary_in_usd > :salaryInUsdGt) " +
                     "AND (:salaryInUsdLt IS NULL OR salary_in_usd < :salaryInUsdLt) " +
                     "AND (:orSalaryInUsdGt IS NULL OR :orSalaryInUsdLt IS NULL OR " +
                     "    (salary_in_usd > :orSalaryInUsdGt OR salary_in_usd < :orSalaryInUsdLt)) ", countQuery = "SELECT COUNT(0) FROM salaries1 "
                                   +
                                   "WHERE 1=1 " +
                                   "AND (:workYear IS NULL OR work_year = :workYear) " +
                                   "AND (:jobTitle IS NULL OR job_title = :jobTitle) " +
                                   "AND (:salaryInUsdGt IS NULL OR salary_in_usd > :salaryInUsdGt) " +
                                   "AND (:salaryInUsdLt IS NULL OR salary_in_usd < :salaryInUsdLt) " +
                                   "AND (:orSalaryInUsdGt IS NULL OR :orSalaryInUsdLt IS NULL OR " +
                                   "    (salary_in_usd > :orSalaryInUsdGt OR salary_in_usd < :orSalaryInUsdLt)) ", nativeQuery = true)
       Page<SalaryEntity> findWithOrCondition(
                     @Param("workYear") Integer workYear,
                     @Param("jobTitle") String jobTitle,
                     @Param("salaryInUsdGt") Double salaryInUsdGt,
                     @Param("salaryInUsdLt") Double salaryInUsdLt,
                     @Param("orSalaryInUsdGt") Double orSalaryInUsdGt,
                     @Param("orSalaryInUsdLt") Double orSalaryInUsdLt,
                     Pageable pageable);

       /**
        * Subquery bilan native query
        */
       @Query(value = "SELECT id, work_year, experience_level, employment_type, job_title, " +
                     "salary, salary_currency, salary_in_usd, employee_residence, remote_ratio, " +
                     "company_location, company_size " +
                     "FROM salaries1 " +
                     "WHERE salary_in_usd > (" +
                     "    SELECT MAX(salary_in_usd) FROM salaries1 " +
                     "    WHERE (:workYear IS NULL OR work_year = :workYear)" +
                     ") ", countQuery = "SELECT COUNT(0) FROM salaries1 " +
                                   "WHERE salary_in_usd > (" +
                                   "    SELECT MAX(salary_in_usd) FROM salaries1 " +
                                   "    WHERE (:workYear IS NULL OR work_year = :workYear)" +
                                   ") ", nativeQuery = true)
       Page<SalaryEntity> findByMaxSalaryOfYear(@Param("workYear") Integer workYear, Pageable pageable);
}
