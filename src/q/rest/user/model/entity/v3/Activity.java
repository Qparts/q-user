package q.rest.user.model.entity.v3;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="usr_activity")
public class Activity implements Serializable{
	@Id
	private int id;
	private String name;
	private String nameAr;
	private String type;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameAr() {
		return nameAr;
	}

	public void setNameAr(String nameAr) {
		this.nameAr = nameAr;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
