import csv
import matplotlib.pyplot as pyplot
import numpy
import os
import sys
from sklearn.linear_model import LinearRegression
from imageio import imread


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
            dictionary.append(
                dict(zip(map(tryMapToInt, header), map(tryMapToFloat, entries))))
    return dictionary


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


results = readCSVFile('../calibration/experiment_0/awsResults.csv')[0]

keys = list(results.keys())
values = list(results.values())

f = pyplot.figure()
axes = pyplot.gca()
axes.set_ylabel('GFlops')
axes.set_ylim([0, 40])
pyplot.yticks(range(0, 36, 10))
axes.set_xlabel('Memory in MB')
axes.set_xlim([0, 3200])
pyplot.title('AWS Linpack')
pyplot.xticks(range(0, 3200, 400))

pyplot.grid(linestyle='-')

pyplot.plot(keys, values, 'ro')
f.tight_layout()

f.savefig('output.pdf')
pyplot.show()
