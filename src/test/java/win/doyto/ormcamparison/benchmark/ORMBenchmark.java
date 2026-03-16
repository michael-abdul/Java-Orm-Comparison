package win.doyto.ormcamparison.benchmark;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import win.doyto.ormcamparison.ORMApplication;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-Xms256M", "-Xmx2G", "-XX:+UseG1GC" })
@Threads(8)
@Warmup(iterations = 8, time = 1)
@Measurement(iterations = 3, time = 3)
public class ORMBenchmark {

    private static void print(String msg) { System.out.println(msg); }
    private static void printf(String fmt, Object... args) { System.out.printf(fmt + "%n", args); }

    private static final Map<String, LatencyAccumulator> latencyMap = new ConcurrentHashMap<>();
    private static final Map<String, Long> totalTimeMs = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════════════════════
    // main — JMH 실행 진입점
    // ══════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ORMBenchmark.class.getSimpleName())
                .build();
        Collection<RunResult> results = new Runner(opt).run();
        printJmhSummary(results);
    }

    // ══════════════════════════════════════════════════════════
    // Spring 컨텍스트 초기화
    // ══════════════════════════════════════════════════════════

    static ConfigurableApplicationContext context;
    static MockMvc mockMvc;

    @BeforeAll
    static void beforeAll() {
        context = SpringApplication.run(ORMApplication.class, "--spring.profiles.active=postgresql");
        mockMvc = MockMvcBuilders
                .webAppContextSetup((WebApplicationContext) context)
                .build();
    }

    @AfterAll
    static void afterAll() {
        printLatencySummary();
        context.close();
    }

    @Setup
    public void init() { beforeAll(); }

    @TearDown
    public void down() { context.close(); }

    private static final int WARMUP = 5;
    private static final int ROUNDS = 100;

    // ══════════════════════════════════════════════════════════
    // 쿼리 픽스처
    // ══════════════════════════════════════════════════════════

    String q1 = "/salary/?pageSize=1000";
    String q2 = "/salary/?or.salaryInUsdGt=300000&or.salaryInUsdLt=30000&pageSize=1000";
    String q3 = "/salary/?work_year=2025&salaryInUsdLt=100000&salaryInUsdGt=20000&pageSize=1000";

    // ══════════════════════════════════════════════════════════
    // DRY: 공통 벤치마크 헬퍼
    // ══════════════════════════════════════════════════════════

    /**
     * 워밍업 + 측정을 한곳에서 처리하는 헬퍼입니다.
     *
     * @param key    latencyMap 키 (예: "dq_q1", "mybatis_q2")
     * @param url    요청 URL (prefix + query)
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
        totalTimeMs.put(key, (System.nanoTime() - start) / 1_000_000L);
    }

    // ══════════════════════════════════════════════════════════
    // DoytoQuery 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark @Test public void dqQuery1() throws Exception { runBenchmark("dq_q1", "/dq" + q1); }
    @Benchmark @Test public void dqQuery2() throws Exception { runBenchmark("dq_q2", "/dq" + q2); }
    @Benchmark @Test public void dqQuery3() throws Exception { runBenchmark("dq_q3", "/dq" + q3); }

    // ══════════════════════════════════════════════════════════
    // JPA 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark @Test public void jpaQuery1() throws Exception { runBenchmark("jpa_q1", "/jpa" + q1); }
    @Benchmark @Test public void jpaQuery2() throws Exception { runBenchmark("jpa_q2", "/jpa" + q2); }
    @Benchmark @Test public void jpaQuery3() throws Exception { runBenchmark("jpa_q3", "/jpa" + q3); }

    // ══════════════════════════════════════════════════════════
    // JDBC Template 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark @Test public void jdbcQuery1() throws Exception { runBenchmark("jdbc_q1", "/jdbc" + q1); }
    @Benchmark @Test public void jdbcQuery2() throws Exception { runBenchmark("jdbc_q2", "/jdbc" + q2); }
    @Benchmark @Test public void jdbcQuery3() throws Exception { runBenchmark("jdbc_q3", "/jdbc" + q3); }

    // ══════════════════════════════════════════════════════════
    // MyBatis-Plus 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark @Test public void mpQuery1() throws Exception { runBenchmark("mp_q1", "/mp" + q1); }
    @Benchmark @Test public void mpQuery2() throws Exception { runBenchmark("mp_q2", "/mp" + q2); }
    @Benchmark @Test public void mpQuery3() throws Exception { runBenchmark("mp_q3", "/mp" + q3); }

    // ══════════════════════════════════════════════════════════
    // MyBatis (Vanilla) 벤치마크  ← 새로 추가
    // ══════════════════════════════════════════════════════════

    @Benchmark @Test public void mybatisQuery1() throws Exception { runBenchmark("mybatis_q1", "/mybatis" + q1); }
    @Benchmark @Test public void mybatisQuery2() throws Exception { runBenchmark("mybatis_q2", "/mybatis" + q2); }
    @Benchmark @Test public void mybatisQuery3() throws Exception { runBenchmark("mybatis_q3", "/mybatis" + q3); }

    // ══════════════════════════════════════════════════════════
    // 로그 출력
    // ══════════════════════════════════════════════════════════

    public static void printLatencySummary() {
        String[] queries = { "q1", "q2", "q3" };
        String[] orms   = { "dq", "jpa", "jdbc", "mp", "mybatis" };
        String[] labels = { "DoytoQuery ", "JPA        ", "JDBC       ", "MyBatis+   ", "MyBatis    " };

        print("");
        print("====================================================================================");
        print("  ORM 프레임워크 응답시간 벤치마크 결과 (JUnit 단독 실행)");
        print("  단위: ms (밀리초) / s (초), 낮을수록 좋음");
        print("====================================================================================");

        for (int qi = 0; qi < queries.length; qi++) {
            String q = queries[qi];

            record OrmEntry(int ormIdx, long avgNs) {}
            var ranked = new java.util.ArrayList<OrmEntry>();
            for (int i = 0; i < orms.length; i++) {
                LatencyAccumulator acc = latencyMap.get(orms[i] + "_" + q);
                if (acc != null && acc.count() > 0)
                    ranked.add(new OrmEntry(i, acc.avg()));
            }
            ranked.sort(java.util.Comparator.comparingLong(OrmEntry::avgNs));

            print("");
            printf("  [쿼리%d] %s", qi + 1, queryDescription(qi + 1));
            print("  ┌──────┬─────────────┬────────────┬────────────┬────────────┬────────────┬────────────┐");
            print("  │ 순위 │ 프레임워크   │  평균       │  최소       │  최대       │  p95       │  총 시간   │");
            print("  ├──────┼─────────────┼────────────┼────────────┼────────────┼────────────┼────────────┤");

            String[] rankMedals = { "1위", "2위", "3위", "4위", "5위" };
            for (int r = 0; r < ranked.size(); r++) {
                int i = ranked.get(r).ormIdx();
                LatencyAccumulator acc = latencyMap.get(orms[i] + "_" + q);
                Long totalMs = totalTimeMs.get(orms[i] + "_" + q);
                String totalStr = totalMs != null ? formatDuration(totalMs * 1_000_000L) : "     N/A  ";
                printf("  │ %s │ %s │ %s │ %s │ %s │ %s │ %s │",
                        rankMedals[r], labels[i],
                        formatDuration(acc.avg()),
                        formatDuration(acc.min()),
                        formatDuration(acc.max()),
                        formatDuration(acc.p95()),
                        totalStr);
            }
            print("  └──────┴─────────────┴────────────┴────────────┴────────────┴────────────┴────────────┘");
        }

        printOverallRanking(orms, labels, queries);
    }

    private static String formatDuration(long ns) {
        double ms = ns / 1_000_000.0;
        if (ms >= 1_000.0) return String.format("%7.3f s ", ms / 1_000.0);
        if (ms >= 1.0)     return String.format("%7.2f ms", ms);
        return                    String.format("%7.3f ms", ms);
    }

    private static String queryDescription(int n) {
        return switch (n) {
            case 1 -> "work_year + salary 범위 필터";
            case 2 -> "jobTitle + OR 조건";
            case 3 -> "복합 조건 (서브쿼리 MAX)";
            default -> "";
        };
    }

    private static void printOverallRanking(String[] orms, String[] labels, String[] queries) {
        record OrmScore(String label, long avgNs) {}
        var scores = new java.util.ArrayList<OrmScore>();

        for (int i = 0; i < orms.length; i++) {
            long total = 0;
            int cnt = 0;
            for (String q : queries) {
                LatencyAccumulator acc = latencyMap.get(orms[i] + "_" + q);
                if (acc != null && acc.count() > 0) { total += acc.avg(); cnt++; }
            }
            if (cnt > 0) scores.add(new OrmScore(labels[i], total / cnt));
        }
        scores.sort(java.util.Comparator.comparingLong(OrmScore::avgNs));

        String[] medals = { "1위", "2위", "3위", "4위", "5위" };
        print("");
        print("  ┌──────────────────────────────────────────────┐");
        print("  │   ORM 종합 순위 (전체 쿼리 평균 응답시간 기준)   │");
        print("  ├──────┬─────────────┬───────────────────────┤");
        print("  │ 순위 │ 프레임워크   │   전체 평균 응답시간    │");
        print("  ├──────┼─────────────┼───────────────────────┤");
        for (int i = 0; i < scores.size(); i++) {
            printf("  │ %s │ %s │ %s │",
                    medals[i], scores.get(i).label(),
                    formatDuration(scores.get(i).avgNs()));
        }
        print("  └──────┴─────────────┴───────────────────────┘");
        print("====================================================================================");
        print("");
    }

    private static void printJmhSummary(Collection<RunResult> results) {
        print("");
        print("=================================================================");
        print("  JMH 벤치마크 최종 결과 (처리량: ops/sec, 높을수록 좋음)");
        print("=================================================================");
        print("  ┌───────────────────────────┬────────────┬────────────┐");
        print("  │ 벤치마크                   │ 처리량 avg │   오차 ±   │");
        print("  ├───────────────────────────┼────────────┼────────────┤");
        results.stream()
                .sorted((a, b) -> Double.compare(
                        b.getPrimaryResult().getScore(),
                        a.getPrimaryResult().getScore()))
                .forEach(r -> {
                    String name = r.getParams().getBenchmark();
                    String short_ = name.contains(".")
                            ? name.substring(name.lastIndexOf('.') + 1) : name;
                    double score = r.getPrimaryResult().getScore();
                    double err   = r.getPrimaryResult().getScoreError();
                    printf("  │ %-27s │ %10.1f │ ±%9.1f │", short_, score, err);
                });
        print("  └───────────────────────────┴────────────┴────────────┘");
        print("=================================================================");
        print("");
    }

    private static void record(String key, long elapsedNanos) {
        latencyMap.computeIfAbsent(key, k -> new LatencyAccumulator()).add(elapsedNanos);
    }

    static class LatencyAccumulator {
        private final AtomicLong sum   = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong min   = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong max   = new AtomicLong(0);

        private static final int MAX_SAMPLES = 10_000;
        private final java.util.concurrent.ConcurrentLinkedQueue<Long> samples =
                new java.util.concurrent.ConcurrentLinkedQueue<>();

        void add(long ns) {
            sum.addAndGet(ns);
            count.incrementAndGet();
            min.updateAndGet(cur -> Math.min(cur, ns));
            max.updateAndGet(cur -> Math.max(cur, ns));
            if (samples.size() < MAX_SAMPLES) samples.add(ns);
        }

        long count() { return count.get(); }
        long avg()   { return count.get() == 0 ? 0 : sum.get() / count.get(); }
        long min()   { return min.get() == Long.MAX_VALUE ? 0 : min.get(); }
        long max()   { return max.get(); }

        long p95() {
            if (samples.isEmpty()) return 0;
            var sorted = samples.stream().sorted().toList();
            int idx = (int) Math.ceil(0.95 * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
        }
    }
}