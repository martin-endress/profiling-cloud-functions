import csv
import json
import matplotlib.patches as mpatches
import matplotlib.pyplot as pyplot
import numpy
import os
import scipy.stats as stats

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


def plotBoth(caption, values):
    plotHist(caption, values, False)
    plotHist(caption, values, True)


def plotHist(caption, values, withDistribution):
    count, bins, ignored = pyplot.hist(
        values, facecolor='g', density=withDistribution)

    pyplot.xlabel(caption)
    pyplot.ylabel('Executions')
    # pyplot.grid(True)
    legend = ['Number of Executions']
    if withDistribution:
        mu, sigma = stats.norm.fit(values)
        x = numpy.linspace(mu - 3*sigma, mu + 3*sigma, 100)
        pyplot.plot(x, stats.norm.pdf(x, mu, sigma))
        legend = ['Normal Distribution', 'Percentage of Executions']
    pyplot.legend(legend)
    pyplot.show()


memoryUtilization = []
cpuUtilisation = []
durationMS = []
maxMemoryUtilization = []

# TODO change this to a parameter
for root, dirs, files in os.walk('../profiles/20 runs limited/'):
#for root, dirs, files in os.walk('../profiles/cpu_15runs(2.0CPU)/'):
#for root, dirs, files in os.walk('../profiles/MEM_15runs(2.0CPU,2GBMEM)/'):
    for file in files:
        if file == 'meta.json':
            jsonFile = readJsonFile(root+'/'+file)
            memoryUtilization.append(jsonFile['averageMemoryUtilization'])
            cpuUtilisation.append(jsonFile['cpuUtilisation'])
            durationMS.append(jsonFile['durationMS'])
            maxMemoryUtilization.append(jsonFile['maxMemoryUtilization'])

plotHist('memoryUtilization', memoryUtilization, False)
plotBoth('cpuUtilisation', cpuUtilisation)
plotBoth('durationMS', durationMS)
plotBoth('maxMemoryUtilization', maxMemoryUtilization)

#csvFile = readCSVFile('artifacts/metrics.csv')
# plotFile(csvFile)
