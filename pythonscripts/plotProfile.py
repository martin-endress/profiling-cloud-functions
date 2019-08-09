import csv
import matplotlib.pyplot as pyplot
import numpy
import os

'''
This python script plots profiles created by the stats retriever.

available items in dict:
    stats_time,
    stats_total_cpu_usage,cpu_0,cpu_1,cpu_2,cpu_3,
    memory_usage,memory_cache,memory_limit
    rx_bytes,rx_dropped,rx_errors,rx_packets,
    tx_bytes,tx_dropped,tx_errors,tx_packets
'''


def readCSVFile(fileName):
    """
    returns list of dictionaries for from a csv file.  
    one entry is one row in the csv
    """
    with open(fileName) as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            dictionary = [{k: tryMapToInt(v) for k, v in row.items()}
                          for row in csv.DictReader(csvfile)]
    return dictionary


def tryMapToInt(entry):
    try:
        return int(entry)
    except ValueError:
        return entry


def getEntries(l, key):
    """ returns a column of the csv file """
    return [d[key] for d in l]


def getDeltaEntries(l, key):
    """ prepend option is optional -> first value is returned as is, so the length is the same """
    return numpy.diff(getEntries(l, key), prepend=0)


def getOptimal(time, delta):
    return list(map(lambda x: x * delta, time))


def plotFile(file, folder):
    time = getEntries(file, 'stats_time')
    bytesRecieved = getDeltaEntries(file, 'rx_bytes')
    bytesSent = getDeltaEntries(file, 'tx_bytes')
    statsCpuUsage = getDeltaEntries(file, 'stats_total_cpu_usage')
    memoryLimit = getEntries(file, 'memory_limit')
    memoryUsage = getEntries(file, 'memory_usage')

    optimalBytes = getOptimal(time, 20)

    fig, ax1 = pyplot.subplots()

    color = 'tab:blue'
    ax1.set_ylabel('statsCpuUsage', color=color)
    ax1.set_ylim([0, 15E8])
    ax1.plot(time, statsCpuUsage, color=color)
    ax1.tick_params(axis='y', labelcolor=color)

    ax2 = ax1.twinx()
    color = 'tab:red'
    ax2.set_xlabel('time (ms)')
    ax2.set_ylabel('memoryUsage', color=color)
    ax2.set_ylim([0, 900E6])
    ax2.plot(time, memoryUsage, color=color)
    ax2.tick_params(axis='y', labelcolor=color)

    ax3 = ax1.twinx()
    ax3.spines["right"].set_position(("axes", 1.2))
    color = 'tab:orange'
    ax3.set_ylabel('bytesRecieved', color=color)
    #ax3.set_ylim([0, 900000])
    #ax3.plot(time, bytesRecieved, color=color)
    ax3.tick_params(axis='y', labelcolor=color)

    fig.tight_layout()
    fig.savefig(folder+'/output.pdf')
    # pyplot.show()


for root, dirs, files in os.walk('../profiles/cpu_15runs(2.0CPU)/'):
    for file in files:
        if file.endswith('cs.csv'):
            csvFile = readCSVFile(root+'/'+file)
            plotFile(csvFile, root)

#csvFile = readCSVFile('artifacts/metrics.csv')
# plotFile(csvFile)
