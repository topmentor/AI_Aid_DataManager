/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.util;

import com.sox.ltex.model.shape.MBR;
import java.util.*;

/**
 * Requirements covered:
 * 1) Generate 30 random 2x2 squares in x,y ∈ [1..200]
 * 2) Find clusters via distance-based clustering (DBSCAN-like, using square centers)
 *    - minPts=2 => "2개만 모여도 군집 인정" (includes itself)
 * 3) Main cluster selection priority:
 *    (A) Maximize number of squares included (MOST IMPORTANT)
 *    (B) Subject to extent(W<=80,H<=80), choose cluster whose extent is closest to 80x80
 *    (C) If NO cluster fits 80x80: start from the largest DBSCAN cluster and prune to fit 80x80, then refine
 * 4) Options:
 *    - eps, minPts, outlierCount, outlierMode
 * 5) Logs clearly distinguish:
 *    - "clusters=0" vs "clusters exist but none fits 80x80"
 * 6) Output:
 *    - main cluster extent
 *    - each square extent in main cluster
 *    - each outlier square extent (top-K by option)
 */
public class MainClusterByMaxCount {

    // =======================
    // Options
    // =======================
    enum OutlierMode {
        FARTHEST_FROM_CLUSTER_CENTER,
        NEAREST_TO_CLUSTER
    }

    static class Config {
        int N = 30;
        int SIZE = 2;
        int MIN = 1;
        int MAX = 200;

        int CLUSTER_MAX_W = 100;
        int CLUSTER_MAX_H = 100;

        // Distance-based clustering (DBSCAN-like)
        double eps = 15.0;
        int minPts = 2; // "2개만 모여도 군집 인정" (self 포함)

        // Outlier selection
        int outlierCount = 6;
        OutlierMode outlierMode = OutlierMode.FARTHEST_FROM_CLUSTER_CENTER;

        long seed = 20260115L;
    }

    // =======================
    // Geometry / Data
    // =======================
    static String formatMbr(MBR mbr) {
        return String.format("{minX:%.1f, minY:%.1f, maxX:%.1f, maxY:%.1f, w:%.1f, h:%.1f}",
                mbr.minX, mbr.minY, mbr.maxX, mbr.maxY, mbr.getWidth(), mbr.getHeight());
    }

    static double area(MBR mbr) {
        return mbr.getWidth() * mbr.getHeight();
    }

    static String formatMbrWithId(int idx, MBR mbr) {
        return String.format("MBR#%02d extent=%s center=(%.1f,%.1f)",
                idx + 1, formatMbr(mbr), mbr.getCenterX(), mbr.getCenterY());
    }

    static List<MBR> mbrsByIndices(List<MBR> all, List<Integer> indices) {
        List<MBR> out = new ArrayList<>(indices.size());
        for (int idx : indices) out.add(all.get(idx));
        return out;
    }

    // =======================
    // Random generation
    // =======================
    static List<MBR> generateRandomMbrs(Config cfg) {
        int maxMinXY = cfg.MAX - cfg.SIZE;
        if (maxMinXY < cfg.MIN) throw new IllegalArgumentException("Range too small for square size.");

        Random rnd = new Random(cfg.seed);
        List<MBR> out = new ArrayList<>(cfg.N);
        for (int i = 0; i < cfg.N; i++) {
            int x = cfg.MIN + rnd.nextInt(maxMinXY - cfg.MIN + 1);
            int y = cfg.MIN + rnd.nextInt(maxMinXY - cfg.MIN + 1);
            out.add(new MBR(x, y, x + cfg.SIZE, y + cfg.SIZE));
        }
        return out;
    }

    // =======================
    // DBSCAN-like clustering
    // =======================
    static List<List<Integer>> dbscan(List<MBR> mbrs, double eps, int minPts) {
        int n = mbrs.size();
        int[] labels = new int[n]; // 0=unvisited, -1=noise, >0=clusterId
        int clusterId = 0;

        for (int i = 0; i < n; i++) {
            if (labels[i] != 0) continue;

            List<Integer> neighbors = regionQuery(mbrs, i, eps);
            if (neighbors.size() < minPts) {
                labels[i] = -1;
                continue;
            }

            clusterId++;
            expandCluster(mbrs, labels, i, neighbors, clusterId, eps, minPts);
        }

        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (labels[i] > 0) map.computeIfAbsent(labels[i], k -> new ArrayList<>()).add(i);
        }

