package mx.scanot.service;

import java.util.UUID;
import mx.scanot.domain.Cliente;
import mx.scanot.domain.Rol;
import mx.scanot.domain.Usuario;
import mx.scanot.repo.UsuarioRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAccounts {
  private final UsuarioRepo usuarios;
  private final PasswordEncoder encoder;

  public GoogleAccounts(UsuarioRepo usuarios, PasswordEncoder encoder) {
    this.usuarios = usuarios;
    this.encoder = encoder;
  }

  @Transactional
  public Usuario crearCliente(String email, String name) {
    String correo = email.trim().toLowerCase();
    if (usuarios.findByEmail(correo).isPresent()) {
      throw new IllegalArgumentException("Ese correo ya tiene cuenta. Entra con Google.");
    }
    Cliente creado = new Cliente();
    creado.setEmail(correo);
    creado.setDisplayName(name == null || name.isBlank() ? correo : name.trim());
    creado.setRol(Rol.CLIENTE);
    creado.setPassword(encoder.encode(UUID.randomUUID().toString()));
    return usuarios.save(creado);
  }
}
