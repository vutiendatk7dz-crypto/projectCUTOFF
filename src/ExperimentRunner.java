import java.io.*;
import java.util.Locale;

public class ExperimentRunner {

    // Bao gồm 1 (Mergesort gốc), 7 (giáo trình cũ), 10 (giáo trình mới) và dải rộng tới 100
    private static final int[] CUTOFFS = {1, 2, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 40, 50, 64, 100};
    private static final int TRIALS = 10;

    private static final String[][] EXP1_DATASETS = {
            {"random_125k",           "data/random_125k.txt",           "125000",  "random"},
            {"random_500k",           "data/random_500k.txt",           "500000",  "random"},
            {"random_1m",             "data/random_1m.txt",             "1000000", "random"},
            {"sorted_500k",           "data/sorted_500k.txt",           "500000",  "sorted"},
            {"partially_sorted_500k", "data/partially_sorted_500k.txt", "500000",  "partially_sorted"},
            {"reverse_500k",          "data/reverse_500k.txt",          "500000",  "reverse"},
            {"few_unique_500k",       "data/few_unique_500k.txt",       "500000",  "few_unique"}
    };

    private static final String[][] DOUBLING_DATASETS = {
            {"62500",   "data/random_62500.txt"},
            {"125000",  "data/random_125k.txt"},
            {"250000",  "data/random_250k.txt"},
            {"500000",  "data/random_500k.txt"},
            {"1000000", "data/random_1m.txt"}
    };

