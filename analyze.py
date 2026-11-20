import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

os.makedirs("results", exist_ok=True)

df_cut = pd.read_csv("results/cutoff_results.csv")
df_dbl = pd.read_csv("results/doubling_results.csv")

# Tách thí nghiệm chính (stop_if_sorted == False) và đối chứng Cải tiến 2
df_main = df_cut[df_cut["stop_if_sorted"] == False].copy()
df_imp2 = df_cut[df_cut["stop_if_sorted"] == True].copy()

# Thống kê mô tả cho Thí nghiệm 1
stats = (
    df_main.groupby(["dataset", "size", "distribution", "cutoff"])["time_ms"]
    .agg(["count", "mean", "std", "median", "min", "max"])
    .reset_index()
)
stats["ci95"] = 1.96 * stats["std"] / np.sqrt(stats["count"])

# Thống kê cho Thí nghiệm 2 (Doubling Hypothesis)
dbl_stats = (
    df_dbl.groupby(["cutoff", "size"])["time_ms"]
    .agg(["count", "mean", "std"])
    .reset_index()
    .sort_values(["cutoff", "size"])
)

# --- XUẤT BẢNG MARKDOWN TỰ ĐỘNG ---
with open("results/summary_tables.md", "w", encoding="utf-8") as f:
    f.write("# Tổng hợp bảng thống kê thí nghiệm CUTOFF cho Mergesort\n\n")

    # Bảng 1: Tổng hợp CUTOFF tối ưu trên tất cả các bộ dữ liệu
    f.write("## Bảng 1: So sánh CUTOFF = 1, 7, 10 và Best CUTOFF theo từng bộ dữ liệu\n\n")
    f.write("| Bộ dữ liệu | Kích thước (N) | Phân bố | CUTOFF=1 (ms) | CUTOFF=7 (ms) | CUTOFF=10 (ms) | Best CUTOFF | Best Mean ± Std (ms) | Cải thiện vs CUTOFF=1 |\n")
    f.write("|---|---:|---|---:|---:|---:|---:|---:|---:|\n")
    for ds_name, g in stats.groupby("dataset", sort=False):
        t1 = g.loc[g["cutoff"] == 1, "mean"].values[0]
        t7 = g.loc[g["cutoff"] == 7, "mean"].values[0]
        t10 = g.loc[g["cutoff"] == 10, "mean"].values[0]
        best = g.loc[g["mean"].idxmin()]
        imp = (t1 - best["mean"]) / t1 * 100.0
        f.write(
            f"| `{ds_name}` | {int(best['size']):,} | {best['distribution']} "
            f"| {t1:.2f} | {t7:.2f} | {t10:.2f} | **{int(best['cutoff'])}** "
            f"| **{best['mean']:.2f} ± {best['std']:.2f}** | **+{imp:.2f}%** |\n"
        )

    # Bảng 2: Chi tiết trên bộ dữ liệu chuẩn random_1m (N = 1,000,000)
    f.write("\n## Bảng 2: Thống kê chi tiết theo CUTOFF trên `random_1m` (N = 1,000,000)\n\n")
    f.write("| CUTOFF | Mean (ms) | Std (ms) | Median (ms) | Min (ms) | Max (ms) | 95% CI (±ms) | Cải thiện vs CUTOFF=1 |\n")
    f.write("|---:|---:|---:|---:|---:|---:|---:|---:|\n")
    g1m = stats[stats["dataset"] == "random_1m"].sort_values("cutoff")
    base1m = g1m.loc[g1m["cutoff"] == 1, "mean"].values[0]
    for _, r in g1m.iterrows():
        imp = (base1m - r["mean"]) / base1m * 100.0
        f.write(
            f"| {int(r['cutoff'])} | {r['mean']:.2f} | {r['std']:.2f} | {r['median']:.2f} "
            f"| {r['min']:.2f} | {r['max']:.2f} | {r['ci95']:.2f} | {imp:+.2f}% |\n"
        )

    # Bảng 3: Doubling Hypothesis & Hồi quy Power Law (Mục 1.4 bài giảng)
    f.write("\n## Bảng 3: Kiểm chứng Doubling Hypothesis & Quy tắc hàm mũ $T(N) = a N^b$\n\n")
    for cutoff, g in dbl_stats.groupby("cutoff"):
        g = g.sort_values("size").reset_index(drop=True)
        # Hồi quy tuyến tính trên thang log2-log2: lg(T) = b * lg(N) + c
        lg_n = np.log2(g["size"].values)
        lg_t = np.log2(g["mean"].values / 1000.0)  # đổi sang giây giống bài giảng
        b, c = np.polyfit(lg_n, lg_t, 1)
        a = 2 ** c
        f.write(f"### CUTOFF = {int(cutoff)} — Mô hình hồi quy: $T(N) \\approx {a:.3e} \\times N^{{{b:.3f}}}$ giây\n\n")
        f.write("| N | Mean T(N) (ms) | Std (ms) | ratio = T(2N)/T(N) | lg(ratio) |\n")
        f.write("|---:|---:|---:|---:|---:|\n")
        prev_t = None
        for _, r in g.iterrows():
            if prev_t is None:
                ratio_str, lg_str = "—", "—"
            else:
                ratio = r["mean"] / prev_t
                ratio_str = f"{ratio:.3f}"
                lg_str = f"{np.log2(ratio):.3f}"
            f.write(f"| {int(r['size']):,} | {r['mean']:.2f} | {r['std']:.2f} | {ratio_str} | {lg_str} |\n")
            prev_t = r["mean"]
        f.write("\n")

    # Bảng 4: So sánh bật/tắt Cải tiến 2 (stopIfSorted)
    if not df_imp2.empty:
        f.write("## Bảng 4: Ảnh hưởng của Cải tiến 2 (`stopIfSorted`, bài giảng tr. 239) ở N = 500,000\n\n")
        f.write("| Bộ dữ liệu | CUTOFF | Tắt `stopIfSorted` (ms) | Bật `stopIfSorted` (ms) | Chênh lệch |\n")
        f.write("|---|---:|---:|---:|---:|\n")
        imp2_stats = df_imp2.groupby(["dataset", "cutoff"])["time_ms"].mean().reset_index()
        for _, r in imp2_stats.iterrows():
            t_off = stats[(stats["dataset"] == r["dataset"]) & (stats["cutoff"] == r["cutoff"])]["mean"].values[0]
            t_on = r["time_ms"]
            diff = (t_off - t_on) / t_off * 100.0
            f.write(f"| `{r['dataset']}` | {int(r['cutoff'])} | {t_off:.2f} | {t_on:.2f} | {diff:+.2f}% |\n")

