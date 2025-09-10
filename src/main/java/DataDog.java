public class DataDog {
   /* public class DatadogInvertedIndex {

        Map<String, Set<Set<String>>> invertedIndex = new HashMap<>();

        public void pushTags(List<String> tags) {
            Set<String> tagsSet = new HashSet<>(tags);
            for (String tag : tagsSet) {
                Set<Set<String>> targetDocuments = invertedIndex.getOrDefault(tag, new HashSet<>());
                targetDocuments.add(tagsSet);
                invertedIndex.put(tag, targetDocuments);
            }
        }

        public Set<String> searchTags(List<String> tags) {
            Set<String> allFoundTags = searchTag(tags.get(0)).stream()
                    .filter(s -> s.containsAll(tags))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            tags.forEach(allFoundTags::remove);
            return allFoundTags;
        }

        private Set<Set<String>> searchTag(String tag) {
            return invertedIndex.getOrDefault(tag, new HashSet<>());
        }

        public static void main(String[] args) {
            DatadogInvertedIndex s = new DatadogInvertedIndex();
            s.pushTags(List.of("apple","google", "facebook"));
            s.pushTags(List.of("banana","facebook"));
            s.pushTags(List.of("facebook", "google", "tesla"));
            s.pushTags(List.of("intuit", "google", "facebook"));

            Set<String> res1 = s.searchTags(List.of("apple"));
            Set<String> res2 = s.searchTags(List.of("facebook", "google"));
            Lis
        }
    }*/
}
