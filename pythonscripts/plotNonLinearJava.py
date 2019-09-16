import csv
import matplotlib.pyplot as pyplot
import matplotlib.patches as mpatches
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


def plotLinearRegression(a, b, colour):
    model = LinearRegression()
    shapedA = numpy.array(a).reshape((-1, 1))
    model.fit(shapedA, b)
    x = numpy.linspace(0, 5, 3)
    pyplot.plot(x, x * model.coef_ + model.intercept_, color=colour, label="a")


# fetch input csv
path = str(sys.argv[1])
csvFile = readCSVFile(path)

# parse data
limits = list(csvFile[1].keys())
fib = list(map(lambda x: 1/x, csvFile[1].values()))
lin = list(csvFile[2].values())
lin2 = list(csvFile[3].values())
sys = list(csvFile[4].values())
fib_ = list(map(lambda x: 1/x, csvFile[6].values()))
lin_ = list(csvFile[7].values())
lin2_ = list(csvFile[8].values())
sys_ = list(csvFile[9].values())

max_fib = max(fib)
max_lin = max(lin)
max_lin2 = max(lin2)
max_sys = max(sys)

fib = list(map(lambda x: (x / max_fib), fib))
lin = list(map(lambda x: (x / max_lin), lin))
lin2 = list(map(lambda x: (x / max_lin2), lin2))
sys = list(map(lambda x: (x / max_sys), sys))
fib_ = list(map(lambda x: (x / max_fib), fib_))
lin_ = list(map(lambda x: (x / max_lin), lin_))
lin2_ = list(map(lambda x: (x / max_lin2), lin2_))
sys_ = list(map(lambda x: (x / max_sys), sys_))

# general information
f = pyplot.figure()
pyplot.title('Different CPU Benchmarks')
axes = pyplot.gca()
axes.set_ylabel('\'Independent Metric\'')
axes.set_ylim([0, 1])

axes.set_xlabel('Container CPU Quota')
axes.set_xlim([0, 1])

# reference line
x = numpy.linspace(-1, 2, 3)
pyplot.plot(x, x, linestyle='--', color='grey')

# plots
plotLinearRegression(limits, fib_, 'orange')
plotLinearRegression(limits, lin_, 'salmon')
plotLinearRegression(limits, sys_, 'teal')

# legends
ref_patch = mpatches.Patch(color='grey', label='Reference Machine')
fib_patch = mpatches.Patch(color='orange', label='Fibonacci (1 / exec time)')
lin_patch = mpatches.Patch(color='salmon', label='Linpack (GFLOPS)')
sys_patch = mpatches.Patch(color='teal', label='Sysbench (events / sec)')
pyplot.legend(handles=[ref_patch, fib_patch, sys_patch, lin_patch],
              loc='upper left')

# output
f.tight_layout()
f.savefig('output.pdf')

pyplot.show()
