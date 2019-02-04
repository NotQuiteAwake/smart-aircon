import serial
import time
from pandas import read_csv
import csv
from lstm_class import keras_lstm
from multiprocessing import Lock, Value, Manager

manager = Manager()

ser = serial.Serial('/dev/cu.usbmodem14101', 115200, timeout = 1)
mutex = Lock()

act_LSTM = keras_lstm(_n_epochs=5000, _verbose=0)
exp_LSTM = keras_lstm(_n_epochs=5000, _verbose=0)
time_LSTM = keras_lstm(_n_epochs=2000, _verbose=0)
record_flag = False


def fltRead():
	s = ser.readline().strip()
	while (len(s) <= 3):
		s = ser.readline().strip()
	return s


def readSequence(file):
	dataframe = read_csv(file, usecols=[1], engine='python', skipfooter=0)
	dataset = dataframe.values
	return dataset.reshape((len(dataset)))


def write_csv_file(file, head, row):
	with open(file, 'wb') as csv_file:
		writer = csv.writer(csv_file)
		if head is not None:
			writer.writerow(head)
		for i in range(len(row)):
			writer.writerow([i, row[i]])


def round_int(num):
	return int(num + 0.5)


def init():
	global tempRec, timedt, expdt, lastt, lstStamp, p_nxt, turnedOn, turnedOff, stStamp, nxtDiff, init_state

	time.sleep(2)
	print("Reading Data...")
	timedt = readSequence('../data/timeSequence.csv')
	expdt_arr = readSequence('../data/tempSequence.csv')
	for i in range(len(expdt_arr)):
		expdt.append(expdt_arr[i])

	print("Training LSTM for turn-on time prediction...")
	lastt = timedt[-1]
	timedt = time_LSTM.time_transform(timedt)
	time_LSTM.train(timedt)

	if record_flag :
		samp_len = 40
		ser.write(str(samp_len) + "#")
		
		print("Collecting Data...")
		for i in range(samp_len):
			s = fltRead()
			print(s)
			if(s[1] == '.' or s[2] == '.'): 
				tempRec.append(float(s))

		print("Temperature data stored to ../data/temp_record.csv")
		write_csv_file("../data/temp_record.csv", ["timeStamp", "Temperature"], tempRec)

	else :
		ser.write("0#")
		tempRec_arr = readSequence("../data/temp_record.csv").tolist()
		for i in range(len(tempRec_arr)):
			tempRec.append(tempRec_arr[i])

	print("Training LSTM for actual temperature prediction...")
	act_LSTM.train(tempRec)
	stStamp = int(time.time())
	lstStamp = stStamp - (24 - lastt); nxtDiff = round_int(time_LSTM.predict(timedt[-3 : ]))
	print(nxtDiff)

	p_nxt = 0
	turnedOn = False
	turnedOff = True
	ser.write('3#')
	s = fltRead()

	if mutex.acquire():
		init_state.value = 1
	mutex.release()

#	print("Init Complete")


def loop():
	global tempRec, timedt, expdt, lastt, lstStamp, p_nxt, turnedOn, turnedOff, stStamp, nxtDiff, nxt_time, p_nxt_time
	while True:
		s = fltRead()
		if s == "FD08F7":
			print("Conditioner turned off by user.")
			turnedOff = True; turnedOn = False
		elif s == "FD8877":
			print("Conditioner turned on by user.")
			turnedOff = False; turnedOn = True
		cur = int(time.time())
		relStamp.value = int(cur - stStamp)
		timeDiff = int(cur - lstStamp)

		mutex.acquire()
		if nxt_time.value != 0:
			p_nxt_time.value = nxt_time.value
			nxtDiff = (nxt_time.value - lstStamp % 24 + 24) % 24
			nxt_time.value = 0
		mutex.release()

		if (timeDiff + 1 == nxtDiff):
			turnedOn = True
			lstStamp = cur + 1
			timedt.append(int(nxtDiff))
			timedt.pop(0)
			nxtDiff = round_int(time_LSTM.predict(timedt[-3:]))
			if mutex.acquire():
				p_nxt_time.value = (relStamp.value + nxtDiff) % 24
			mutex.release()

		if s[2] == '.' or s[1] == '.':
			print(relStamp.value % 24)
			print("Predicted time diff: " + str(nxtDiff))
			print("A Temperature: " + s)
			if mutex.acquire():
				tempRec.append(float(s))
				tempRec.pop(0)
				p_nxt = act_LSTM.predict(tempRec[-3:])[0, 0]
			mutex.release()
			print("Predicted Temperature: " + str(p_nxt))
			print("E Temperature: " + str(expdt[(relStamp.value + 1) % 24]))
			if turnedOn:
				turnedOff = False
				if (expdt[(relStamp.value + 1) % 24] > p_nxt):
					ser.write("2#")
					print("turning on heating...")
				else:
					ser.write("1#")
					print("turning on cooling...")
			else:
				if not(turnedOff):
					ser.write("3#")
				else: 
					ser.write("4#")
			turnedOn = False
	ser.close()


def run(init_state_v, nxt_time_v, exp_arr, temp_arr, _record_flag, p_nxt_time_v, cur_time):
	global init_state, nxt_time, expdt, tempRec, record_flag, p_nxt_time, relStamp
	record_flag = _record_flag
	tempRec = temp_arr
	init_state = init_state_v
	expdt = exp_arr
	nxt_time = nxt_time_v
	p_nxt_time = p_nxt_time_v
	relStamp = cur_time
	init()
	loop()


if __name__ == '__main__':
	tempRec = manager.list()
	expdt = manager.list()
	init_state = Value('i', 0)
	nxt_time = Value('i', 0)
	run(init_state, nxt_time, expdt, tempRec)
