# ScanOT — Spring Boot (JDK 21 + Maven)

Taller de diagnostico inteligente. El scanner da de alta o actualiza vehiculos por VIN.

## Requisitos
- JDK 21
- Maven 3.9 (o usa `mvnw` incluido)
- PostgreSQL 14+ y pgAdmin 4

## 1. Base de datos
En pgAdmin:
1. Create database → nombre `scanot`
2. Query Tool → abre `scanot-postgres.sql` (esta en esta carpeta) → F5

Si la base esta vacia, al arrancar Spring se crean tablas y datos demo solos (`ddl-auto=update` + seeder).

## 2. Configura PostgreSQL
Edita `src/main/resources/application.properties`:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/scanot
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD
```

## 3. Abrir en el IDE
- IntelliJ IDEA: File → Open → carpeta `scanot-spring` (abre el `pom.xml`)
- VS Code: File → Open Folder + Extension Pack for Java
- Visual Studio: no es un .sln de C#. Usa IntelliJ o VS Code.

## 4. Arrancar

```bash
./mvnw spring-boot:run
```

Windows:

```bat
mvnw.cmd spring-boot:run
```

Abre http://localhost:8080

## Cuentas
| Perfil | Correo | Contrasena |
|---|---|---|
| Cliente | cliente@scanot.mx | scanot123 |
| Mecanico | mecanico@scanot.mx | scanot123 |
| Encargado | encargado@scanot.mx | scanot123 |

Cliente demo: placa `NVX-18-42` VIN `3N1CN7AP8ML800011`

## Que incluye
- Bienvenida + login
- Scanner: subir JSON/CSV/TXT o escanear foto/QR (ZXing)
- Alta/update de vehiculos por VIN
- Mecanico: status + fotos/video
- Encargado: flujo kanban de OT
## APIs REST
Base: http://localhost:8080/api

Auth: HTTP Basic. Usuario: mecanico@scanot.mx  Contrasena: scanot123

Catalogo (sin login): GET http://localhost:8080/api

Ejemplos:
- GET /api/me
- GET /api/vehiculos
- GET /api/diagnosticos
- GET /api/partes
- GET /api/ordenes
- GET /api/flujo
- POST /api/scanner   JSON: {"vin":"3N1CN7AP8ML800011","plate":"NVX-18-42","brand":"Nissan","model":"Versa","year":2021,"km":65000,"dtcs":["P0171"]}
- POST /api/vehiculos/1/avance  JSON: {"kind":"piezas_pedidas","message":"Se encargaron piezas"}

