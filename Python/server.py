from flask import *
from multiprocessing import Array, Value, Lock
import sys

import logging
log = logging.getLogger('werkzeug')
log.setLevel(logging.ERROR)

app = Flask(__name__)
tasks = list()
temp = Array('d', [])
exp = Array('d', [i for i in range(24)])
nxt_time = Value('i', 0)
init_state = Value('i', 0)
cur_stamp = Value('i', 0)
p_nxt_time = Value('i', 0)

mutex = Lock()


@app.route('/')
def service_test():
	return "<h1>This is a local server for server-app connectivity.<h1>\n"


@app.errorhandler(404)
def not_found(error):
	return make_response(jsonify({'error': 'Not Found'}), 404)


@app.route('/post_tasks', methods=['POST'])
def handle_task():
	if not request.json or not 'cmd' in request.json:
		abort(400)	
	cmd = request.json['cmd']
	if cmd == 'data_req' or cmd == 'check_init_state':
		task = {'cmd': cmd}
	elif cmd == 'modify_exp':
		task = {
			'cmd': request.json['cmd'],
			'exp_time': request.json['exp_time'],
			'exp_temp': request.json['exp_temp']
		}
	else:
		return jsonify({'status': -1})
	tasks.append(task)
	return jsonify({"status": 1})


@app.route('/data_fetch', methods=['GET'])
def comp_task():
	if len(tasks) == 0:
		return jsonify({'status': -1, 'init_state': init_state.value})
	else:
		task = tasks[-1]
		tasks.pop(-1)
		if task['cmd'] == 'check_init_state':
			return jsonify({'status': 1, 'init_state': init_state.value})
		elif task['cmd'] == 'data_req':
			temp_list = [x for x in temp]
			exp_list = [x for x in exp]
			return jsonify({'status': 1, 'temp': temp_list, 'exp': exp_list, 'time': cur_stamp.value % 24, 'p_time': p_nxt_time.value})
		elif task['cmd'] == 'modify_exp':
			if mutex.acquire():
				nxt_time.value = task['exp_time']
				exp[nxt_time.value] = task['exp_temp']
			mutex.release()
			return jsonify({'status': 1})
		else:
			return jsonify({'status': -1})


def run(init_state_v, nxt_time_v, exp_arr, temp_arr, p_nxt_time_v, cur_stamp_v):
	global exp, temp, nxt_time, init_state, p_nxt_time, cur_stamp
	exp = exp_arr
	temp = temp_arr
	nxt_time = nxt_time_v
	init_state = init_state_v
	p_nxt_time = p_nxt_time_v
	cur_stamp = cur_stamp_v
	app.run(host='0.0.0.0', port=8080)


if __name__ == '__main__':
	app.run(host='0.0.0.0', port=8080)
