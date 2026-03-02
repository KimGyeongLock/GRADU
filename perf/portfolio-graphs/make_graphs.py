import pandas as pd
import matplotlib.pyplot as plt

plt.style.use('seaborn-v0_8-whitegrid')

metrics = pd.read_csv('user_metrics.csv')
query = pd.read_csv('db_query.csv')

# 1) p95 comparison
plt.figure(figsize=(6.4, 4.2))
colors = ['#2E86DE', '#E67E22']
plt.bar(metrics['scenario'], metrics['p95_ms'], color=colors)
plt.title('p95 Latency Comparison (Lower is Better)')
plt.ylabel('Latency (ms)')
for x, y in enumerate(metrics['p95_ms']):
    plt.text(x, y, f'{y:.0f} ms', ha='center', va='bottom', fontsize=10)
plt.tight_layout()
plt.savefig('chart_p95_comparison.png', dpi=220)
plt.close()

# 2) Throughput comparison
plt.figure(figsize=(6.4, 4.2))
plt.bar(metrics['scenario'], metrics['throughput_rps'], color=colors)
plt.title('Throughput Comparison (Higher is Better)')
plt.ylabel('Requests/sec')
for x, y in enumerate(metrics['throughput_rps']):
    plt.text(x, y, f'{y:.1f}/s', ha='center', va='bottom', fontsize=10)
plt.tight_layout()
plt.savefig('chart_throughput_comparison.png', dpi=220)
plt.close()

# 3) DB query count reduction
before = float(query.loc[0, 'before'])
after = float(query.loc[0, 'after'])

plt.figure(figsize=(6.4, 4.2))
plt.bar(['Before (DB Aggregation)', 'After (Redis)'], [before, after], color=['#C0392B', '#27AE60'])
plt.title('DB Aggregation Query Count')
plt.ylabel('Count')
for x, y in enumerate([before, after]):
    plt.text(x, y, f'{y:.0f}', ha='center', va='bottom', fontsize=10)
plt.tight_layout()
plt.savefig('chart_db_query_reduction.png', dpi=220)
plt.close()

# 4) one-page summary chart (optional)
fig, axes = plt.subplots(1, 3, figsize=(15, 4.2))

axes[0].bar(metrics['scenario'], metrics['p95_ms'], color=colors)
axes[0].set_title('p95 Latency (ms)')
for x, y in enumerate(metrics['p95_ms']):
    axes[0].text(x, y, f'{y:.0f}', ha='center', va='bottom', fontsize=9)

axes[1].bar(metrics['scenario'], metrics['throughput_rps'], color=colors)
axes[1].set_title('Throughput (req/s)')
for x, y in enumerate(metrics['throughput_rps']):
    axes[1].text(x, y, f'{y:.1f}', ha='center', va='bottom', fontsize=9)

axes[2].bar(['Before', 'After'], [before, after], color=['#C0392B', '#27AE60'])
axes[2].set_title('DB Query Count')
for x, y in enumerate([before, after]):
    axes[2].text(x, y, f'{y:.0f}', ha='center', va='bottom', fontsize=9)

fig.suptitle('GRADU Ranking Optimization: MySQL vs Redis', fontsize=14)
fig.tight_layout()
fig.savefig('chart_summary_dashboard.png', dpi=220)
plt.close(fig)

print('Generated:')
print('- chart_p95_comparison.png')
print('- chart_throughput_comparison.png')
print('- chart_db_query_reduction.png')
print('- chart_summary_dashboard.png')
