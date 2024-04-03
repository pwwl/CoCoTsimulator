package simulation;

/**
 the position that the BLE emitting device is in, such as in a ShirtPocket or a purse.
 This is needed to more accurately simulate RSSI transmission between devices so our simulation works a little more
 close to reality.
 */

public enum Position {


	//names must be in order for filename builder to work
	FRONTPANTSPOCKET(0), 
	INHAND(1), 
	SHIRTPOCKET(2);
	//REARPANTSPOCKET(3);
	//INPURSE()
	//the commented ones don't have enough data, for example there's no data between purse and hand at all

	//constructor just to give the position the ID
	public int ID;
	private Position(int ID) {
		this.ID = ID;
	}
}