print("Đã xuất bảng thống kê tại results/summary_tables.md")

# --- BIỂU ĐỒ 1: Thời gian chạy theo CUTOFF ở 3 kích thước mảng Random ---
fig, axes = plt.subplots(1, 3, figsize=(16, 4.8))
for ax, ds_name, title in zip(
        axes,
        ["random_125k", "random_500k", "random_1m"],
        ["Random (N = 125,000)", "Random (N = 500,000)", "Random (N = 1,000,000)"],
):
    sub = stats[stats["dataset"] == ds_name].sort_values("cutoff")
    best = sub.loc[sub["mean"].idxmin()]
    ax.errorbar(sub["cutoff"], sub["mean"], yerr=sub["ci95"], marker="o", capsize=3, linewidth=1.8, label="Mean ± 95% CI")
    ax.axvline(7, color="orange", linestyle="--", alpha=0.85, label="CUTOFF = 7 (cũ)")
    ax.axvline(10, color="green", linestyle="--", alpha=0.85, label="CUTOFF = 10 (mới)")
    ax.scatter([best["cutoff"]], [best["mean"]], color="red", s=90, zorder=5, label=f"Best = {int(best['cutoff'])}")
    ax.set_title(title)
    ax.set_xlabel("Giá trị CUTOFF")
    ax.set_ylabel("Thời gian chạy (ms)")
    ax.grid(True, linestyle=":", alpha=0.6)
    ax.legend(fontsize=8)

plt.tight_layout()
plt.savefig("results/cutoff_vs_time_sizes.png", dpi=200)
plt.close()

