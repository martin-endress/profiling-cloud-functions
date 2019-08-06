import csv
import matplotlib.pyplot as pyplot
import numpy
import os
import json
from scipy.stats import norm

'''
This python script plots meta data of multiple profiles created by the stats retriever.

available items in dict:
    local,user,system,stats_time,
    stats_total_cpu_usage,cpu_0,cpu_1,cpu_2,cpu_3,
    memory_usage,memory_cache,memory_limit
    rx_bytes,rx_dropped,rx_errors,rx_packets,
    tx_bytes,tx_dropped,tx_errors,tx_packets
'''


def readJsonFile(fileName):
    with open(fileName) as jsonFile:
        data = json.load(jsonFile)
        return data


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


def plotFile(jsonFile, folder):
    return jsonFile['averageMemoryUtilization']


cpuUtilization = []
for root, dirs, files in os.walk('../profiles/run3/'):
    for file in files:
        if file == 'meta.json':
            jsonFile = readJsonFile(root+'/'+file)
            cpuUtilization.append(plotFile(jsonFile, root))

count, bins, ignored = pyplot.hist(cpuUtilization, normed=True)
mu, sigma = norm.fit(cpuUtilization)
pyplot.plot(bins, 1/(sigma * numpy.sqrt(2 * numpy.pi)) * numpy.exp( - (bins - mu)**2 / (2 * sigma**2) ), linewidth=1.5, color='r')

#pyplot.hist(cpuUtilization)
pyplot.show()

#csvFile = readCSVFile('artifacts/metrics.csv')
# plotFile(csvFile)
