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