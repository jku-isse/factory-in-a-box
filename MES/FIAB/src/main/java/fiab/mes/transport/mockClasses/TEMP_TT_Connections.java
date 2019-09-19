package fiab.mes.transport.mockClasses;

public class TEMP_TT_Connections {
	private String south;
	private String north;
	private String west;
	private String east;
	private String turntable;
	
	public TEMP_TT_Connections(String turntable, String north, String east, String south, String west) {
		this.turntable = turntable;
		this.east = east;
		this.north = north;
		this.south = south;
		this.west = west;
	}

	public String getSouth() {
		return south;
	}

	public String getNorth() {
		return north;
	}

	public String getWest() {
		return west;
	}

	public String getEast() {
		return east;
	}

	public String getTurntable() {
		return turntable;
	}
	
	

}
