package ioc.entity;

public class Role {
	
	private String id;
	
	private String name;
	
	private User user;

	public Role(){
		System.out.println("ÊµÀý»¯Role Bean");
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	

}
