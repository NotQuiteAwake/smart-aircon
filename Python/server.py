# -*- coding:utf-8 -*-


from flask import *
from multiprocessing import Value, Lock, Manager
from ctypes import c_char_p
from person_class import Person
from state_class import State
import json

import logging
log = logging.getLogger('werkzeug')

manager = Manager()
app = Flask(__name__)
tasks = list()
temp = manager.list()
users = manager.dict()
member_list = manager.list()
nxt_time = Value('i', 0)
init_state = Value('i', 0)
cur_stamp = Value('i', 0)
p_nxt_time = Value('i', 0)
mutex = Lock()
prime_user = Value(c_char_p, "default")
states = manager.dict()


@app.route('/')
def service_test():
	return jsonify({'description': 'smart-air-conditioner'})


@app.errorhandler(404)
def not_found(error):
	return make_response(jsonify({'error': 'Not Found'}), 404)


@app.route('/post_tasks', methods=['POST'])
def handle_task():
	if not request.json or 'cmd' not in request.json:
		abort(400)
	task = {}
	# TODO: test usability
	task = json.loads(request.get_data())
	tasks.append(task)
	app.logger.error(request.get_data())
	return jsonify({"status": 1})


@app.route('/data_fetch', methods=['GET'])
def comp_task():
	# TODO: check params are right on Android
	# TODO: globalize the mutex lock
	if len(tasks) == 0:
		return jsonify({'status': -1, 'init_state': init_state.value})
	else:
		task = tasks[-1]
		tasks.pop(-1)
		cmd = task['cmd']
		ret = jsonify({'status': 1})
		if cmd == 'check_init_state':
			ret = jsonify({'status': 1, 'init_state': init_state.value})

		elif cmd == 'get_stat':
			temp_list = [x for x in temp]
			exp_list = users[prime_user['value']].get_exp_temp()
			ret =  jsonify({'status': 1, 'temp': temp_list,
							'exp_temp': exp_list, 'time': cur_stamp.value % 24,
							'p_time': p_nxt_time.value, 'isOn': turnedOn.value})

		elif cmd == 'set_exp_temp':
			person_id = task['person_id']
			exp_time = task['exp_time']
			exp_temp = task['exp_temp']
			if person_id in member_list:
				if mutex.acquire():
					person = users[person_id]
					person.set_exp_temp(exp_time, exp_temp)
					users[person_id] = person
				mutex.release()
				ret =  jsonify({'status': 1})
			else:
				ret = jsonify({'status': -1})

		elif cmd == 'set_exp_time':
			if mutex.acquire():
				nxt_time.value = task['exp_time']
			mutex.release()
			ret = jsonify({'status': 1})

		elif cmd == 'get_member_list':
			ret = jsonify({'status': 1, 'member_list': users.keys()})

		elif cmd == 'get_exp_temp':
			app.logger.info(task)
			person_id = task['person_id']
			if person_id in users:
				ret = jsonify({'status': 1, 'exp_temp': users[person_id].get_exp_temp()})
			else:
				ret = jsonify({'status': -1, 'exp_temp': users['default'].get_exp_temp()})

		elif cmd == 'get_prime_user':
			ret = jsonify({'status': 1, 'prime_user': prime_user['value']})

		elif cmd == 'set_user_presence':
			person_id = task['person_id']
			presence = task['presence']
			person = users[person_id]
			person.set_presence(presence)
			users[person_id] = person
			ret = jsonify({'status': 1})

		elif cmd == 'set_user_priority':
			person_id = task['person_id']
			priority = task['priority']
			person = users[person_id]
			person.set_priority(priority)
			users[person_id] = person
			ret = jsonify({'status': 1})

		# TODO: Implement findStateById to get the real state detail with state ID
		elif cmd == 'set_user_state':
			person_id = task['person_id']
			state_id = task['state_id']

			person = users[person_id]
			person.set_state(states[state_id])
			users[person_id] = person

			ret = jsonify({'status': 1})

		elif cmd == 'add_state' or cmd == 'set_state':
			state_id = task['state_id']
			temp_diff = task['temp_diff']
			states[state_id] = State(state_id, temp_diff)
			ret = jsonify({'status': 1})

		elif cmd == 'remove_state':
			del states[task['state_id']]
			ret = jsonify({'status': 1})

		elif cmd == 'add_user':
			person_id = task['person_id']
			priority = task['priority']
			exp_temp = task['exp_temp']
			state_id = task['state_id']
			# TODO: add findStateById
			users[person_id] = Person(person_id, exp_temp, priority, state_id)
			ret = jsonify({'status': 1})

		elif cmd == 'remove_user':
			person_id = task['person_id']
			del users[person_id]
			ret = jsonify({'status': 1})

		elif cmd == 'get_user':
			person_id = task['person_id']
			if person_id in users.keys():
				# TODO: check get_presence value correctness
				ret =  jsonify(users[person_id].to_dict())
			else:
				ret = jsonify({'status': -1})

		elif cmd == 'set_name':
			person_id = task['person_id']
			name = task['name']
			users[name] = users[person_id]
			del users[person_id]
			ret = jsonify({'status': 1})

		elif cmd == 'get_state_list':
			ret = jsonify({'status': 1, 'state_list': states.keys()})

		else:
			ret = jsonify({'status': -1})

		update_priority()
		return ret


