JSON API Commands
- check_init_state
    
    ```JAVA
    @return 
    *status
    *init_state
    ```

- ~~data_req~~

    ```JAVA
    @return
    *status
    *temp_list
    *exp // current enabled set of exp
    *time
    *p_time // predicted time
    ```

- ~~modify_exp_temp~~

    ```JAVA
    @param
    *exp_time
    *exp_temp
    !person_id // the id of the person whose exp is to be modified
    *
    ```
    ```JAVA
    @return
    *status
    ```

- ~~modify_exp_time~~

    ```JAVA
    @param
    *exp_time
    !person_id
    ```

    ```JAVA
    @return
    *status
    ```

- ~~request_member_list~~

    ```JAVA
    @return
    *member_list
    !status    
    ```

- ~~request_exp~~

    ```JAVA
    @param
    *person_id
    ```

    ```JAVA
    @return
    !status
    *exp
    ```