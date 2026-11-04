package mx.scanot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** El mecanico escanea, publica avances, usa el chat y manda avisos. */
@Entity
@Table(name = "mecanicos")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Mecanico extends Usuario {
}
