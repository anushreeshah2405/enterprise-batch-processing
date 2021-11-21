import csv
import random
from random import randint

records = 50000
start = 500000

end = start + records
ids = [start + i for i in range(records+1)]

names = ['Peter','Tony','Jack','Steve','Natasha','Bruce']
surnames = ['Parker', 'Stark', 'Rogers','Romanoff','Banner']
n = 10

with open("Users5.csv", 'w', newline='') as f:
    writer = csv.writer(f)
    for i in range(0,records):
        writer.writerow([ids[i],'test'+str(i)+'@gmail.com',random.choice(names),random.choice(surnames),''.join(["{}".format(randint(0, 9)) for num in range(0, n)])])
