package fiab.mes.transport.actor.transportsystem;

import java.util.List;

public interface TransportRoutingInterface {

	// for now we use machine ids
	List<Position> calculateRoute(Position fromMachine, Position toMachine) throws RoutingException;

	public static Position UNKNOWN_POSITION = new Position("0.0");

	public static class RoutingException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Error error;
		
		public RoutingException(String message, Error error) {
			super(message);
			this.error = error;
		}
		
		public static enum Error {
			UNKNOWN_POSITION,
			NO_ROUTE
		}
	}
	
	public static class Position {
		protected String pos = null;
		
		public Position(String pos) {
			this.pos = pos;
		}
		
		public String getPos() {
			return pos;
		}
		
		@Override
		public String toString() {
			return "Position [" + pos + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((pos == null) ? 0 : pos.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Position other = (Position) obj;
			if (pos == null) {
				if (other.pos != null)
					return false;
			} else if (!pos.equals(other.pos))
				return false;
			return true;
		}
		
	}
	
}