package fiab.mes.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

public class FakeAuthenticator {
	
	private List<User> users;
	private static Key jwtKey;
	
	public FakeAuthenticator() {
		jwtKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
		users = new ArrayList<User>();
		users.add(new User("1", "admin", "admin", "Admin", "User", User.Role.Admin));
		users.add(new User("2", "user", "user", "Normal", "User", User.Role.User));
		
	}
	
	public Optional<User> authenticate(String username, String password) {
		return users.stream()
			.filter(u -> u.username.equals(username))
			.filter(u -> u.password.equals(password))
			.findAny();
	}
	
	public boolean isLoggedIn(String token) {
		try {
			token = token.split(" ")[1];
			String t = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody().getSubject();
			return users.stream()
					.anyMatch(u -> t.equals(u.id));
		} catch (MalformedJwtException | SignatureException | ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
	
	public static class User {
		private String id;
		private String username;
		private String password;
		private String firstName;
		private String lastName;
		private Role role;
		private String token;
		
		public User(String id, String username, String password, String firstName, String lastName, Role role) {
			super();
			this.id = id;
			this.username = username;
			this.password = password;
			this.firstName = firstName;
			this.lastName = lastName;
			this.role = role;
			this.token = Jwts.builder().setSubject(id).signWith(jwtKey).compact();
		}

		public String getId() {
			return id;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public Role getRole() {
			return role;
		}
		
		public String getToken() {
			return token;
		}

		public static enum Role {User, Admin}
	}
	
	public static class Credentials {
		private String username;
		private String password;
		public String getUsername() {
			return username;
		}
		public String getPassword() {
			return password;
		}
		
		
	}

}
