import csv
import json
import matplotlib.patches as mpatches
import matplotlib.pyplot as pyplot
import numpy
import os
import scipy.stats as stats
import sys

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


def plotBoth(path, caption, values):
    plotHist(path, caption, values, False)
    plotHist(path, caption, values, True)


def plotHist(path, caption, values, withDistribution):
    f = pyplot.figure()
    count, bins, ignored = pyplot.hist(
        values, facecolor='g', density=withDistribution)

    pyplot.xlabel(caption)
    pyplot.ylabel('Executions')
    # pyplot.grid(True)
    legend = ['Number of Executions']
    if withDistribution:
        mu, sigma = stats.norm.fit(values)
        x = numpy.linspace(mu - 3 * sigma, mu + 3 * sigma, 100)
        pyplot.plot(x, stats.norm.pdf(x, mu, sigma))
        q95 = stats.norm.ppf(0.95, loc=mu, scale=sigma)
        q99 = stats.norm.ppf(0.99, loc=mu, scale=sigma)
        print(caption)
        print(" 95% " + str(q95))
        print(" 99% " + str(q99))
        pyplot.axvline(x=q99)
        legend = ['Normal Distribution', 'Percentage of Executions']
    pyplot.legend(legend)
    # pyplot.show()
    dist = ''
    if withDistribution:
        dist = 'withD'
    fileName = path + caption + dist + ".pdf"
    f.savefig(fileName)


# TODO validate input and
path = str(sys.argv[1])

memoryUtilization = []
cpuUtilisation = []
durationMS = []
maxMemoryUtilization = []
network = []

for root, dirs, files in os.walk(path):
    for file in files:
        if file == 'meta.json':
            jsonFile = readJsonFile(root+'/'+file)
            memoryUtilization.append(jsonFile['averageMemoryUtilization'])
            cpuUtilisation.append(jsonFile['cpuUtilisation'])
            durationMS.append(jsonFile['durationMS'])
            maxMemoryUtilization.append(jsonFile['maxMemoryUtilization'])
            network.append(jsonFile['networkUsage'])

plotBoth(path, 'memoryUtilization', memoryUtilization)
plotBoth(path, 'cpuUtilisation', cpuUtilisation)
plotBoth(path, 'durationMS', durationMS)
plotBoth(path, 'maxMemoryUtilization', maxMemoryUtilization)
plotBoth(path, 'networkUsage', network)

# csvFile = readCSVFile('artifacts/metrics.csv')
# plotFile(csvFile)
