# Portfolio Graphs (MySQL vs Redis)

## Files
- `user_metrics.csv`: latency/throughput metrics
- `db_query.csv`: DB query count before/after
- `make_graphs.py`: chart generator

## Run (local)
```bash
cd /Users/admin/Desktop/GRADU/perf/portfolio-graphs
python3 make_graphs.py
```

## Output
- `chart_p95_comparison.png`
- `chart_throughput_comparison.png`
- `chart_db_query_reduction.png`
- `chart_summary_dashboard.png`

## Run (Colab)
1. Upload `user_metrics.csv`, `db_query.csv`, `make_graphs.py`
2. Run:
```python
!python make_graphs.py
```
