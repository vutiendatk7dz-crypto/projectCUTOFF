@SuppressWarnings({"rawtypes", "unchecked"})
public class Insertionsort {

    public static void sort(Comparable[] a) {
        sort(a, 0, a.length - 1);
    }

    /** Sắp xếp chèn trên đoạn [lo..hi] theo cài đặt bài giảng. */
    public static void sort(Comparable[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            for (int j = i; j > lo && less(a[j], a[j - 1]); j--) {
                exch(a, j, j - 1);
            }
        }
    }

    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }

    private static void exch(Comparable[] a, int i, int j) {
        Comparable swap = a[i];
        a[i] = a[j];
        a[j] = swap;
    }
}
