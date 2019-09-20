import csv
import matplotlib.pyplot as pyplot
import numpy
import os
import sys

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
        reader = csv.reader(csvfile, delimiter=',')
        header = next(reader, None)
        dictionary = []
        for entries in reader:
            dictionary.append(dict(zip(header, map(tryMapToInt, entries))))
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
    print(file)
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
    ax1.set_xlabel('time (ms)')
    ax1.set_ylim([0, 12E8])
    ax1.plot(time, statsCpuUsage, color=color)
    ax1.tick_params(axis='y', labelcolor=color)

    time_ms = 30000
    memSize = 1024 * 1024 * 1024
    ax2 = ax1.twinx()
    color = 'tab:red'
    ax2.set_ylabel('memoryUsage', color=color)
    ax2.set_ylim([0, 1.2 * memSize])
    ax2.plot(time, memoryUsage, color=color)
    ax2.tick_params(axis='y', labelcolor=color)

    x = numpy.linspace(0, time_ms, 2)
    ax2.plot(x, x * (memSize / time_ms), linestyle=":", color='tab:blue', linewidth=1)
    #ax2.plot(x, x * (memSize / time_ms), ':',
    #         dashes=(1.1, 2.5), color='tab:blue', linewidth=1)
    #ax2.plot(x, x * (memSize / time_ms), ':',
    #         dashes=(1, 2), color='tab:red', linewidth=1)

    #netSize = 5* 1024 * 1024
    #ax3 = ax1.twinx()
    #ax3.spines["right"].set_position(("axes", 1.2))
    #color = 'tab:orange'
    #ax3.set_ylabel('bytesRecieved', color=color)
    #ax3.set_ylim([0, netSize])
    #ax3.plot(time, bytesRecieved, color=color)
    #ax3.tick_params(axis='y', labelcolor=color)

    fig.tight_layout()
    fig.savefig(folder+'/output.pdf')
    # pyplot.show()


path = str(sys.argv[1])

for root, dirs, files in os.walk(path):
    for file in files:
        if file.endswith('cs.csv'):
            csvFile = readCSVFile(root+'/'+file)
            plotFile(csvFile, root)

#csvFile = readCSVFile('artifacts/metrics.csv')
# plotFile(csvFile)
