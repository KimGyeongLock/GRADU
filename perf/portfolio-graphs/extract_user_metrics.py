import argparse
import glob
import os
import pandas as pd


def calc(jtl_path):
    df = pd.read_csv(jtl_path, low_memory=False)
    if "success" in df.columns:
        success = df["success"].astype(str).str.lower().eq("true")
        df = df[success]
    elapsed = df["elapsed"].astype(float)
    p95 = elapsed.quantile(0.95)
    avg = elapsed.mean()
    t0 = df["timeStamp"].min()
    t1 = df["timeStamp"].max()
    duration_sec = max((t1 - t0) / 1000.0, 1e-9)
    tps = len(df) / duration_sec
    return avg, p95, tps


def extract_one(input_dir, scenario):
    rows = []
    for jtl in sorted(glob.glob(os.path.join(input_dir, "results_*.jtl"))):
        users = int(os.path.basename(jtl).split("_")[1].split(".")[0])
        avg, p95, tps = calc(jtl)
        rows.append(
            {
                "scenario": scenario,
                "user_count": users,
                "avg_ms": round(avg, 2),
                "p95_ms": round(p95, 2),
                "throughput_rps": round(tps, 2),
            }
        )
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", required=True)
    parser.add_argument("--output-csv", required=True)
    parser.add_argument("--scenario", default=None)
    args = parser.parse_args()

    scenario = args.scenario
    if scenario is None:
        scenario = os.path.basename(os.path.normpath(args.input_dir))
    rows = extract_one(args.input_dir, scenario)
    out = pd.DataFrame(rows).sort_values(["user_count", "scenario"])
    out.to_csv(args.output_csv, index=False)
    print(out)


if __name__ == "__main__":
    main()
