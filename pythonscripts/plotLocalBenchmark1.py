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
            dictionary.append(dict(zip(header, map(tryMapToFloat, entries))))
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


def metaPath(x):
    return '../calibration/experiment_local_' + str(round(x, 2)) + '/local.csv'


limits = list(numpy.arange(0.25, 4.0, 0.25))

averages = []
maxs = []

for limit in limits:
    firstCsvEntry = readCSVFile(metaPath(limit))[0]
    averages.append(firstCsvEntry['average'])
    maxs.append(firstCsvEntry['max'])

x = numpy.array(limits).reshape((-1, 1))
y = numpy.array(averages)

model = LinearRegression()
model.fit(x, y)

r_sq = model.score(x, y)
print('coefficient of determination: ', r_sq)
print('intercept:', model.intercept_)
print('slope:', model.coef_)

f = pyplot.figure()
axes = pyplot.gca()
axes.set_ylabel('GFlops')
axes.set_xlabel('Quota')
# axes.set_xlim([0, 3200])
pyplot.title('Local Linpack')
# pyplot.xticks(range(0, 3200, 400))
#
# pyplot.grid(linestyle='-')
#
print(averages)

# Plot measurements
pyplot.plot(limits, averages, 'o', markerfacecolor='lightgray',
            markeredgecolor='grey')

# Plot regression model
pyplot.plot(x, x * model.coef_ + model.intercept_,
            linestyle='--', color='orange')

f.tight_layout()

f.savefig('output.pdf')
pyplot.show()
