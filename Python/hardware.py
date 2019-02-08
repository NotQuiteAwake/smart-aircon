import csv
import time
from multiprocessing import Lock, Value, Manager
from person_class import Person
from state_class import State

import serial
from pandas import read_csv

from lstm_class import keras_lstm

manager = Manager()

ser = serial.Serial('/dev/cu.usbmodem14101', 115200, timeout=1)
mutex = Lock()

act_LSTM = keras_lstm(_name='act_lstm',_n_epochs=5000, _verbose=0)
time_LSTM = keras_lstm(_name='time_lstm', _n_epochs=2000, _verbose=0)
record_flag = False


def flt_read():
	s = ser.readline().strip()
	while len(s) <= 3:
		s = ser.readline().strip()
	return s


def read_sequence(filename):
	dataframe = read_csv(filename, usecols=[1], engine='python', skipfooter=0)
	dataset = dataframe.values
	return dataset.reshape((len(dataset)))


def write_csv_file(filename, head, row):
	with open(filename, 'wb') as csv_file:
		writer = csv.writer(csv_file)
		if head is not None:
			writer.writerow(head)
		for i in range(len(row)):
			writer.writerow([i, row[i]])


def round_int(num):
	return int(num + 0.5)


def init():
	global tempRec, timedt, users, lastt, lstStamp, p_nxt, turnedOn, turnedOff, stStamp, nxtDiff, init_state
	global load_models
	time.sleep(2)
	print("Reading Data...")
	timedt = read_sequence('../data/timeSequence.csv')
	expdt_arr = read_sequence('../data/tempSequence.csv')

	person = users[prime_user['value']]

	t_list = list()
	for item in expdt_arr:
		t_list.append(item)

	person.set_exp(t_list)
	users[prime_user['value']] = person

	del t_list

	lastt = timedt[-1]
	timedt = time_LSTM.time_transform(timedt)

	if load_models == 1:
		time_LSTM.load_model()
		print("Turn-on model loaded from saved file")
	else:
		print("Training LSTM for turn-on time prediction...")
		time_LSTM.train(timedt)
		time_LSTM.save_model()
		print("Model saved to disk.")

	if record_flag:
		samp_len = 40
		ser.write(str(samp_len) + "#")
		
		print("Collecting Data...")
		for i in range(samp_len):
			s = flt_read()
			print(s)
			if s[1] == '.' or s[2] == '.':
				tempRec.append(float(s))

		print("Temperature data stored to ../data/temp_record.csv")
		write_csv_file("../data/temp_record.csv", ["timeStamp", "Temperature"], tempRec)

	else:
		ser.write("0#")
		temp_rec_arr = read_sequence("../data/temp_record.csv").tolist()
		for i in range(len(temp_rec_arr)):
			tempRec.append(temp_rec_arr[i])

	if load_models == 1:
		act_LSTM.load_model()
		print("Act temp model loaded from saved file")

	else:
		print("Training LSTM for actual temperature prediction...")
		act_LSTM.train(tempRec)
		act_LSTM.save_model()
		print("Model saved to disk.")

	stStamp = int(time.time())
	lstStamp = stStamp - (24 - lastt)
	nxtDiff = round_int(time_LSTM.predict(timedt[-3:]))
	print(nxtDiff)

	p_nxt = 0
	turnedOn.value = 0
	turnedOff = True
	ser.write('3#')
	s = flt_read()

	if mutex.acquire():
		init_state.value = 1
	mutex.release()


def loop():
	global tempRec, timedt, users, lastt, lstStamp, p_nxt, turnedOn, turnedOff, stStamp, nxtDiff, nxt_time, p_nxt_time
	global turnedOn

	while True:
		s = flt_read()
		if s == "FD08F7":
			print("Conditioner turned off by user.")
			turnedOff = True
			turnedOn.value = 0
		elif s == "FD8877":
			print("Conditioner turned on by user.")
			turnedOff = False
			turnedOn.value = 1
		cur = int(time.time())

		relStamp.value = int(cur - stStamp)
		time_diff = int(cur - lstStamp)

		mutex.acquire()
		if nxt_time.value != 0:
			p_nxt_time.value = nxt_time.value
			nxtDiff = (nxt_time.value - lstStamp % 24 + 24) % 24
			nxt_time.value = 0
		mutex.release()

		if time_diff + 1 == nxtDiff:
			turnedOn.value = 1
			lstStamp = cur + 1
			timedt.append(int(nxtDiff))
			timedt.pop(0)
			nxtDiff = round_int(time_LSTM.predict(timedt[-3:]))
			if mutex.acquire():
				p_nxt_time.value = (relStamp.value + nxtDiff) % 24
			mutex.release()

		if s[2] == '.' or s[1] == '.':
			print(relStamp.value % 24)
# 			print("Predicted time diff: " + str(nxtDiff))
			print("Actual Temperature: " + s)

			if mutex.acquire():
				tempRec.append(float(s))
				tempRec.pop(0)
				p_nxt = act_LSTM.predict(tempRec[-3:])[0, 0]
			mutex.release()
			print("Predicted Temperature: " + str(p_nxt))
			mutex.acquire()
			exp_list = users[prime_user['value']].get_exp_temp()
			exp_temp = exp_list[(relStamp.value + 1) % 24]
			state = users[prime_user['value']].get_state()

			if keep.value == 1:
				exp_temp = p_nxt + state.get_temp_diff()

			if forced_swing.value == 1:
				forced_swing.value = 0
				exp_temp = p_nxt + state.get_temp_diff()
			mutex.release()
			print("Target Temperature: " + str(exp_temp))
			if turnedOn.value == 1:
				turnedOff = False

				if exp_temp > p_nxt:
					ser.write("2#")
# 					print("turning on heating...")
				else:
					ser.write("1#")
# 					print("turning on cooling...")
			else:
				if not turnedOff:
					ser.write("3#")
				else: 
					ser.write("4#")
# 			turnedOn.value = 0
	ser.close()


def run(init_state_v, nxt_time_v, users_dict, temp_arr, record_flag_v,
		p_nxt_time_v, cur_time, prime_user_s, turnedOn_v, keep_v, forced_swing_v, load_models_v):
	global init_state, nxt_time, users, tempRec, record_flag, p_nxt_time, \
		relStamp, prime_user, turnedOn, keep, forced_swing, load_models
	init_state = init_state_v
	nxt_time = nxt_time_v
	users = users_dict
	tempRec = temp_arr
	record_flag = record_flag_v
	p_nxt_time = p_nxt_time_v
	relStamp = cur_time
	prime_user = prime_user_s
	turnedOn = turnedOn_v
	keep = keep_v
	forced_swing = forced_swing_v
	load_models = load_models_v

	init()
	loop()

# TODO: implement the DEMO mode


if __name__ == '__main__':
	tempRec = manager.list()
	users = manager.dict()
	init_state = Value('i', 0)
	nxt_time = Value('i', 0)
	run(init_state, nxt_time, users, tempRec)
