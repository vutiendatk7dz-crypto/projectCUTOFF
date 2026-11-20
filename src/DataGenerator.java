import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DataGenerator {
    private static final long SEED = 42L;
//1. dãy
    public static void main(String[] args) throws IOException {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();

        System.out.println("=== Sinh bộ dữ liệu thí nghiệm (SEED = " + SEED + ") ===");

        // 1. Dãy 5 kích thước nhân đôi cho Doubling Hypothesis & khảo sát CUTOFF theo N
        writeArray("data/random_62500.txt", generateKnuthShuffled(62_500, SEED));
        writeArray("data/random_125k.txt",  generateKnuthShuffled(125_000, SEED + 1));
        writeArray("data/random_250k.txt",  generateKnuthShuffled(250_000, SEED + 2));
        writeArray("data/random_500k.txt",  generateKnuthShuffled(500_000, SEED + 3));
        writeArray("data/random_1m.txt",    generateKnuthShuffled(1_000_000, SEED + 4));

        // 2. Các phân bố đặc trưng ở N = 500,000 (Best, Partially Sorted, Worst, Duplicate Keys)
        writeArray("data/sorted_500k.txt",           generateSorted(500_000));
        writeArray("data/partially_sorted_500k.txt", generatePartiallySorted(500_000, SEED + 5));
        writeArray("data/reverse_500k.txt",          generateReverse(500_000));
        writeArray("data/few_unique_500k.txt",       generateFewUnique(500_000, 100, SEED + 6));

        System.out.println("Hoàn tất sinh 9 file dữ liệu trong thư mục data/.");
    }

    /** Sinh hoán vị ngẫu nhiên phân bố đều bằng thuật toán Knuth Shuffle (Bài giảng tr. 216-218). */
    private static int[] generateKnuthShuffled(int n, long seed) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;

        Random rand = new Random(seed);
        for (int i = 0; i < n; i++) {
            int r = rand.nextInt(i + 1); // Chọn r ngẫu nhiên trong đoạn [0, i]
            int swap = a[i];
            a[i] = a[r];
            a[r] = swap;
        }
        return a;
    }

    /** Mảng đã sắp xếp tăng dần (Best case của Insertion Sort, tr. 208). */
    private static int[] generateSorted(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;
        return a;
    }

    /**
     * Mảng sắp xếp một phần (Partially-sorted, tr. 210):
     * Tráo cục bộ trong từng khối độ dài 6 để tổng số inversion luôn tuyến tính O(N).
     */
    private static int[] generatePartiallySorted(int n, long seed) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;

        Random rand = new Random(seed);
        int blockSize = 6;
        for (int start = 0; start < n; start += blockSize) {
            int end = Math.min(start + blockSize, n);
            for (int i = start; i < end; i++) {
                int r = start + rand.nextInt(i - start + 1);
                int tmp = a[i];
                a[i] = a[r];
                a[r] = tmp;
            }
        }
        return a;
    }

    /** Mảng sắp xếp giảm dần (Worst case của Insertion Sort, tr. 208). */
    private static int[] generateReverse(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = n - i;
        return a;
    }

    /** Mảng nhiều khóa trùng lặp (100 giá trị phân biệt). */
    private static int[] generateFewUnique(int n, int distinctKeys, long seed) {
        Random rand = new Random(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = rand.nextInt(distinctKeys);
        return a;
    }

    private static void writeArray(String path, int[] a) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(Integer.toString(a.length));
            bw.newLine();
            for (int v : a) {
                bw.write(Integer.toString(v));
                bw.newLine();
            }
        }
        File f = new File(path);
        System.out.printf("  Đã tạo %-32s | N = %,9d | %.2f MB%n", path, a.length, f.length() / (1024.0 * 1024.0));
    }
}