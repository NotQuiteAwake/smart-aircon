# JSON API Commands
### Introduction
The file lists all the API commands that are available through JSON. 

### What does it mean?
~~Strikethrough~~ commands are not ready-to-go yet.

*Italic* commands are implemented but not tested.

```JAVA
@return
// provides the list of variables that are returned by the server.
!not-taken-care-of
*ready-to-use
```

```JAVA
@param
// provdes the list of parameters to be sent along with the command. 
```

### List of Commands
- check_init_state
    
    ```JAVA
    @return 
    *status
    *init_state
    ```

- *data_req*

    ```JAVA
    @return
    *status
    *temp
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

- *modify_exp_time*

    ```JAVA
    @param
    *exp_time
    ```

    ```JAVA
    @return
    *status
    ```

- *request_member_list*

    ```JAVA
    @return
    *status    
    *member_list
    ```

- *request_exp*

    ```JAVA
    @param
    *person_id
    ```

    ```JAVA
    @return
    *status
    *exp
    ```

- *request_prime_user*

    ```JAVA
    @return
    *status
    *prime_user
    ```

- ~~set_user_presence~~

    ```JAVA
    @param
    !person_id
    !presense
    ```

    ```JAVA
    @return
    !status
    ```

- ~~set_user_priority~~
    
    ```JAVA
    @param
    !person_id
    !priority
    ```

    ```JAVA
    @return
    !status
    ```

- ~~set_user_state~~
    
    ```JAVA
    @param
    !person_id
    !state_id
    ```
    
    ```JAVA
    @return
    !status
    ```

- ~~add_state~~

    ```JAVA
    @param
    !state_id
    !temp_diff
    ```

    ```JAVA
    @return
    !status
    ```

- ~~remove_state~~

    ```JAVA
    @param
    !state_id
    ```

    ```JAVA
    @return
    !status
    ```

- ~~set_state~~

    ```JAVA
    @param
    !state_id
    !temp_diff
    ```

    ```JAVA
    !status
    ```