    public static void main(String[] args) throws IOException {
        File resDir = new File("results");
        if (!resDir.exists()) resDir.mkdirs();

        saveSystemInfo("results/system_info.txt");

        // Bước 1: JVM Warm-up để JIT compiler tối ưu mã máy trước khi bấm giờ
        System.out.println("=== Bước 1: JVM Warm-up ===");
        Integer[] warmupArr = loadDataset("data/random_125k.txt");
        for (int w = 0; w < 5; w++) {
            for (int c : new int[]{1, 7, 10, 25}) {
                Integer[] copy = warmupArr.clone();
                Mergesort.sort(copy, c);
            }
        }
        System.out.println("Warm-up hoàn tất.\n");

        // Bước 2: Thí nghiệm 1 - Khảo sát giá trị CUTOFF tối ưu
        System.out.println("=== Bước 2: Chạy Thí nghiệm 1 (Khảo sát CUTOFF) ===");
        double[] sumTime1M = new double[CUTOFFS.length];

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("results/cutoff_results.csv")))) {
            pw.println("dataset,size,distribution,stop_if_sorted,cutoff,trial,time_ms");

            for (String[] ds : EXP1_DATASETS) {
                String name = ds[0];
                String path = ds[1];
                String size = ds[2];
                String dist = ds[3];

                System.out.println("Đang đo trên bộ dữ liệu: " + name + " ...");
                Integer[] original = loadDataset(path);

                for (int trial = 1; trial <= TRIALS; trial++) {
                    for (int idx = 0; idx < CUTOFFS.length; idx++) {
                        int cutoff = CUTOFFS[idx];
                        Integer[] arr = original.clone();

                        Stopwatch timer = new Stopwatch();
                        Mergesort.sort(arr, cutoff, false);
                        double timeMs = timer.elapsedTimeMillis();

                        if (!Mergesort.isSorted(arr)) {
                            throw new IllegalStateException("Lỗi sắp xếp: " + name + ", cutoff=" + cutoff);
                        }

                        if (name.equals("random_1m")) {
                            sumTime1M[idx] += timeMs;
                        }
                        pw.printf(Locale.US, "%s,%s,%s,false,%d,%d,%.4f%n", name, size, dist, cutoff, trial, timeMs);
                    }
                }
            }

            // Thí nghiệm bổ sung: bật Cải tiến 2 (stopIfSorted = true) trên random_500k & sorted_500k
            System.out.println("Đang đo đối chứng Cải tiến 2 (stopIfSorted = true)...");
            String[][] imp2Sets = {
                    {"random_500k", "data/random_500k.txt", "500000", "random"},
                    {"sorted_500k", "data/sorted_500k.txt", "500000", "sorted"}
            };
            for (String[] ds : imp2Sets) {
                Integer[] original = loadDataset(ds[1]);
                for (int trial = 1; trial <= TRIALS; trial++) {
                    for (int cutoff : new int[]{1, 7, 10, 20}) {
                        Integer[] arr = original.clone();
                        Stopwatch timer = new Stopwatch();
                        Mergesort.sort(arr, cutoff, true);
                        double timeMs = timer.elapsedTimeMillis();
                        if (!Mergesort.isSorted(arr)) throw new IllegalStateException("Lỗi sắp xếp (imp2)");
                        pw.printf(Locale.US, "%s,%s,%s,true,%d,%d,%.4f%n", ds[0], ds[2], ds[3], cutoff, trial, timeMs);
                    }
                }
            }
        }

        // Xác định tự động Best CUTOFF trên random_1m để chạy Doubling Test
        int bestIdx = 0;
        for (int i = 1; i < CUTOFFS.length; i++) {
            if (sumTime1M[i] < sumTime1M[bestIdx]) bestIdx = i;
        }
        int bestCutoff = CUTOFFS[bestIdx];
        System.out.printf("%n=> Best CUTOFF sơ bộ trên random_1m: %d (Mean = %.2f ms)%n%n",
                bestCutoff, sumTime1M[bestIdx] / TRIALS);

        // Bước 3: Thí nghiệm 2 - Doubling Hypothesis (Bài giảng mục 1.4, tr. 82)
        System.out.println("=== Bước 3: Chạy Thí nghiệm 2 (Doubling Hypothesis) ===");
        int[] doublingCutoffs = (bestCutoff == 1 || bestCutoff == 7 || bestCutoff == 10)
                ? new int[]{1, 7, 10}
                : new int[]{1, 7, 10, bestCutoff};

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("results/doubling_results.csv")))) {
            pw.println("size,cutoff,trial,time_ms");
            for (String[] ds : DOUBLING_DATASETS) {
                String size = ds[0];
                Integer[] original = loadDataset(ds[1]);
                System.out.println("  Doubling Test với N = " + size + " ...");

                for (int trial = 1; trial <= TRIALS; trial++) {
                    for (int cutoff : doublingCutoffs) {
                        Integer[] arr = original.clone();
                        Stopwatch timer = new Stopwatch();
                        Mergesort.sort(arr, cutoff, false);
                        double timeMs = timer.elapsedTimeMillis();
                        if (!Mergesort.isSorted(arr)) throw new IllegalStateException("Lỗi sắp xếp (doubling)");
                        pw.printf(Locale.US, "%s,%d,%d,%.4f%n", size, cutoff, trial, timeMs);
                    }
                }
            }
        }

        System.out.println("\nHoàn tất toàn bộ thí nghiệm! Dữ liệu đã lưu trong thư mục results/.");
    }

    private static Integer[] loadDataset(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            int n = Integer.parseInt(br.readLine().trim());
            Integer[] arr = new Integer[n];
            for (int i = 0; i < n; i++) {
                arr[i] = Integer.parseInt(br.readLine().trim());
            }
            return arr;
        }
    }

    private static void saveSystemInfo(String path) throws IOException {
        String info = String.format(
                "OS: %s %s (%s)%n" +
                        "CPU Logical Cores: %d%n" +
                        "Java Version: %s (%s)%n" +
                        "JVM Max Heap: %.2f MB%n" +
                        "Trials per config: %d%n",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                System.getProperty("java.version"),
                System.getProperty("java.vm.name"),
                Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0),
                TRIALS
        );
        try (FileWriter fw = new FileWriter(path)) {
            fw.write(info);
        }
        System.out.println("=== Thông số môi trường thí nghiệm ===\n" + info);
    }
}
