import pandas as pd
import matplotlib.pyplot as plt

plt.style.use("seaborn-v0_8-whitegrid")

metrics = pd.read_csv("user_metrics_by_count.csv")
query = pd.read_csv("db_query.csv")

# 정렬
metrics = metrics.sort_values(["scenario", "user_count"])

# 시나리오 분리
mysql = metrics[metrics["scenario"].str.lower() == "mysql"]
redis = metrics[metrics["scenario"].str.lower() == "redis"]

# 1) User Count vs p95
plt.figure(figsize=(7.2, 4.4))
plt.plot(mysql["user_count"], mysql["p95_ms"], marker="o", label="MySQL")
plt.plot(redis["user_count"], redis["p95_ms"], marker="o", label="Redis")
plt.title("User Count vs p95 Latency")
plt.xlabel("User Count")
plt.ylabel("p95 (ms)")
plt.legend()
plt.tight_layout()
plt.savefig("chart_usercount_p95.png", dpi=220)
plt.close()

# 2) User Count vs Throughput
plt.figure(figsize=(7.2, 4.4))
plt.plot(mysql["user_count"], mysql["throughput_rps"], marker="o", label="MySQL")
plt.plot(redis["user_count"], redis["throughput_rps"], marker="o", label="Redis")
plt.title("User Count vs Throughput")
plt.xlabel("User Count")
plt.ylabel("Requests/sec")
plt.legend()
plt.tight_layout()
plt.savefig("chart_usercount_throughput.png", dpi=220)
plt.close()

# 3) User Count vs Avg Latency
plt.figure(figsize=(7.2, 4.4))
plt.plot(mysql["user_count"], mysql["avg_ms"], marker="o", label="MySQL")
plt.plot(redis["user_count"], redis["avg_ms"], marker="o", label="Redis")
plt.title("User Count vs Average Latency")
plt.xlabel("User Count")
plt.ylabel("Average Latency (ms)")
plt.legend()
plt.tight_layout()
plt.savefig("chart_usercount_avg.png", dpi=220)
plt.close()

# 4) DB 쿼리 카운트(막대)
before = float(query.loc[0, "before"])
after = float(query.loc[0, "after"])

plt.figure(figsize=(6.4, 4.2))
plt.bar(["Before (DB)", "After (Redis)"], [before, after], color=["#C0392B", "#27AE60"])
plt.title("DB Aggregation Query Count")
plt.ylabel("Count")
for x, y in enumerate([before, after]):
    plt.text(x, y, f"{y:.0f}", ha="center", va="bottom", fontsize=10)
plt.tight_layout()
plt.savefig("chart_db_query_reduction.png", dpi=220)
plt.close()

print("Generated:")
print("- chart_usercount_p95.png")
print("- chart_usercount_throughput.png")
print("- chart_usercount_avg.png")
print("- chart_db_query_reduction.png")
