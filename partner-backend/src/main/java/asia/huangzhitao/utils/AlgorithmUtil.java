package asia.huangzhitao.utils;

import org.omg.CORBA.INTERNAL;

import java.util.*;

import static org.apache.commons.lang3.math.NumberUtils.min;

/**
 * 最短编辑距离算法
 *
 * @author huang
 * @date 2024/01/22
 */
public final class AlgorithmUtil {
    private AlgorithmUtil() {
    }

    /**
     * 最短路径编辑算法
     *
     * @param tagList1 标签一
     * @param tagList2 标签二
     * @return 距离
     */
//    public static int minDistance(List<String> tagList1, List<String> tagList2) {
//        int n = tagList1.size();
//        int m = tagList2.size();
//
//        if (n * m == 0) {
//            return 999;
//        }
//
//        int[][] d = new int[n + 1][m + 1];
//        for (int i = 0; i < n + 1; i++) {
//            d[i][0] = i;
//        }
//
//        for (int j = 0; j < m + 1; j++) {
//            d[0][j] = j;
//        }
//
//        for (int i = 1; i < n + 1; i++) {
//            for (int j = 1; j < m + 1; j++) {
//                int left = d[i - 1][j] + 1;
//                int down = d[i][j - 1] + 1;
//                int left_down = d[i - 1][j - 1];
//                if (!tagList1.get(i - 1).equalsIgnoreCase(tagList2.get(j - 1))) {
//                    left_down += 1;
//                }
//                d[i][j] = Math.min(left, Math.min(down, left_down));
//            }
//        }
//        return d[n][m];
        public static int minDistance(List<String> tagList1, List<String> tagList2) {
            if (tagList1 == null || tagList2 == null) {
                throw new IllegalArgumentException("Tag lists must not be null");
            }

            // 将两个列表转换为集合，以便进行集合运算
            Set<String> set1 = new HashSet<>(tagList1);
            Set<String> set2 = new HashSet<>(tagList2);

            // 计算交集和并集的大小
            int intersectionSize = intersection(set1, set2).size();
            int unionSize = union(set1, set2).size();

            // 计算杰卡德系数
            double jacCardSimilarity = (double) intersectionSize / unionSize;

            // 转换为距离，距离越小表示越相似
            return (int) Math.round((1 - jacCardSimilarity) * 100);
        }
        // 计算两个集合的交集
        private static Set<String> intersection(Set<String> set1, Set<String> set2) {
            Set<String> result = new HashSet<>(set1);
            result.retainAll(set2);
            return result;
        }
        // 计算两个集合的并集
        private static Set<String> union(Set<String> set1, Set<String> set2) {
            Set<String> result = new HashSet<>(set1);
            result.addAll(set2);
            return result;
        }



}
