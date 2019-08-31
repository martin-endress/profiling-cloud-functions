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


measurements = []

keys = []
values = []


for n in range(14):
    result = readCSVFile('calibration/awsCalibration' +
                         str(n) + '/awsResults.csv')[0]
    keys.extend(list(result.keys()))
    values.extend(list(result.values()))

print(keys)
print(values)


f = pyplot.figure(figsize=(10.4,4.88))
axes = pyplot.gca()
axes.set_ylabel('GFlops')
axes.set_ylim([0, 36])
pyplot.yticks(range(0, 36, 10))
axes.set_xlabel('Memory in MB')
axes.set_xlim([0, 3200])
pyplot.title('AWS Linpack')
pyplot.xticks(range(0, 3300, 400))

pyplot.grid(linestyle='-')


# Plot Measurement
pyplot.plot(keys, values, 'o', markerfacecolor='lightgray',
            markeredgecolor='grey')

model = LinearRegression()
shapedKeys = numpy.array(keys).reshape((-1, 1))
model.fit(shapedKeys, values)

r_sq = model.score(shapedKeys, values)
print('coefficient of determination: ', r_sq)
print('intercept:', model.intercept_)
print('slope:', model.coef_)
x = numpy.linspace(0, 4000, 3)
# Plot regression model
pyplot.plot(x, x * model.coef_ + model.intercept_, linestyle='--',color='orange')

f.tight_layout()
f.savefig('output.pdf')
pyplot.show()
