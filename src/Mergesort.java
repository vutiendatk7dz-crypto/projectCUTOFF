@SuppressWarnings({"rawtypes", "unchecked"})
public class Mergesort {

    private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {
        // Sao chép đoạn [lo..hi] sang mảng phụ aux[]
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }

        // Trộn ngược lại vào mảng a[] (giữ tính ổn định khi hai khóa bằng nhau lấy nửa trái)
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if      (i > mid)              a[k] = aux[j++];
            else if (j > hi)               a[k] = aux[i++];
            else if (less(aux[j], aux[i])) a[k] = aux[j++];
            else                           a[k] = aux[i++];
        }
    }

    private static void sort(Comparable[] a, Comparable[] aux, int lo, int hi, int cutoff, boolean stopIfSorted) {
        // Cải tiến 1 (Bài giảng tr. 238): dùng Insertion sort cho mảng con kích thước <= cutoff
        // Khi cutoff = 1, điều kiện trở thành (hi <= lo) -> Mergesort nguyên bản (tr. 230)
        if (hi <= lo + cutoff - 1) {
            if (cutoff > 1) {
                Insertionsort.sort(a, lo, hi);
            }
            return;
        }

        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid, cutoff, stopIfSorted);
        sort(a, aux, mid + 1, hi, cutoff, stopIfSorted);

        // Cải tiến 2 (Bài giảng tr. 239): dừng nếu phần tử lớn nhất nửa trái <= nhỏ nhất nửa phải
        if (stopIfSorted && !less(a[mid + 1], a[mid])) return;

        merge(a, aux, lo, mid, hi);
    }

    /** Sắp xếp với tham số CUTOFF (tắt Cải tiến 2 để đo độc lập ảnh hưởng của CUTOFF). */
    public static void sort(Comparable[] a, int cutoff) {
        sort(a, cutoff, false);
    }

    /** Sắp xếp với tùy chọn bật/tắt Cải tiến 2 (stopIfSorted). */
    public static void sort(Comparable[] a, int cutoff, boolean stopIfSorted) {
        if (a == null || a.length <= 1) return;
        Comparable[] aux = new Comparable[a.length]; // Cấp phát 1 lần ngoài hàm đệ quy (tr. 230)
        sort(a, aux, 0, a.length - 1, cutoff, stopIfSorted);
    }

    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }

    /** Kiểm tra tính đúng đắn của kết quả sắp xếp (Bài giảng tr. 192). */
    public static boolean isSorted(Comparable[] a) {
        for (int i = 1; i < a.length; i++) {
            if (less(a[i], a[i - 1])) return false;
        }
        return true;
    }
}