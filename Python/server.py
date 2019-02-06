from flask import *
from multiprocessing import Value, Lock, Manager
from ctypes import c_char_p
from person_class import Person
from state_class import State

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
state = manager.dict()

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
	for key, value in request.json:
		task[key] = value
	tasks.append(task)
	return jsonify({"status": 1})


'''
	if cmd in {'data_req', 'check_init_state', 'request_member_list'}:
		task = {'cmd': cmd}
	elif cmd == 'modify_exp_temp':
		task = {
			'cmd': cmd,
			'exp_time': request.json['exp_time'],
			'exp_temp': request.json['exp_temp'],
			'person_id': request.json['person_id']
		}
	elif cmd == 'modify_exp_time':
		task = {
			'cmd': cmd,
			'exp_time': request.json['exp_time']
		}
		# TODO request_exp
	elif cmd == 'request_exp':
		task = {
			'cmd': cmd,
			'person_id': request.json['person_id']
		}
		# TODO add_person
	elif cmd == 'add_person':
		task = {
			'cmd': cmd,
			'person_id': request.json['person_id'],
			'exp_time': request.json['exp_time']
		}
	else:
		return jsonify({'status': -1})
'''


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
		if cmd == 'check_init_state':
			return jsonify({'status': 1, 'init_state': init_state.value})

		elif cmd == 'data_req':
			temp_list = [x for x in temp]
			exp_list = users[prime_user.value].get_exp_temp()
			return jsonify({'status': 1, 'temp': temp_list, 'exp_temp': exp_list, 'time': cur_stamp.value % 24, 'p_time': p_nxt_time.value})

		elif cmd == 'modify_exp_temp':
			person_id = task['person_id']
			exp_time = task['exp_time']
			exp_temp = task['exp_temp']
			if person_id in member_list:
				if mutex.acquire():
					users[person_id].set_exp_temp(exp_time, exp_temp)
				mutex.release()
				return jsonify({'status': 1})
			else:
				return jsonify({'status': -1})

		elif cmd == 'modify_exp_time':
			if mutex.acquire():
				nxt_time.value = task['exp_time']
			mutex.release()
			return jsonify({'status': 1})

		elif cmd == 'request_member_list':
			return jsonify({'status': 1, 'member_list': users.keys()})

		elif cmd == 'request_exp_temp':
			person_id = task['person_id']
			if person_id in users:
				return jsonify({'status': 1, 'exp_temp': users[person_id].get_exp_temp()})
			else:
				return jsonify({'status': -1, 'exp_temp': users['default'].get_exp_temp()})

		elif cmd == 'request_prime_user':
			return jsonify({'status': 1, 'prime_user': prime_user.value})

		elif cmd == 'set_user_presence':
			person_id = task['person_id']
			presence = task['presence']
			users[person_id].set_presence((presence > 0))
			return jsonify({'status': 1})

		elif cmd == 'set_user_priority':
			person_id = task['person_id']
			priority = task['priority']
			users[person_id].set_user_priority(priority)
			return jsonify({'status': 1})

		# TODO: Implement findStateById to get the real state detail with state ID
		elif cmd == 'set_user_state':
			person_id = task['person_id']
			state_id = task['state_id']
			users[person_id].set_user_state(state_id)
			return jsonify({'status': 1})

		elif cmd == 'add_state' or cmd == 'set_state':
			state_id = task['state_id']
			temp_diff = task['temp_diff']
			state[state_id] = State(state_id, temp_diff)
			return jsonify({'status': 1})

		elif cmd == 'remove_state':
			del state[task['state_id']]
			return jsonify({'status': 1})

		elif cmd == 'add_user':
			person_id = task['person_id']
			priority = task['priority']
			exp_temp = task['exp_temp']
			state_id = task['state_id']
			# TODO: add findStateById
			users[person_id] = Person(person_id, exp_temp, priority, state_id)
			return jsonify({'status': 1})

		elif cmd == 'remove_user':
			person_id = task['person_id']
			del users[person_id]
			return jsonify({'status': 1})

		elif cmd == 'get_user':
			person_id = task['person_id']
			if person_id in users.keys():
				user = users[person_id]
				exp_temp = user.get_exp_temp()
				# TODO: check get_presence value correctness
				is_present = 1 if user.get_presence() else 0
				state_id = user.get_state().get_state_id()
				priority = user.get_priority()

				return jsonify({'status': 1,
								"person_id": person_id,
								"exp_temp": exp_temp,
								"state_id": state_id,
								"presence": is_present,
								"priority": priority})

			else:
				return jsonify({'status': -1})

		else:
			return jsonify({'status': -1})

# TODO: add mutex lock
# TODO: add default state
# TODO: add state_dict into the param as a manager.dict()
# TODO: add precautions for state nonexistent


def run(init_state_v, nxt_time_v, exp_dict, temp_arr, p_nxt_time_v, cur_stamp_v, prime_user_s, state_dict):
	log.setLevel(logging.ERROR)
	# TODO: synchronize prime_user as a Value with type String (person_id)
	# TODO: write a custom Manager class
	global users, temp, nxt_time, init_state, p_nxt_time, cur_stamp, prime_user, state
	users = exp_dict
	temp = temp_arr
	nxt_time = nxt_time_v
	init_state = init_state_v
	p_nxt_time = p_nxt_time_v
	cur_stamp = cur_stamp_v
	prime_user = prime_user_s
	state = state_dict
	app.run(host='0.0.0.0', port=8080)


if __name__ == '__main__':
	global users, temp
	print("Running in DEMO mode.")
	init_state.value = 1
	users['default'] = Person()
	for i in range(24):
		temp.append(i)
		# TODO: This line won't work.
		users['default'].get_exp_temp().append(i)

	app.run(host='127.0.0.1', port=8080)
