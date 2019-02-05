from flask import *
from multiprocessing import Value, Lock, Manager
import sys

import logging
log = logging.getLogger('werkzeug')

manager = Manager()
app = Flask(__name__)
tasks = list()
temp = manager.list()
exp = manager.list()
member_list = manager.list()
nxt_time = Value('i', 0)
init_state = Value('i', 0)
cur_stamp = Value('i', 0)
p_nxt_time = Value('i', 0)
mutex = Lock()


@app.route('/')
def service_test():
	return jsonify({'description': 'smart-air-conditioner'})


@app.errorhandler(404)
def not_found(error):
	return make_response(jsonify({'error': 'Not Found'}), 404)


@app.route('/post_tasks', methods=['POST'])
def handle_task():
	if not request.json or not 'cmd' in request.json:
		abort(400)
	task = {}
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
			exp_list = [x for x in exp]
			return jsonify({'status': 1, 'temp': temp_list, 'exp': exp_list['default'], 'time': cur_stamp.value % 24, 'p_time': p_nxt_time.value})
		elif cmd == 'modify_exp_temp':
			if mutex.acquire():
				exp[task['exp_time']] = task['exp_temp']
			mutex.release()
			return jsonify({'status': 1})
		elif cmd == 'modify_exp_time':
			if mutex.acquire():
				nxt_time.value = task['exp_time']
			mutex.release()
			return jsonify({'status': 1})
		elif cmd == 'request_member_list':
			return jsonify({'member_list': member_list})
		elif cmd == 'request_exp':
			person_id = task['person_id']
			if person_id in exp:
				return jsonify({'exp': exp[person_id]})
			else:
				return jsonify({'exp': exp['default']})
		else:
			return jsonify({'status': -1})


def run(init_state_v, nxt_time_v, exp_arr, temp_arr, p_nxt_time_v, cur_stamp_v, member_list_arr):
	log.setLevel(logging.ERROR)
	global exp, temp, nxt_time, init_state, p_nxt_time, cur_stamp, member_list
	exp = exp_arr
	temp = temp_arr
	nxt_time = nxt_time_v
	init_state = init_state_v
	p_nxt_time = p_nxt_time_v
	cur_stamp = cur_stamp_v
	member_list = member_list_arr
	app.run(host='0.0.0.0', port=8080)


if __name__ == '__main__':
	print("Running in DEMO mode.")
	init_state.value = 1
	for i in range(24):
		temp.append(i)
		exp.append(i)

	app.run(host='127.0.0.1', port=8080)
