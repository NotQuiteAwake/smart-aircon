import hardware
import server
from multiprocessing import Value, Manager, Process
from ctypes import c_char_p
from person_class import Person
from state_class import State

if __name__ == '__main__':
	init_state = Value('i', 0)
	nxt_time_v = Value('i', 0)
	p_nxt_time_v = Value('i', 0)
	cur_time = Value('i', 0)
	keep = Value('i', 0)
	turnedOn = Value('i', 0)
	turnedOff = Value('i', 1)
	forced_swing = Value('i', 0)

	manager = Manager()

	users = manager.dict()
	users['default'] = Person()

	load_models = 1 if raw_input("Load models?") == 'y' else 0
	weighed_mean = raw_input("Use weighed mean?") == 'y'

	states = manager.dict()
	states['default'] = State()

	temp_arr = manager.list()
	prime_user = manager.dict({'value':'default'})
	record_flag = (raw_input("Take record?") == 'y')

	hard_proc = Process(target=hardware.run,
						args=(init_state, nxt_time_v, users, temp_arr, record_flag, p_nxt_time_v,
							  cur_time, prime_user, turnedOn, keep, forced_swing, load_models,
							  turnedOff, weighed_mean),
						name="hardware_proc")
	serv_proc = Process(target=server.run,
						args=(init_state, nxt_time_v, users, temp_arr, p_nxt_time_v,
							  cur_time, prime_user, states, turnedOn, keep, forced_swing,
							  turnedOff, weighed_mean),
						name="server_proc")

	hard_proc.start()
	serv_proc.start()
	serv_proc.join()
	hard_proc.join()
