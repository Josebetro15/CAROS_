package mx.scanot.service;

import mx.scanot.domain.SolicitudCuenta;
import mx.scanot.repo.SolicitudRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudIntentos {
  private final SolicitudRepo solicitudes;

  public SolicitudIntentos(SolicitudRepo solicitudes) {
    this.solicitudes = solicitudes;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int sumar(Long id) {
    SolicitudCuenta s = solicitudes.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No hay un registro pendiente con ese correo."));
    int n = s.getIntentos() + 1;
    s.setIntentos(n);
    if (n >= 5) solicitudes.delete(s);
    else solicitudes.save(s);
    return n;
  }
}
