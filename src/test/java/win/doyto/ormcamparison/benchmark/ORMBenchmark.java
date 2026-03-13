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

/**
 * ORM 프레임워크 성능 벤치마크입니다.
 *
 * <h2>측정 대상</h2>
 * <ul>
 * <li>DoytoQuery (dq)</li>
 * <li>JPA (jpa)</li>
 * <li>JDBC Template (jdbc)</li>
 * <li>MyBatis-Plus (mp)</li>
 * </ul>
 *
 * <h2>실행 방법</h2>
 * 
 * <pre>
 * # JUnit 단독 실행 — 응답시간 비교표 출력
 * mvn test -Dtest=ORMBenchmark -Dspring.profiles.active=postgresql
 *
 * # JMH 전체 벤치마크 — throughput 비교표 출력
 * mvn exec:java -Dexec.mainClass=win.doyto.ormcamparison.benchmark.ORMBenchmark
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-Xms256M", "-Xmx2G", "-XX:+UseG1GC" })
@Threads(8)
@Warmup(iterations = 8, time = 1)
@Measurement(iterations = 3, time = 3)
public class ORMBenchmark {

    private static void print(String msg) {
        System.out.println(msg);
    }

    private static void printf(String fmt, Object... args) {
        System.out.printf(fmt + "%n", args);
    }

    private static final Map<String, LatencyAccumulator> latencyMap = new ConcurrentHashMap<>();
    // ++ 총 시간 맵 (워밍업 제외, ROUNDS 전체 소요 시간 ms)
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
    public void init() {
        beforeAll();
    }

    @TearDown
    public void down() {
        context.close();
    }

    private static final int WARMUP = 5;
    private static final int ROUNDS = 1;

    // ══════════════════════════════════════════════════════════
    // 쿼리 픽스처
    // ══════════════════════════════════════════════════════════

    String q1 = "/salary/?pageSize=100";
    String q2 = "/salary/?or.salaryInUsdGt=300000&or.salaryInUsdLt=30000&pageSize=10000000";
    String q3 = "/salary/?work_year=2025&salaryInUsdLt=100000&salaryInUsdGt=20000&pageSize=10000000";

    // ══════════════════════════════════════════════════════════
    // 쿼리1 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void dqQuery1() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/dq" + q1)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/dq" + q1)).andExpect(status().isOk());
            record("dq_q1", System.nanoTime() - s);
        }
        totalTimeMs.put("dq_q1", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void jpaQuery1() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/jpa" + q1)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/jpa" + q1)).andExpect(status().isOk());
            record("jpa_q1", System.nanoTime() - s);
        }
        totalTimeMs.put("jpa_q1", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void jdbcQuery1() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/jdbc" + q1)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/jdbc" + q1)).andExpect(status().isOk());
            record("jdbc_q1", System.nanoTime() - s);
        }
        totalTimeMs.put("jdbc_q1", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void mpQuery1() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/mp" + q1)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/mp" + q1)).andExpect(status().isOk());
            record("mp_q1", System.nanoTime() - s);
        }
        totalTimeMs.put("mp_q1", (System.nanoTime() - start) / 1_000_000L);
    }

    // ══════════════════════════════════════════════════════════
    // 쿼리2 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void dqQuery2() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/dq" + q2)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/dq" + q2)).andExpect(status().isOk());
            record("dq_q2", System.nanoTime() - s);
        }
        totalTimeMs.put("dq_q2", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void jpaQuery2() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/jpa" + q2)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/jpa" + q2)).andExpect(status().isOk());
            record("jpa_q2", System.nanoTime() - s);
        }
        totalTimeMs.put("jpa_q2", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void jdbcQuery2() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/jdbc" + q2)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/jdbc" + q2)).andExpect(status().isOk());
            record("jdbc_q2", System.nanoTime() - s);
        }
        totalTimeMs.put("jdbc_q2", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void mpQuery2() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/mp" + q2)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/mp" + q2)).andExpect(status().isOk());
            record("mp_q2", System.nanoTime() - s);
        }
        totalTimeMs.put("mp_q2", (System.nanoTime() - start) / 1_000_000L);
    }

    // ══════════════════════════════════════════════════════════
    // 쿼리3 벤치마크
    // ══════════════════════════════════════════════════════════

    @Benchmark
    @Test
    public void dqQuery3() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/dq" + q3)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/dq" + q3)).andExpect(status().isOk());
            record("dq_q3", System.nanoTime() - s);
        }
        totalTimeMs.put("dq_q3", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void jpaQuery3() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/jpa" + q3)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/jpa" + q3)).andExpect(status().isOk());
            record("jpa_q3", System.nanoTime() - s);
        }
        totalTimeMs.put("jpa_q3", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void jdbcQuery3() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/jdbc" + q3)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/jdbc" + q3)).andExpect(status().isOk());
            record("jdbc_q3", System.nanoTime() - s);
        }
        totalTimeMs.put("jdbc_q3", (System.nanoTime() - start) / 1_000_000L);
    }

    @Benchmark
    @Test
    public void mpQuery3() throws Exception {
        for (int i = 0; i < WARMUP; i++)
            mockMvc.perform(get("/mp" + q3)).andExpect(status().isOk());
        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) {
            long s = System.nanoTime();
            mockMvc.perform(get("/mp" + q3)).andExpect(status().isOk());
            record("mp_q3", System.nanoTime() - s);
        }
        totalTimeMs.put("mp_q3", (System.nanoTime() - start) / 1_000_000L);
    }

    // ══════════════════════════════════════════════════════════
    // 로그 출력
    // ══════════════════════════════════════════════════════════

    public static void printLatencySummary() {
        String[] queries = { "q1", "q2", "q3" };
        String[] orms = { "dq", "jpa", "jdbc", "mp" };
        String[] labels = { "DoytoQuery  ", "JPA       ", "JDBC      ", "MyBatis+  " };

        print("");
        print("====================================================================================");
        print("  ORM 프레임워크 응답시간 벤치마크 결과 (JUnit 단독 실행)");
        print("  단위: ms (밀리초) / s (초), 낮을수록 좋음");
        print("====================================================================================");

        for (int qi = 0; qi < queries.length; qi++) {
            String q = queries[qi];

            record OrmEntry(int ormIdx, long avgNs) {
            }
            java.util.List<OrmEntry> ranked = new java.util.ArrayList<>();
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

            String[] rankMedals = { "1위", "2위", "3위", "4위" };
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
        if (ms >= 1_000.0) {
            return String.format("%7.3f s ", ms / 1_000.0);
        } else if (ms >= 1.0) {
            return String.format("%7.2f ms", ms);
        } else {
            return String.format("%7.3f ms", ms);
        }
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
        record OrmScore(String label, long avgNs) {
        }
        java.util.List<OrmScore> scores = new java.util.ArrayList<>();

        for (int i = 0; i < orms.length; i++) {
            long total = 0;
            int cnt = 0;
            for (String q : queries) {
                LatencyAccumulator acc = latencyMap.get(orms[i] + "_" + q);
                if (acc != null && acc.count() > 0) {
                    total += acc.avg();
                    cnt++;
                }
            }
            if (cnt > 0)
                scores.add(new OrmScore(labels[i], total / cnt));
        }
        scores.sort(java.util.Comparator.comparingLong(OrmScore::avgNs));

        String[] medals = { "1위", "2위", "3위", "4위" };
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
                            ? name.substring(name.lastIndexOf('.') + 1)
                            : name;
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

    /**
     * 스레드 안전 응답시간 누산기입니다.
     * JMH {@code @Threads(8)} 환경에서 AtomicLong 으로 동시성을 처리합니다.
     */
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
            java.util.List<Long> sorted = samples.stream().sorted().toList();
            int idx = (int) Math.ceil(0.95 * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
        }
    }
}