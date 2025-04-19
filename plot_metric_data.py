import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import cm
import numpy as np

df = pd.read_csv("data/metric_data.csv")

colors = cm.viridis(np.linspace(0, 1, 4))

plt.figure(figsize=(10, 7))
widthOfLine = 4
plt.plot(df["x"], df["y_wifi"], label="Wi-Fi", linewidth=widthOfLine, color=colors[0])
plt.plot(df["x"], df["y_aprs"], label="APRS", linewidth=widthOfLine, color=colors[1])
plt.plot(df["x"], df["y_lora"], label="LoraWAN", linewidth=widthOfLine, color=colors[2])
plt.plot(df["x"], df["y_midband5g"], label="Midband 5G", linewidth=widthOfLine, color=colors[3])


def addHorizontalAxe(yValue, label, position=10000):
    plt.axhline(y=yValue, color='black', linestyle='--')
    plt.text(x=position, y=yValue + 0.00000005, s=label, color='black', fontsize=10, verticalalignment='bottom' )

addHorizontalAxe(5.0, "Full-HD video (~5Mbps)")
addHorizontalAxe(2.5, "HD-Ready audio (~2.5Mbps)")
addHorizontalAxe(0.320, "High-Quality audio (~320kbps)")
addHorizontalAxe(0.064, "Low-Quality audio (~64kbps)")
addHorizontalAxe(0.032, "Speech only audio (~32kbps)")
addHorizontalAxe(0.100, "Rich text data (~100kbps)")
addHorizontalAxe(0.001, "Position, Identification, Direction (1kbps)", 1.5)
addHorizontalAxe(0.0001, "Keep Alive Message (10bps)", 1.5)





plt.yscale('symlog', linthresh=0.001)
plt.xscale("log")
plt.legend(loc="upper right")
plt.xlim(1.0, 60000.0)
plt.ylim(-0.0001, 10000.0)
plt.xlabel("Distance (meters)")
plt.ylabel("Data Rate (Mbps)")
plt.show()

