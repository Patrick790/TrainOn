package licenta.model;
import jakarta.persistence.*;

import java.io.Serializable;

@MappedSuperclass
public class BruteEntity<ID extends Serializable> implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected ID id;

    public BruteEntity() {
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BruteEntity)) return false;
        BruteEntity<?> bruteEntity = (BruteEntity<?>) o;
        return getId().equals(bruteEntity.getId());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                '}';
    }
}
