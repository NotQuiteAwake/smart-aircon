# JSON API Commands

## Introduction

The file lists all the API commands that are available through JSON.

## What it means

~~Strikethrough~~ commands are not ready-to-go yet.

*Italic* commands are implemented but not tested.

```JAVA
@return
// provides the list of variables that are returned by the server.
!not-yet-implemented
*ready-to-use
?optional
```

```JAVA
@param
// provdes the list of parameters to be sent along with the command.
```

## List of Commands

- check_init_state

    ```JAVA
    @return
    *status
    *init_state
    ```

- *get_stat*

    ```JAVA
    @return
    *status
    *temp
    *exp
    *exp_temp // current enabled set of exp
    *time
    *p_time // predicted time
    ```

- *set_exp_temp*

    ```JAVA
    @param
    *exp_time
    *exp_temp
    *person_id // the id of the person whose exp is to be modified
    *
    ```
    ```JAVA
    @return
    *status
    ```

- *set_exp_time*

    ```JAVA
    @param
    *exp_time
    ```

    ```JAVA
    @return
    *status
    ```

- *get_member_list*

    ```JAVA
    @return
    *status
    *member_list
    ```

- *get_exp_temp*

    ```JAVA
    @param
    *person_id
    ```

    ```JAVA
    @return
    *status
    *exp_temp
    ```

- *get_prime_user*

    ```JAVA
    @return
    *status
    *prime_user
    ```

- *set_user_presence*

    ```JAVA
    @param
    *person_id
    *presence
    ```

    ```JAVA
    @return
    *status
    ```

- *set_user_priority*

    ```JAVA
    @param
    *person_id
    *priority
    ```

    ```JAVA
    @return
    *status
    ```

- *set_user_state*

    ```JAVA
    @param
    *person_id
    *state_id
    ```

    ```JAVA
    @return
    *status
    ```

- *add_state*

    ```JAVA
    @param
    *state_id
    *temp_diff
    ```

    ```JAVA
    @return
    *status
    ```

- *remove_state*

    ```JAVA
    @param
    *state_id
    ```

    ```JAVA
    @return
    *status
    ```

- *set_state*

    ```JAVA
    @param
    *state_id
    *temp_diff
    ```

    ```JAVA
    *status
    ```

- *add_user*

    ```JAVA
    @param
    *person_id
    ?exp_temp // 'default' if void.
    ?priority // 'default' if void.
    ?state_id // 'default' if void.
    ```

    ```JAVA
    @return
    *status
    ```

- *remove_user*

    ```JAVA
    @param
    *person_id
    ```

    ```JAVA
    @return
    *status
    ```

- *get_user*

    ```JAVA
    @param
    *person_id
    ```

    ```JAVA
    @return
    *status
    ?person_id // void if status = -1
    ?state_id
    ?exp_temp
    ?priority
    ?presence
    ```

- *set_name*

    ```JAVA
    @param
    *person_id
    *name

    @return
    *status

- *get_state_list*

    ```JAVA
    @return
    *status
    *state_list
    ```

- *set_state_off*

    ```JAVA
    @return
    *status
    ```