# --- BIỂU ĐỒ 2: Ảnh hưởng của CUTOFF trên 5 phân bố dữ liệu (N = 500,000) ---
plt.figure(figsize=(10.5, 6))
for ds_name, label in [
    ("random_500k", "Ngẫu nhiên đều (Knuth Shuffle)"),
    ("sorted_500k", "Đã sắp xếp (Best case)"),
    ("partially_sorted_500k", "Sắp xếp một phần (Linear inversions)"),
    ("reverse_500k", "Giảm dần (Worst case Insertion)"),
    ("few_unique_500k", "Nhiều khóa trùng (100 giá trị)"),
]:
    sub = stats[stats["dataset"] == ds_name].sort_values("cutoff")
    plt.errorbar(sub["cutoff"], sub["mean"], yerr=sub["ci95"], marker="o", capsize=3, linewidth=1.8, label=label)

plt.axvline(7, color="gray", linestyle="--", alpha=0.7, label="CUTOFF = 7 (cũ)")
plt.axvline(10, color="black", linestyle=":", alpha=0.7, label="CUTOFF = 10 (mới)")
plt.title("Ảnh hưởng của CUTOFF trên các phân bố dữ liệu (N = 500,000)")
plt.xlabel("Giá trị CUTOFF")
plt.ylabel("Thời gian trung bình (ms)")
plt.grid(True, linestyle=":", alpha=0.6)
plt.legend()
plt.tight_layout()
plt.savefig("results/cutoff_vs_time_distributions.png", dpi=200)
plt.close()

# --- BIỂU ĐỒ 3: So sánh trực tiếp CUTOFF = 1, 7, 10 và Best CUTOFF trên random_1m ---
sub1m = stats[stats["dataset"] == "random_1m"]
best1m = sub1m.loc[sub1m["mean"].idxmin()]
labels = [
    "CUTOFF = 1\n(Mergesort gốc)",
    "CUTOFF = 7\n(Giáo trình cũ)",
    "CUTOFF = 10\n(Giáo trình mới)",
    f"Best CUTOFF = {int(best1m['cutoff'])}\n(Tối ưu trên máy)",
]
values = [
    sub1m.loc[sub1m["cutoff"] == 1, "mean"].values[0],
    sub1m.loc[sub1m["cutoff"] == 7, "mean"].values[0],
    sub1m.loc[sub1m["cutoff"] == 10, "mean"].values[0],
    best1m["mean"],
]
colors = ["#7f8c8d", "#e67e22", "#27ae60", "#2980b9"]

plt.figure(figsize=(8.8, 5))
bars = plt.bar(labels, values, color=colors, width=0.55)
for bar, val in zip(bars, values):
    pct = (values[0] - val) / values[0] * 100.0
    txt = f"{val:.2f} ms" if val == values[0] else f"{val:.2f} ms\n({pct:+.1f}%)"
    plt.text(bar.get_x() + bar.get_width() / 2, val + max(values) * 0.015, txt, ha="center", va="bottom", fontsize=10, fontweight="bold")

plt.ylim(0, max(values) * 1.18)
plt.ylabel("Thời gian trung bình (ms)")
plt.title("So sánh hiệu năng CUTOFF trên random_1m (N = 1,000,000)")
plt.grid(axis="y", linestyle=":", alpha=0.6)
plt.tight_layout()
plt.savefig("results/speedup_comparison.png", dpi=200)
plt.close()

# --- BIỂU ĐỒ 4: Standard plot & Log-Log plot (Doubling Hypothesis, bài giảng tr. 79-80) ---
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5))
for cutoff, g in dbl_stats.groupby("cutoff"):
    g = g.sort_values("size")
    lbl = f"CUTOFF = {int(cutoff)}"
    ax1.plot(g["size"] / 1000.0, g["mean"], marker="o", linewidth=1.8, label=lbl)
    ax2.plot(np.log2(g["size"]), np.log2(g["mean"]), marker="o", linewidth=1.8, label=lbl)

ax1.set_title("Standard Plot: T(N) theo kích thước N")
ax1.set_xlabel("Kích thước đầu vào N (nghìn phần tử)")
ax1.set_ylabel("Thời gian chạy T(N) (ms)")
ax1.grid(True, linestyle=":", alpha=0.6)
ax1.legend()

ax2.set_title("Log-Log Plot: lg(T(N)) theo lg(N)")
ax2.set_xlabel("lg(N)")
ax2.set_ylabel("lg(T(N)) (ms)")
ax2.grid(True, linestyle=":", alpha=0.6)
ax2.legend()

plt.tight_layout()
plt.savefig("results/doubling_loglog.png", dpi=200)
plt.close()

print("Đã xuất 4 biểu đồ tại thư mục results/")