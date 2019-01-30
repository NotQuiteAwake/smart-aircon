import hardware
import server
from multiprocessing import Value, Manager, Process

if __name__ == '__main__':
#    global init_state, nxt_time_v, exp_arr, temp_arr
    init_state = Value('i', 0)
    nxt_time_v = Value('i', 0)
    p_nxt_time_v = Value('i', 0)
    cur_time = Value('i', 0)
    manager = Manager()
    exp_arr = manager.list()
    temp_arr = manager.list()
    record_flag = (raw_input("Take record?") == 'y')
    hardware_proc = Process(target=hardware.run, args=(init_state, nxt_time_v, exp_arr, temp_arr, record_flag, p_nxt_time_v, cur_time), name="hardware_proc")
    server_proc = Process(target=server.run, args=(init_state, nxt_time_v, exp_arr, temp_arr, p_nxt_time_v, cur_time), name="server_proc")
    hardware_proc.start()
    server_proc.start()
    server_proc.join()
    hardware_proc.join()
