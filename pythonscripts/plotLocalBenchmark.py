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
                dict(zip(map(tryMapToFloat, header), map(tryMapToFloat, entries))))
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


path = str(sys.argv[1])
maxQuota = int(sys.argv[2])

result = readCSVFile(path)[0]
limits = list(result.keys())
averages = list(result.values())

model = LinearRegression(fit_intercept=False)
shapedLimits = numpy.array(limits).reshape((-1, 1))
model.fit(shapedLimits, averages)
r_sq = model.score(shapedLimits, averages)

print('coefficient of determination: ', r_sq)
print('intercept:', model.intercept_)
print('slope:', model.coef_)

f = pyplot.figure()
pyplot.title('Local Linpack')
axes = pyplot.gca()
axes.set_ylabel('GFlops')
axes.set_ylim([0, 180])
pyplot.yticks(range(0, 181, 20))

axes.set_xlabel('Quota')
axes.set_xlim([0, maxQuota])
#pyplot.xticks(range(0, 3300, 400))

# Plot measurements
pyplot.plot(limits, averages, 'o', markerfacecolor='lightgray',
            markeredgecolor='grey')

# Plot regression model
x = numpy.linspace(0, 5, 3)
pyplot.plot(x, x * model.coef_ + model.intercept_,
            linestyle='--', color='orange')

f.tight_layout()

f.savefig('output.pdf')
pyplot.show()
