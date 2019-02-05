import hardware
import server
from multiprocessing import Value, Manager, Process

if __name__ == '__main__':
    init_state = Value('i', 0)
    nxt_time_v = Value('i', 0)
    p_nxt_time_v = Value('i', 0)
    cur_time = Value('i', 0)
    manager = Manager()
    exp_arr = manager.list()
    temp_arr = manager.list()
    member_list = manager.list()
    record_flag = (raw_input("Take record?") == 'y')
    hard_proc = Process(target=hardware.run,
                        args=(init_state, nxt_time_v, exp_arr, temp_arr, record_flag, p_nxt_time_v, cur_time, member_list),
                        name="hardware_proc")
    serv_proc = Process(target=server.run,
                        args=(init_state, nxt_time_v, exp_arr, temp_arr, p_nxt_time_v, cur_time, member_list),
                        name="server_proc")
    hard_proc.start()
    serv_proc.start()
    serv_proc.join()
    hard_proc.join()
