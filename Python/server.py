from flask import *
from multiprocessing import Value, Lock, Manager
from ctypes import c_char_p

import logging
log = logging.getLogger('werkzeug')

manager = Manager()
app = Flask(__name__)
tasks = list()
temp = manager.list()
exp = manager.dict()
member_list = manager.list()
nxt_time = Value('i', 0)
init_state = Value('i', 0)
cur_stamp = Value('i', 0)
p_nxt_time = Value('i', 0)
mutex = Lock()
prime_user = Value(c_char_p, "default")


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
			exp_list = [x for x in exp[prime_user.value]]
			return jsonify({'status': 1, 'temp': temp_list, 'exp': exp_list, 'time': cur_stamp.value % 24, 'p_time': p_nxt_time.value})

		elif cmd == 'modify_exp_temp':
			person_id = task['person_id']
			exp_time = task['exp_time']
			exp_temp = task['exp_temp']
			if person_id in member_list:
				if mutex.acquire():
					exp[person_id][exp_time] = exp_temp
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
			return jsonify({'status': 1, 'member_list': exp.keys()})

		elif cmd == 'request_exp':
			person_id = task['person_id']
			if person_id in exp:
				return jsonify({'status': 1, 'exp': exp[person_id]})
			else:
				return jsonify({'status': -1, 'exp': exp['default']})

		elif cmd == 'request_prime_user':
			return jsonify({'status': 1, 'prime_user': prime_user.value})

		else:
			return jsonify({'status': -1})


def run(init_state_v, nxt_time_v, exp_dict, temp_arr, p_nxt_time_v, cur_stamp_v, prime_user_s):
	log.setLevel(logging.ERROR)
	# TODO: synchronize prime_user as a Value with type String (person_id)
	global exp, temp, nxt_time, init_state, p_nxt_time, cur_stamp, prime_user
	exp = exp_dict
	exp = exp_dict
	temp = temp_arr
	nxt_time = nxt_time_v
	init_state = init_state_v
	p_nxt_time = p_nxt_time_v
	cur_stamp = cur_stamp_v
	prime_user = prime_user_s
	app.run(host='0.0.0.0', port=8080)


if __name__ == '__main__':
	global exp, temp
	print("Running in DEMO mode.")
	init_state.value = 1
	exp['default'] = {}
	for i in range(24):
		temp.append(i)
		exp['default'].append(i)

	app.run(host='127.0.0.1', port=8080)
