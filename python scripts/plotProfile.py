import csv
import matplotlib.pyplot as pyplot
import numpy

'''
This python script plots profiles created by the stats retriever.

available items in dict:
    local,user,system,stats_time,
    stats_total_cpu_usage,cpu_0,cpu_1,cpu_2,cpu_3,
    rx_bytes,rx_dropped,rx_errors,rx_packets,
    tx_bytes,tx_dropped,tx_errors,tx_packets
'''


# returns list of dictionaries for from a csv file.
# one entry is one row in the csv
def readCSVFile(fileName):
    with open(fileName) as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            dictionary = [{k: int(v) for k, v in row.items()}
                          for row in csv.DictReader(csvfile)]
    return dictionary


# returns a column of the csv file
def getEntries(l, key):
    return [d[key] for d in l]


csvFile = readCSVFile('../profiles/Metrics 2019-07-12T14:59:15.963862841.csv')
localT = getEntries(csvFile, "local")
recievedBytes = getEntries(csvFile, "rx_bytes")
pyplot.plot(localT, recievedBytes, 'ro')
pyplot.show()
