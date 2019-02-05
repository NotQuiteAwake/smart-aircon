import hardware
import server
from multiprocessing import Value, Manager, Process
from ctypes import c_char_p

if __name__ == '__main__':
    init_state = Value('i', 0)
    nxt_time_v = Value('i', 0)
    p_nxt_time_v = Value('i', 0)
    cur_time = Value('i', 0)
    manager = Manager()
    exp_arr = manager.dict()
    temp_arr = manager.list()
    prime_user = Value(c_char_p, "default")
    record_flag = (raw_input("Take record?") == 'y')
    hard_proc = Process(target=hardware.run,
                        args=(init_state, nxt_time_v, exp_arr, temp_arr, record_flag, p_nxt_time_v, cur_time, prime_user),
                        name="hardware_proc")
    serv_proc = Process(target=server.run,
                        args=(init_state, nxt_time_v, exp_arr, temp_arr, p_nxt_time_v, cur_time, prime_user),
                        name="server_proc")
    hard_proc.start()
    serv_proc.start()
    serv_proc.join()
    hard_proc.join()