def update_priority():
	#implement isOn
	global users, temp, states, prime_user, keep, forced_swing, turnedOn
	pre_prio = {'person_id': 'null', 'priority': -100}
	npr_prio = {'person_id': 'null', 'priority': -100}

	for key in users.keys():
		person = users[key]
		prec = person.get_presence()
		prio = person.get_priority()
		p_id = person.get_person_id()

		if npr_prio['priority'] < prio:
			npr_prio['person_id'] = p_id
			npr_prio['priority'] = prio

		if prec == 1 and pre_prio['priority'] < prio:
			pre_prio['person_id'] = p_id
			pre_prio['priority'] = prio

	mutex.acquire()
	if pre_prio['person_id'] == 'null':
		keep.value = 1
		prime_user['value'] = npr_prio['person_id']

	else:
		keep.value = 0
		prime_user['value'] = pre_prio['person_id']

	person = users[prime_user['value']]

	if turnedOn.value == 1 and person.is_on_time():
		forced_swing.value = 1
	else:
		forced_swing.value = 0
	mutex.release()

# THE PRIORITY SYSTEM:
# FIRST PRIORITY: USERS AT HOME
# SECOND PRIORITY: USERS WITH HIGHER PRIORITY VALUE

# 	on_start_state = users[prime_user].get_state()
# 	exp_list = users[prime_user].get_exp_temp()


# TODO: add mutex lock
# TODO: add precautions for state nonexistent


def run(init_state_v, nxt_time_v, users_dict, temp_arr, p_nxt_time_v,
		cur_stamp_v, prime_user_s, state_dict, turnedOn_v, keep_v, forced_swing_v):
	log.setLevel(logging.ERROR)
	global users, temp, nxt_time, init_state, p_nxt_time, \
		cur_stamp, prime_user, states, turnedOn, forced_swing, keep

	turnedOn = turnedOn_v
	keep = keep_v
	forced_swing = forced_swing_v

	users = users_dict
	temp = temp_arr
	nxt_time = nxt_time_v
	init_state = init_state_v
	p_nxt_time = p_nxt_time_v
	cur_stamp = cur_stamp_v
	prime_user = prime_user_s
	states = state_dict
	app.run(host='0.0.0.0', port=8080)


if __name__ == '__main__':
	global users, temp, states, turnedOn, forced_swing, keep
	print("Running in DEMO mode.")
	init_state.value = 1

	states = manager.dict()
	users = manager.dict()
	users['default'] = Person()
	states['default'] = State()

	forced_swing = Value('i', 0)
	turnedOn = Value('i', 1)
	keep = Value('i', 0)

	for i in range(24):
		temp.append(i)

	app.run(host='0.0.0.0', port=8080)
