package mx.scanot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** El encargado supervisa ordenes y tambien publica, chatea y avisa. */
@Entity
@Table(name = "encargados")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Encargado extends Usuario {
}
