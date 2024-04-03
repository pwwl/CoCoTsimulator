/**
 ** Copyright 2024 Trevor Kann, Lujo Bauer, Robert K. Cunningham
 ** 
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 **/
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
