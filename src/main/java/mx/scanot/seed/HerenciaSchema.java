package mx.scanot.seed;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Deja la herencia en 3FN: lo comun vive en usuarios y cada subtipo
 * solo tiene su llave. Las relaciones van a la tabla del rol.
 * Si dos roles pueden hacer lo mismo, un arco exclusivo impide las dos llaves a la vez.
 */
@Component
public class HerenciaSchema {
  private final JdbcTemplate jdbc;

  public HerenciaSchema(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void aplicar() {
    copia("INSERT INTO clientes (usuario_id) SELECT id FROM usuarios u WHERE rol = 'CLIENTE' AND NOT EXISTS (SELECT 1 FROM clientes c WHERE c.usuario_id = u.id)");
    copia("INSERT INTO mecanicos (usuario_id) SELECT id FROM usuarios u WHERE rol = 'MECANICO' AND NOT EXISTS (SELECT 1 FROM mecanicos m WHERE m.usuario_id = u.id)");
    copia("INSERT INTO encargados (usuario_id) SELECT id FROM usuarios u WHERE rol = 'ENCARGADO' AND NOT EXISTS (SELECT 1 FROM encargados e WHERE e.usuario_id = u.id)");

    copia("DROP TABLE IF EXISTS chats CASCADE");
    copia("DROP TABLE IF EXISTS chat_messages CASCADE");
    copia("DROP TABLE IF EXISTS recuperaciones CASCADE");
    copia("DROP TABLE IF EXISTS password_resets CASCADE");
    copia("ALTER TABLE usuarios DROP COLUMN IF EXISTS origen");
    copia("ALTER TABLE usuarios DROP COLUMN IF EXISTS provider");

    copia("UPDATE correos m SET cliente_id = c.usuario_id FROM clientes c JOIN usuarios u ON u.id = c.usuario_id WHERE m.cliente_id IS NULL AND lower(m.correo_destino) = lower(u.correo)");
    copia("UPDATE ordenes SET encargado_id = (SELECT usuario_id FROM encargados ORDER BY usuario_id LIMIT 1) WHERE encargado_id IS NULL AND EXISTS (SELECT 1 FROM encargados)");

    arco("avances_autor", "avances", "num_nonnulls(mecanico_id, encargado_id) <= 1");
    arco("lecturas_autor", "lecturas", "num_nonnulls(mecanico_id, encargado_id) <= 1");
    arco("correos_remitente", "correos", "num_nonnulls(mecanico_id, encargado_id) <= 1");
    arco("usuario_rol", "usuarios", "rol IN ('CLIENTE', 'MECANICO', 'ENCARGADO')");
  }

  private void arco(String name, String table, String check) {
    copia("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + name);
    copia("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " CHECK (" + check + ")");
  }

  private void copia(String sql) {
    try {
      jdbc.execute(sql);
    } catch (RuntimeException ignored) {
      // La columna vieja ya no existe o la tabla todavia no tiene ese dato.
    }
  }
}
