public class Stopwatch {
    private final long start;

    public Stopwatch() {
        start = System.nanoTime();
    }

    /** Trả về thời gian đã trôi qua tính bằng giây (đúng API bài giảng). */
    public double elapsedTime() {
        long now = System.nanoTime();
        return (now - start) / 1_000_000_000.0;
    }

    /** Trả về thời gian đã trôi qua tính bằng mili-giây để tiện lập bảng. */
    public double elapsedTimeMillis() {
        long now = System.nanoTime();
        return (now - start) / 1_000_000.0;
    }
}
