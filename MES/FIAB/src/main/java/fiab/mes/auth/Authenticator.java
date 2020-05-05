package fiab.mes.auth;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.security.Key;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

public class Authenticator {
	
	private List<User> users;
	private static Key jwtKey;
	private static Gson gson;
	private static JsonReader reader;
	private static final Type USER_TYPE = new TypeToken<List<User>>() { }.getType();


	boolean authOff = false;
	
	public Authenticator(boolean authOff) {
		this();
		this.authOff = authOff;
	}
	
	
	public Authenticator() {
		jwtKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
		gson = new Gson();
		try {
			reader = new JsonReader(new FileReader("cred.json"));
			users = gson.fromJson(reader, USER_TYPE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		users.stream().forEach(u -> u.setToken());
	}
	
	public Optional<User> authenticate(String username, String password) {
		return users.stream()
			.filter(u -> u.username.equals(username))
			.filter(u -> u.password.equals(password))
			.findAny();
	}
	
	public boolean isLoggedIn(String token) {
		if (authOff)
			return true;
		else
		try {
			token = token.split(" ")[1];
			String t = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody().getSubject();
			return users.stream()
					.anyMatch(u -> t.equals(u.id));
		} catch (MalformedJwtException | SignatureException | ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
	
	public static class PublicUser {
		private String firstName;
		private Role role;
		private String token;
		
		public static enum Role {User, Admin}

		private PublicUser(String firstName, Role role) {
			this.firstName = firstName;
			this.role = role;
		}
		
		private PublicUser(String firstName, Role role, String token) {
			this(firstName, role);
			this.token = token;
		}
		
		public String getFirstName() {
			return firstName;
		}
		
		public Role getRole() {
			return role;
		}
		
		public String getToken() {
			return token;
		}
	}
	
	public static class User extends PublicUser {
		private String id;
		private String username;
		private String password;
		private String lastName;
		
		public User(String id, String username, String password, String firstName, String lastName, Role role) {
			super(firstName, role);
			this.id = id;
			this.username = username;
			this.password = password;
			this.lastName = lastName;
		}

		public PublicUser createPublicCopy() {
			return new PublicUser(this.getFirstName(), this.getRole(), this.getToken());
		}
		
		public void setToken() {
			super.token = Jwts.builder().setSubject(id).signWith(jwtKey).compact();
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

		public String getLastName() {
			return lastName;
		}

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
