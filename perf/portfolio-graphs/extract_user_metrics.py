import glob, os, pandas as pd

def calc(jtl):
    df = pd.read_csv(jtl)
    if "success" in df.columns:
        df = df[df["success"] == True]
    elapsed = df["elapsed"].astype(float)
    p95 = elapsed.quantile(0.95)
    avg = elapsed.mean()
    t0 = df["timeStamp"].min()
    t1 = df["timeStamp"].max()
    duration_sec = max((t1 - t0) / 1000.0, 1e-9)
    tps = len(df) / duration_sec
    return avg, p95, tps

rows = []
for tag in ["redis", "mysql"]:
    for jtl in sorted(glob.glob(f"/Users/admin/Desktop/perf-results/{tag}/results_*.jtl")):
        users = int(os.path.basename(jtl).split("_")[1].split(".")[0])
        avg, p95, tps = calc(jtl)
        rows.append({"scenario": tag, "user_count": users, "avg_ms": round(avg,2), "p95_ms": round(p95,2), "throughput_rps": round(tps,2)})

out = pd.DataFrame(rows).sort_values(["user_count","scenario"])
out.to_csv("/Users/admin/Desktop/perf-results/user_metrics_by_count.csv", index=False)
print(out)
