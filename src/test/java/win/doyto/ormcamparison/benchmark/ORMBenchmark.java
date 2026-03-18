package win.doyto.ormcamparison.benchmark;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ORMBenchmark {

    private static void print(String msg) {
        System.out.println(msg);
    }

    private static void printf(String fmt, Object... args) {
        System.out.printf(fmt + "%n", args);
    }

    private static final Map<String, LatencyAccumulator> latencyMap = new ConcurrentHashMap<>();
    private static final Map<String, Long> totalTimeMs = new ConcurrentHashMap<>();
    private static final Map<String, Double> opsMap = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════════════════════
    // main — JMH 실행 진입점
    // ══════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        beforeAll();

        Options opt = new OptionsBuilder()
                .include(ORMBenchmark.class.getSimpleName())
                .forks(0)
                .warmupIterations(0)
                .measurementIterations(1)
                .resultFormat(ResultFormatType.JSON)
                .result("target/test-data.json")
                .build();
        Collection<RunResult> results = new Runner(opt).run();

        printJmhSummary(results);
        afterAll();
    }

    // ══════════════════════════════════════════════════════════
    // Spring 컨텍스트 초기화
    // ══════════════════════════════════════════════════════════

    static ConfigurableApplicationContext context;
    static MockMvc mockMvc;

    @BeforeAll
    static void beforeAll() {
        try {
            Class<?> appClass = Class.forName("win.doyto.ormcamparison.ORMApplication");
            context = SpringApplication.run(appClass, "--spring.profiles.active=postgresql");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("ORMApplication class not found", e);
        }
        mockMvc = MockMvcBuilders
                .webAppContextSetup((WebApplicationContext) context)
                .build();
    }

    @AfterAll
    static void afterAll() {
        printLatencySummary();
        context.close();
    }

    private static final int WARMUP = 5;
    private static final int ROUNDS = 100;

    // ══════════════════════════════════════════════════════════
    // 쿼리 픽스처
    // ══════════════════════════════════════════════════════════

    private static final String Q1 = "/salary/?pageSize=100";
    private static final String Q2 = "/salary/?orConditions[0].salaryInUsdGt=300000&orConditions[0].salaryInUsdLt=30000&pageSize=100";
    private static final String Q3 = "/salary/?work_year=2025&salaryInUsdLt=100000&salaryInUsdGt=20000&pageSize=100";

    // ══════════════════════════════════════════════════════════
    // DRY: 공통 벤치마크 헬퍼
    // ══════════════════════════════════════════════════════════

    /**
     * 워밍업 + 측정을 한곳에서 처리하는 헬퍼입니다.
     *
     * @param key latencyMap 키 (예: "dq_q1", "mybatis_q2")
     * @param url 요청 URL (prefix + query)
     */
    private void runBenchmark(String key, String url) throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get(url)).andExpect(status().isOk());

        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get(url)).andExpect(status().isOk());
            record(key, System.nanoTime() - s);
        }
        long totalNs = System.nanoTime() - start;
        totalTimeMs.put(key, totalNs / 1_000_000L);
        opsMap.put(key, ROUNDS / (totalNs / 1_000_000_000.0));
    }

    // ══════════════════════════════════════════════════════════
    // DoytoQuery 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void dqQuery1() throws Exception {
        runBenchmark("dq_q1", "/dq" + Q1);
    }

    @Benchmark
    @Test
    public void dqQuery2() throws Exception {
        runBenchmark("dq_q2", "/dq" + Q2);
    }

    @Benchmark
    @Test
    public void dqQuery3() throws Exception {
        runBenchmark("dq_q3", "/dq" + Q3);
    }

    // ══════════════════════════════════════════════════════════
    // JPA 벤치마크 (Criteria API)
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void jpaQuery1() throws Exception {
        runBenchmark("jpa_q1", "/jpa" + Q1);
    }

    @Benchmark
    @Test
    public void jpaQuery2() throws Exception {
        runBenchmark("jpa_q2", "/jpa" + Q2);
    }

    @Benchmark
    @Test
    public void jpaQuery3() throws Exception {
        runBenchmark("jpa_q3", "/jpa" + Q3);
    }

    // ══════════════════════════════════════════════════════════
    // JPA Native SQL 벤치마크 (최적화)
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void jpaNativeQuery1() throws Exception {
        runBenchmark("jpanative_q1", "/jpa/salary/optimized?pageSize=100");
    }

    @Benchmark
    @Test
    public void jpaNativeQuery2() throws Exception {
        runBenchmark("jpanative_q2",
                "/jpa/salary/optimized-or?pageSize=100&orConditions[0].salaryInUsdGt=300000&orConditions[0].salaryInUsdLt=30000");
    }

    @Benchmark
    @Test
    public void jpaNativeQuery3() throws Exception {
        runBenchmark("jpanative_q3",
                "/jpa/salary/optimized?work_year=2025&salaryInUsdLt=100000&salaryInUsdGt=20000&pageSize=100");
    }

    // ══════════════════════════════════════════════════════════
    // JDBC Template 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void jdbcQuery1() throws Exception {
        runBenchmark("jdbc_q1", "/jdbc" + Q1);
    }

    @Benchmark
    @Test
    public void jdbcQuery2() throws Exception {
        runBenchmark("jdbc_q2", "/jdbc" + Q2);
    }

    @Benchmark
    @Test
    public void jdbcQuery3() throws Exception {
        runBenchmark("jdbc_q3", "/jdbc" + Q3);
    }

    // ══════════════════════════════════════════════════════════
    // MyBatis-Plus 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void mpQuery1() throws Exception {
        runBenchmark("mp_q1", "/mp" + Q1);
    }

    @Benchmark
    @Test
    public void mpQuery2() throws Exception {
        runBenchmark("mp_q2", "/mp" + Q2);
    }

    @Benchmark
    @Test
    public void mpQuery3() throws Exception {
        runBenchmark("mp_q3", "/mp" + Q3);
    }

    // ══════════════════════════════════════════════════════════
    // MyBatis (Vanilla) 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void mybatisQuery1() throws Exception {
        runBenchmark("mybatis_q1", "/mybatis" + Q1);
    }

    @Benchmark
    @Test
    public void mybatisQuery2() throws Exception {
        runBenchmark("mybatis_q2", "/mybatis" + Q2);
    }

    @Benchmark
    @Test
    public void mybatisQuery3() throws Exception {
        runBenchmark("mybatis_q3", "/mybatis" + Q3);
    }

    // ══════════════════════════════════════════════════════════
    // 로그 출력
    // ══════════════════════════════════════════════════════════

    public static void printLatencySummary() {
        String[] queries = { "q1", "q2", "q3" };
        String[] orms = { "dq", "jpa", "jpanative", "jdbc", "mp", "mybatis" };
        String[] labels = { "DoytoQuery   ", "JPA (Criteria)", "JPA (Native) ", "JDBC         ", "MyBatis+     ",
                "MyBatis      " };

        print("");
        print("════════════════════════════════════════════════════════════════════════════════════════════════════");
        print("  ORM 프레임워크 벤치마크 결과 (Native SQL 포함)");
        print("  단위: ms / s │ OPS: 초당 처리량 (높을수록 좋음)");
        print("════════════════════════════════════════════════════════════════════════════════════════════════════");

        String[] rankMedals = { "1위", "2위", "3위", "4위", "5위", "6위" };

        for (int qi = 0; qi < queries.length; qi++) {
            String q = queries[qi];

            record OrmEntry(int ormIdx, long avgNs) {
            }
            var ranked = new java.util.ArrayList<OrmEntry>();
            for (int i = 0; i < orms.length; i++) {
                LatencyAccumulator acc = latencyMap.get(orms[i] + "_" + q);
                if (acc != null && acc.count() > 0)
                    ranked.add(new OrmEntry(i, acc.avg()));
            }
            ranked.sort(java.util.Comparator.comparingLong(OrmEntry::avgNs));

            print("");
            printf("  [쿼리%d] %s", qi + 1, queryDescription(qi + 1));
            print("  ┌──────┬─────────────┬────────────┬────────────┬────────────┬────────────┬────────────┬────────────┐");
            print("  │ 순위  │ 프레임워크   │    평균     │    최소     │    최대     │    p95     │   총시간    │    OPS     │");
            print("  ├──────┼─────────────┼────────────┼────────────┼────────────┼────────────┼────────────┼────────────┤");

            for (int r = 0; r < ranked.size(); r++) {
                int i = ranked.get(r).ormIdx();
                LatencyAccumulator acc = latencyMap.get(orms[i] + "_" + q);
                Long totalMs = totalTimeMs.get(orms[i] + "_" + q);
                Double ops = opsMap.get(orms[i] + "_" + q);

                String totalStr = totalMs != null ? formatDuration(totalMs * 1_000_000L) : "     N/A  ";
                String opsStr = ops != null ? String.format("%8.2f/s", ops) : "     N/A  ";

                printf("  │ %s │ %s │ %s │ %s │ %s │ %s │ %s │ %s │",
                        rankMedals[r], labels[i],
                        formatDuration(acc.avg()),
                        formatDuration(acc.min()),
                        formatDuration(acc.max()),
                        formatDuration(acc.p95()),
                        totalStr,
                        opsStr);
            }
            print("  └──────┴─────────────┴────────────┴────────────┴────────────┴────────────┴────────────┴────────────┘");
        }

        printOverallRanking(orms, labels, queries);
    }

    private static String formatDuration(long ns) {
        double ms = ns / 1_000_000.0;
        if (ms >= 1_000.0)
            return String.format("%7.3f s ", ms / 1_000.0);
        if (ms >= 1.0)
            return String.format("%7.2f ms", ms);
        return String.format("%7.3f ms", ms);
    }

    private static String queryDescription(int n) {
        return switch (n) {
            case 1 -> "1. 기본 급여 조회";
            case 2 -> "2. 여러 직군 동시 검색";
            case 3 -> "3. 2025년 특정 급여구간 필터링";
            default -> "기타 테스트";
        };
    }

    private static void printOverallRanking(String[] orms, String[] labels, String[] queries) {
        record OrmScore(String label, long avgNs, double avgOps) {
        }
        var scores = new java.util.ArrayList<OrmScore>();

        for (int i = 0; i < orms.length; i++) {
            long totalNs = 0;
            double totalOps = 0;
            int cnt = 0;
            for (String q : queries) {
                LatencyAccumulator acc = latencyMap.get(orms[i] + "_" + q);
                Double ops = opsMap.get(orms[i] + "_" + q);
                if (acc != null && acc.count() > 0) {
                    totalNs += acc.avg();
                    totalOps += ops != null ? ops : 0;
                    cnt++;
                }
            }
            if (cnt > 0)
                scores.add(new OrmScore(labels[i], totalNs / cnt, totalOps / cnt));
        }
        scores.sort(java.util.Comparator.comparingLong(OrmScore::avgNs));

        String[] medals = { "1위", "2위", "3위", "4위", "5위", "6위", "7위" };
        print("");
        print("  ┌─────────────────────────────────────────────────────────────────────────────┐");
        print("  │                   ORM 종합 순위 (전체 쿼리 평균 기준)                       │");
        print("  ├──────┬─────────────┬────────────────┬────────────────┬──────────────────────┤");
        print("  │ 순위  │ 프레임워크   │  평균 응답시간   │    평균 OPS     │   성능 차이 (1위대비)  │");
        print("  ├──────┼─────────────┼────────────────┼────────────────┼──────────────────────┤");

        long bestAvgNs = scores.isEmpty() ? 1 : scores.get(0).avgNs();

        for (int i = 0; i < scores.size(); i++) {
            long currentNs = scores.get(i).avgNs();
            double ratio = (double) currentNs / bestAvgNs;
            String diffStr = i == 0 ? "   기준 (1.0배)" : String.format("  %4.1f배 느림  ", ratio);

            printf("  │ %s  │ %s │ %s       │ %10.2f/s   │ %19s  │",
                    medals[i],
                    scores.get(i).label(),
                    formatDuration(scores.get(i).avgNs()),
                    scores.get(i).avgOps(),
                    diffStr);
        }
        print("  └──────┴─────────────┴────────────────┴────────────────┴──────────────────────┘");
        print("════════════════════════════════════════════════════════════════════════════════════════════════════");
        print("");
    }

    private static void printJmhSummary(Collection<RunResult> results) {
        print("");
        print("=================================================================");
        print("  JMH 벤치마크 최종 결과");
        print("=================================================================");
        print("  ┌───────────────────────────┬────────────┬────────────┐");
        print("  │ 벤치마크                   │ 처리량 avg  │   오차 ±    │");
        print("  ├───────────────────────────┼────────────┼────────────┤");
        results.stream()
                .sorted((a, b) -> Double.compare(
                        b.getPrimaryResult().getScore(),
                        a.getPrimaryResult().getScore()))
                .forEach(r -> {
                    String name = r.getParams().getBenchmark();
                    String short_ = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
                    double score = r.getPrimaryResult().getScore();
                    double err = r.getPrimaryResult().getScoreError();
                    printf("  │ %-27s │ %10.1f │ ±%9.1f │", short_, score, err);
                });
        print("  └───────────────────────────┴────────────┴────────────┘");
        print("=================================================================");
        print("");
    }

    private static void record(String key, long elapsedNanos) {
        latencyMap.computeIfAbsent(key, k -> new LatencyAccumulator()).add(elapsedNanos);
    }

    // ══════════════════════════════════════════════════════════
    // LatencyAccumulator
    // ══════════════════════════════════════════════════════════

    static class LatencyAccumulator {
        private final AtomicLong sum = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong max = new AtomicLong(0);

        private static final int MAX_SAMPLES = 10_000;
        private final java.util.concurrent.ConcurrentLinkedQueue<Long> samples = new java.util.concurrent.ConcurrentLinkedQueue<>();

        void add(long ns) {
            sum.addAndGet(ns);
            count.incrementAndGet();
            min.updateAndGet(cur -> Math.min(cur, ns));
            max.updateAndGet(cur -> Math.max(cur, ns));
            if (samples.size() < MAX_SAMPLES)
                samples.add(ns);
        }

        long count() {
            return count.get();
        }

        long avg() {
            return count.get() == 0 ? 0 : sum.get() / count.get();
        }

        long min() {
            return min.get() == Long.MAX_VALUE ? 0 : min.get();
        }

        long max() {
            return max.get();
        }

        long p95() {
            if (samples.isEmpty())
                return 0;
            var sorted = samples.stream().sorted().toList();
            int idx = (int) Math.ceil(0.95 * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
        }
    }
}