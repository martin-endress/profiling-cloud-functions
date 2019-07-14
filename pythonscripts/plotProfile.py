import csv
import matplotlib.pyplot as pyplot
import numpy
from os import listdir
from os.path import isfile, join

'''
This python script plots profiles created by the stats retriever.

available items in dict:
    local,user,system,stats_time,
    stats_total_cpu_usage,cpu_0,cpu_1,cpu_2,cpu_3,
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


def plotFile(file):
    time = getEntries(file, "time")
    #bytesRecieved = getDeltaEntries(file, "rx_bytes")
    #bytesSent = getDeltaEntries(file, "tx_bytes")
    cgroupCpuUsage = getDeltaEntries(file, "user")
    statsCpuUsage = getDeltaEntries(file, "stats_total_cpu_usage")

    fig, ax1 = pyplot.subplots()

    color = 'tab:red'
    ax1.set_xlabel('time (ms)')
    ax1.set_ylabel('cgroupCpuUsage', color=color)
    # ax1.set_ylim([0,800])
    ax1.plot(time, cgroupCpuUsage, color=color)
    ax1.tick_params(axis='y', labelcolor=color)

    ax2 = ax1.twinx()

    color = 'tab:blue'
    ax2.set_ylabel('statsCpuUsage', color=color)
    # ax2.set_ylim([0,8000000000])
    ax2.plot(time, statsCpuUsage, color=color)
    ax2.tick_params(axis='y', labelcolor=color)

    fig.tight_layout()
    fig.savefig('artifacts/output.pdf')
    #pyplot.show()

csvFile = readCSVFile('artifacts/metrics.csv')
plotFile(csvFile)