        List<List<Integer>> clusters = new ArrayList<>(map.values());
        clusters.sort((a, b) -> Integer.compare(b.size(), a.size())); // size desc
        return clusters;
    }

    static void expandCluster(List<MBR> mbrs, int[] labels, int pointIdx,
                              List<Integer> neighbors, int clusterId,
                              double eps, int minPts) {
        labels[pointIdx] = clusterId;
        ArrayDeque<Integer> queue = new ArrayDeque<>(neighbors);

        while (!queue.isEmpty()) {
            int j = queue.poll();

            if (labels[j] == -1) labels[j] = clusterId; // noise -> border
            if (labels[j] != 0) continue;

            labels[j] = clusterId;

            List<Integer> n2 = regionQuery(mbrs, j, eps);
            if (n2.size() >= minPts) {
                for (int k : n2) {
                    if (labels[k] == 0 || labels[k] == -1) queue.add(k);
                }
            }
        }
    }

    static List<Integer> regionQuery(List<MBR> mbrs, int idx, double eps) {
        MBR s = mbrs.get(idx);
        double ex2 = eps * eps;
        List<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < mbrs.size(); i++) {
            MBR t = mbrs.get(i);
            double dx = s.getCenterX() - t.getCenterX();
            double dy = s.getCenterY() - t.getCenterY();
            double d2 = dx * dx + dy * dy;
            if (d2 <= ex2) neighbors.add(i);
        }
        return neighbors;
    }

    // =======================
    // Extent helpers
    // =======================
    static MBR boundingExtentOfMbrs(List<MBR> mbrs) {
        if (mbrs.isEmpty()) return new MBR(0, 0, 0, 0);
        MBR e = new MBR(mbrs.get(0));
        for (int i = 1; i < mbrs.size(); i++) e.append(mbrs.get(i));
        return e;
    }

    static double dist2(double ax, double ay, double bx, double by) {
        double dx = ax - bx, dy = ay - by;
        return dx * dx + dy * dy;
    }

    static boolean fits80(MBR e, Config cfg) {
        return e.getWidth() <= cfg.CLUSTER_MAX_W && e.getHeight() <= cfg.CLUSTER_MAX_H;
    }

    // 80x80에 "근접" 점수 (작을수록 좋음)
    static double closenessScore(MBR e, Config cfg) {
        double dw = cfg.CLUSTER_MAX_W - e.getWidth();
        double dh = cfg.CLUSTER_MAX_H - e.getHeight();
        return dw * dw + dh * dh;
    }

    // Primary: count max, Secondary: closeness min, Third: area max
    static boolean better(int countA, double scoreA, double areaA,
                          int countB, double scoreB, double areaB) {
        if (countA != countB) return countA > countB;
        int c = Double.compare(scoreA, scoreB);
        if (c != 0) return c < 0;
        return areaA > areaB;
    }

    // =======================
    // Main-cluster pick with logs
    // =======================
    static class PickResult {
        final List<Integer> mainClusterIdx;
        final String status;
        final int clustersTotal;
        final int clustersFit80;
        PickResult(List<Integer> mainClusterIdx, String status, int clustersTotal, int clustersFit80) {
            this.mainClusterIdx = mainClusterIdx;
            this.status = status;
            this.clustersTotal = clustersTotal;
            this.clustersFit80 = clustersFit80;
        }
    }

    /**
     * Produces exactly ONE main cluster and ensures it fits 80x80.
     * Priority: maximize count, then closest to 80x80.
     * Logs:
     *  - CASE-A: clusters=0
     *  - CASE-C: clusters>0 but fit80=0
     *  - CASE-B: fit80>0
     */
    static PickResult pickMainClusterMaxCountClosest80(List<MBR> allMbrs, List<List<Integer>> clusters, Config cfg) {
        int total = clusters.size();

        // CASE-A: no clusters at all
        if (total == 0) {
            String status = "[CASE-A] DBSCAN clusters=0 (군집이 1개도 없음) -> build from empty via refine (fit80 enforced)";
            List<Integer> refined = refineMaxCountThenClosest80(Collections.emptyList(), allMbrs, cfg);
            return new PickResult(refined, status, 0, 0);
        }

        // Count clusters that fit 80x80 and pick best SEED among them (count max first)
        int fitCount = 0;
        int bestIdxFit = -1;
        int bestCount = -1;
        double bestScore = Double.POSITIVE_INFINITY;
        double bestArea = -1;

        for (int i = 0; i < clusters.size(); i++) {
            List<Integer> csIdx = clusters.get(i);
            MBR e = boundingExtentOfMbrs(mbrsByIndices(allMbrs, csIdx));
            if (!fits80(e, cfg)) continue;

            fitCount++;
            int count = csIdx.size();
            double score = closenessScore(e, cfg);
            double area = area(e);

            if (bestIdxFit < 0 || better(count, score, area, bestCount, bestScore, bestArea)) {
                bestIdxFit = i;
                bestCount = count;
                bestScore = score;
                bestArea = area;
            }
        }

        List<Integer> seed;
        String status;

        if (bestIdxFit >= 0) {
            status = "[CASE-B] clusters>0 AND fit80>0 -> seed = max-count (tie: closest80) among fit80 clusters";
            seed = new ArrayList<>(clusters.get(bestIdxFit));
        } else {
            // CASE-C: clusters exist, but none fits 80x80
            status = "[CASE-C] clusters>0 BUT fit80=0 (군집은 있으나 80x80 초과) -> seed = largest cluster, then prune to fit80";
            seed = new ArrayList<>(clusters.get(0)); // largest
            seed = pruneToFit80(seed, allMbrs, cfg);
        }

        // Refine: maximize count first, then closest to 80x80
        List<Integer> refined = refineMaxCountThenClosest80(seed, allMbrs, cfg);
        return new PickResult(refined, status, total, fitCount);
    }

    // =======================
    // Prune a set until it fits 80x80
    // =======================
    static List<Integer> pruneToFit80(List<Integer> input, List<MBR> allMbrs, Config cfg) {
        List<Integer> cur = new ArrayList<>(input);
        while (!cur.isEmpty()) {
            MBR e = boundingExtentOfMbrs(mbrsByIndices(allMbrs, cur));
            if (fits80(e, cfg)) return cur;

            // remove farthest from extent center
            double cx = e.getCenterX();
            double cy = e.getCenterY();

            int removeIdx = -1;
            double best = -1;
            for (int i = 0; i < cur.size(); i++) {
                MBR s = allMbrs.get(cur.get(i));
                double d2 = dist2(s.getCenterX(), s.getCenterY(), cx, cy);
                if (d2 > best) { best = d2; removeIdx = i; }
            }
            cur.remove(removeIdx);
        }
        return cur;
    }

    // =======================
    // Refine: Max count first, then closest to 80x80
    // - Greedy with 1-step lookahead (potentialTotal)
    // =======================
    static List<Integer> refineMaxCountThenClosest80(List<Integer> seed, List<MBR> allMbrs, Config cfg) {
        // picked set by index
        LinkedHashSet<Integer> picked = new LinkedHashSet<>(seed);

        // Ensure seed itself fits 80x80 (defensive)
        if (!picked.isEmpty()) {
            List<Integer> tmp = new ArrayList<>(picked);
            if (!fits80(boundingExtentOfMbrs(mbrsByIndices(allMbrs, tmp)), cfg)) {
                tmp = pruneToFit80(tmp, allMbrs, cfg);
                picked.clear();
                picked.addAll(tmp);
            }
        }

        // If empty, pick a reasonable start point (near center)
        if (picked.isEmpty() && !allMbrs.isEmpty()) {
            int start = 0;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < allMbrs.size(); i++) {
                MBR s = allMbrs.get(i);
                double d2 = dist2(s.getCenterX(), s.getCenterY(), 100.0, 100.0);
                if (d2 < best) { best = d2; start = i; }
            }
            picked.add(start);
        }

        while (true) {
            List<Integer> curList = new ArrayList<>(picked);
            MBR curE = boundingExtentOfMbrs(mbrsByIndices(allMbrs, curList));

            Integer bestCand = null;
            int bestPotentialTotal = -1;              // maximize this first
            double bestScore = Double.POSITIVE_INFINITY; // tie: closest to 80x80
            double bestArea = -1;                     // further tie: bigger area

            for (int candIdx = 0; candIdx < allMbrs.size(); candIdx++) {
                if (picked.contains(candIdx)) continue;
                MBR cand = allMbrs.get(candIdx);

                // extent after adding cand
                MBR e2 = new MBR(curE.minX, curE.minY, curE.maxX, curE.maxY);
                e2.append(cand);

                if (!fits80(e2, cfg)) continue;

                // lookahead: count how many more squares could still fit if we add them next (rough)
                int feasibleMore = 0;
                for (int otherIdx = 0; otherIdx < allMbrs.size(); otherIdx++) {
                    if (picked.contains(otherIdx) || otherIdx == candIdx) continue;
                    MBR other = allMbrs.get(otherIdx);
                    MBR e3 = new MBR(e2.minX, e2.minY, e2.maxX, e2.maxY);
                    e3.append(other);
                    if (fits80(e3, cfg)) feasibleMore++;
                }

                int potentialTotal = curList.size() + 1 + feasibleMore; // main objective heuristic
                double score2 = closenessScore(e2, cfg);
                double area2 = area(e2);

                if (potentialTotal > bestPotentialTotal ||
                        (potentialTotal == bestPotentialTotal && score2 < bestScore) ||
                        (potentialTotal == bestPotentialTotal && score2 == bestScore && area2 > bestArea)) {
                    bestCand = candIdx;
                    bestPotentialTotal = potentialTotal;
                    bestScore = score2;
                    bestArea = area2;
                }
            }

            if (bestCand == null) break;
            picked.add(bestCand);
        }

        // Final guarantee
        List<Integer> result = new ArrayList<>(picked);
        if (!result.isEmpty() && !fits80(boundingExtentOfMbrs(mbrsByIndices(allMbrs, result)), cfg)) {
            result = pruneToFit80(result, allMbrs, cfg);
        }
        return result;
    }

    // =======================
    // Outlier selection (all non-main)
    // =======================
    static List<Integer> selectOutliers(List<MBR> allMbrs, List<Integer> mainIdx) {
        Set<Integer> mainIds = new HashSet<>(mainIdx);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < allMbrs.size(); i++) {
            if (!mainIds.contains(i)) candidates.add(i);
        }
        return candidates;
    }

    // =======================
    // Main
    // =======================
    static void runWithMbrs(List<MBR> mbrs) {
        runWithMbrs(mbrs, new Config());
    }

    static void runWithMbrs(List<MBR> mbrs, Config cfg) {
        // 1) distance-based clusters
        List<List<Integer>> clusters = dbscan(mbrs, cfg.eps, cfg.minPts);

        // 2) pick ONE main cluster (max count priority, then closest 80x80)
        PickResult pick = pickMainClusterMaxCountClosest80(mbrs, clusters, cfg);
        List<Integer> mainClusterIdx = pick.mainClusterIdx;

        // 3) outliers (all non-main)
        List<Integer> outliersIdx = selectOutliers(mbrs, mainClusterIdx);

        // =======================
        // Output
        // =======================
        System.out.println("=== CONFIG ===");
        System.out.println("eps=" + cfg.eps + ", minPts=" + cfg.minPts +
                ", outlierCount=" + cfg.outlierCount + ", outlierMode=" + cfg.outlierMode);
        System.out.println("main constraint: extent.w<=80 && extent.h<=80");
        System.out.println();

        System.out.println("=== CLUSTER PICK LOG ===");
        System.out.println(pick.status);
        System.out.println("clustersTotal=" + pick.clustersTotal + ", clustersFit80=" + pick.clustersFit80);
        System.out.println();

        // (Optional) cluster overview
        System.out.println("=== DBSCAN CLUSTERS (size desc) ===");
        if (clusters.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (int i = 0; i < clusters.size(); i++) {
                List<Integer> csIdx = clusters.get(i);
                MBR e = boundingExtentOfMbrs(mbrsByIndices(mbrs, csIdx));
                System.out.printf("  Cluster[%d] size=%d extent=%s fit80=%s%n",
                        i, csIdx.size(), formatMbr(e), fits80(e, cfg));
            }
        }

        System.out.println("\n=== MAIN CLUSTER (ONE) ===");
        MBR me = boundingExtentOfMbrs(mbrsByIndices(mbrs, mainClusterIdx));
        System.out.println("Main cluster count: " + mainClusterIdx.size() + " / " + mbrs.size());
        System.out.println("Main cluster extent: " + formatMbr(me) +
                "  closenessScore=" + String.format("%.2f", closenessScore(me, cfg)));
        System.out.println("Squares in main cluster (each extent):");
        mainClusterIdx.stream().sorted()
                .forEach(idx -> System.out.println("  " + formatMbrWithId(idx, mbrs.get(idx))));

        System.out.println("\n=== OUTLIERS (top-K by option) ===");
        if (outliersIdx.isEmpty()) {
            System.out.println("(none)");
        } else {
            outliersIdx.stream().sorted()
                    .forEach(idx -> System.out.println("  " + formatMbrWithId(idx, mbrs.get(idx))));
            MBR oe = boundingExtentOfMbrs(mbrsByIndices(mbrs, outliersIdx));
            System.out.println("Outliers extent (chosen set): " + formatMbr(oe));
        }
    }

    public static void main(String[] args) {
        Config cfg = new Config();

        // --- Tune here if needed ---
        // cfg.eps = 12.0;
        // cfg.minPts = 2; // "2개면 군집"
        // cfg.outlierCount = 5;
        // cfg.outlierMode = OutlierMode.NEAREST_TO_CLUSTER;
        // cfg.seed = System.currentTimeMillis();

        List<MBR> mbrs = generateRandomMbrs(cfg);
        runWithMbrs(mbrs, cfg);
    }
}
