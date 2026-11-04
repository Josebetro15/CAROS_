package mx.scanot.service;

import mx.scanot.domain.Cliente;
import mx.scanot.domain.Encargado;
import mx.scanot.domain.MailMessage;
import mx.scanot.domain.Mecanico;
import mx.scanot.domain.Rol;
import mx.scanot.domain.ScanReading;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.VehicleUpdate;
import mx.scanot.domain.WorkOrder;
import mx.scanot.repo.ClienteRepo;
import mx.scanot.repo.EncargadoRepo;
import mx.scanot.repo.MecanicoRepo;
import mx.scanot.repo.UsuarioRepo;
import org.springframework.stereotype.Service;

/** Amarra cada accion a la tabla del rol, no a usuarios en general. */
@Service
public class Personas {
  private final UsuarioRepo usuarios;
  private final ClienteRepo clientes;
  private final MecanicoRepo mecanicos;
  private final EncargadoRepo encargados;

  public Personas(UsuarioRepo usuarios, ClienteRepo clientes, MecanicoRepo mecanicos, EncargadoRepo encargados) {
    this.usuarios = usuarios;
    this.clientes = clientes;
    this.mecanicos = mecanicos;
    this.encargados = encargados;
  }

  public Cliente clienteDe(Usuario user) {
    if (user == null || user.getRol() != Rol.CLIENTE) {
      throw new IllegalArgumentException("Solo un cliente puede vincular la unidad");
    }
    if (user instanceof Cliente cliente) return cliente;
    return clientes.getReferenceById(user.getId());
  }

  public void publica(VehicleUpdate update, Usuario user) {
    if (user.getRol() == Rol.MECANICO) update.setMecanico(mecanicos.getReferenceById(user.getId()));
    else if (user.getRol() == Rol.ENCARGADO) update.setEncargado(encargados.getReferenceById(user.getId()));
  }

  public void escanea(ScanReading reading, Usuario user) {
    if (user == null) return;
    if (user.getRol() == Rol.MECANICO) reading.setMecanico(mecanicos.getReferenceById(user.getId()));
    else if (user.getRol() == Rol.ENCARGADO) reading.setEncargado(encargados.getReferenceById(user.getId()));
  }

  public void vigila(WorkOrder order, Usuario user) {
    if (user == null) return;
    if (user.getRol() == Rol.ENCARGADO) {
      order.setEncargado(encargados.getReferenceById(user.getId()));
    } else if (user.getRol() == Rol.MECANICO) {
      order.setMecanico(mecanicos.getReferenceById(user.getId()));
      if (order.getTechnician() == null || order.getTechnician().isBlank()) {
        order.setTechnician(user.getDisplayName());
      }
    }
  }

  public void firma(MailMessage mail, Usuario user) {
    if (user == null) return;
    if (user.getRol() == Rol.MECANICO) mail.setMecanico(mecanicos.getReferenceById(user.getId()));
    else if (user.getRol() == Rol.ENCARGADO) mail.setEncargado(encargados.getReferenceById(user.getId()));
  }

  public void recibe(MailMessage mail) {
    if (mail.getToEmail() == null) return;
    usuarios.findByEmail(mail.getToEmail().trim().toLowerCase())
        .filter(u -> u.getRol() == Rol.CLIENTE)
        .ifPresent(u -> mail.setCliente(clientes.getReferenceById(u.getId())));
  }
}
