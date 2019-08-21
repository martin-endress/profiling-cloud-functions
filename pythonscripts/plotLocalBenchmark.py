import csv
import matplotlib.pyplot as pyplot
import numpy
import json
import os
import sys
from imageio import imread


def readJsonFile(fileName):
    with open(fileName) as jsonFile:
        data = json.load(jsonFile)
        return data


def tryMapToFloat(entry):
    try:
        return float(entry)
    except ValueError:
        return entry


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


def metaPath(x):
    return '../profiles/Profile_08.21_14.43.28/profile_limit_' + str(round(x, 2)) + '/meta.json'


profileFolder = '../profiles/Profile_08.21_14.43.28'

durations = []
limits = list(numpy.arange(0.05, 1.01, 0.05))

for limit in limits:
    fileName = metaPath(limit)
    if not(os.path.isfile(fileName)):
        print(fileName + ' does not exist.')
    jsonFile = readJsonFile(fileName)
    durations.append(jsonFile['durationMS'])

print(limits)
print(durations)

# This data was fetched from docker logs and is not generally available (only for this example)
fibonacciExecTimes = [234998, 87808, 40307, 26611, 17092, 12904, 10810, 9187,
                      8006, 6968, 6127, 5727, 5116, 4716, 4467, 4093, 3754, 3576, 3348, 3061]

f = pyplot.figure()
axes = pyplot.gca()
axes.set_ylabel('execution time fibonacci benchmark')
axes.set_xlabel('docker quota')
axes.set_xlim([0, 1])
pyplot.title('Docker quota')

pyplot.plot(limits, durations)
#pyplot.plot(limits, fibonacciExecTimes)
#pyplot.plot(limits, numpy.subtract(durations, fibonacciExecTimes))
f.tight_layout()

# f.savefig('output.pdf')
pyplot.show()
