/* NOTE:
 * The following enumerations define the action sub-types.
 * Every request that is sent from the client contains respective attributes for them,
 * along with associated payload. 
 */ 

package Server.Actions;

public enum ACTION_SUBTYPE {
    ADD_FLIGHT,
    ADD_CAR_LOCATION,
    ADD_ROOM_LOCATION,
    ADD_CUSTOMER,
    QUERY_FLIGHT,
    QUERY_CAR_LOCATION,
    QUERY_ROOM_LOCATION,
    QUERY_CUSTOMER,
    QUERY_FLIGHT_PRICE,
    QUERY_CAR_PRICE,
    QUERY_ROOM_PRICE,
    DELETE_FLIGHT,
    DELETE_CAR_LOCATION,
    DELETE_ROOM_LOCATION,
    DELETE_CUSTOMER,
    RESERVE_FLIGHT,
    RESERVE_CAR,    
    RESERVE_ROOM,
    RESERVE_BUNDLE
}
