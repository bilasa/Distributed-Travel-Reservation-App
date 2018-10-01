/* NOTE:
 * The following enumerations define the action types.
 * Every request that is sent from the client contains respective attributes for them,
 * along with associated payload. 
 */ 

package Server.Actions;

public enum ACTION_TYPE {
    FLIGHT_ACTION,
    CAR_ACTION,
    ROOM_ACTION,
    CUSTOMER_ACTION
}
