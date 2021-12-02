package common;

/**
 * Перечисление возможных команд в запросе
 */
public enum MessageCommands {

    CREATE_DIR,
    SEND_FILE,
    LOAD_FILE,
    DELETE_FILE,

    SEND_ALL,
    LOAD_ALL,
    DELETE_ALL,

    DIR_OK,
    SEND_OK,
    LOAD_OK,
    DEL_OK,

    SEND_ALL_OK,
    LOAD_ALL_OK,
    DEL_ALL_OK

}
