# MoneyOS (Java + Spring Boot)

Aplicación web de **finanzas personales en pesos colombianos**, hecha en **Java 21** con
**Spring Boot**, con **registro de usuarios**, **inicio de sesión** y **Panel Administrativo con
roles** (ADMIN, OPERADOR y CONSULTA), siguiendo las ideas de la guía *“Sistema de Inicio de
Sesión (Login) y Panel Administrativo con Roles de Usuario en Java Swing”*, llevadas a la web.

**App en línea:** https://moneyos-wn2o.onrender.com

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/GGJUAN00/moneyOS)

## Qué tiene

- **Login** como en la guía: usuario y contraseña, casilla *Mostrar contraseña*, botones
  *Limpiar* e *Ingresar* (también con Enter), contador **“Intentos disponibles: 3”** y bloqueo
  al tercer intento fallido. El usuario no distingue mayúsculas (`admin` = `ADMIN`); la
  contraseña sí.
- **Registro de usuarios** (*Crear cuenta*): nombre, usuario, correo, celular y contraseña. La
  cuenta queda **pendiente** hasta que un administrador la apruebe y le asigne un rol.
- **Panel Administrativo** con barra superior (bienvenida, **insignia de rol de color** y
  *Cerrar sesión* con confirmación), **pestañas según el rol** y barra de estado.
- **Gestión de Usuarios** (solo ADMIN): crear usuarios con rol, tabla sin contraseñas, buscar,
  cambiar rol, aprobar cuentas pendientes, desbloquear y **eliminar el usuario seleccionado**
  (no deja eliminar la propia cuenta ni al único administrador).
- **Finanzas**: saldo, *Agregar dinero* y *Transacción* con tipo de gasto (Comida, Transporte,
  Compras, Facturas…), presupuesto mensual, tabla de movimientos con buscador, gráficas
  (por mes, por semana, saldo y categorías) y **resumen de cada mes** al terminar.
- Fecha y hora de **Colombia**, diseño **negro y gris**, animaciones suaves y **sonidos** al
  guardar o eliminar un movimiento (se pueden apagar en *Mi cuenta*).
- Contraseñas **cifradas con BCrypt**; datos guardados en **base de datos** (no se pierden al
  cerrar el programa, a diferencia de los arreglos de la guía).

## Roles y permisos

| Rol | Insignia | Inicio | Movimientos | Registrar movimiento | Gráficas | Gestión de Usuarios |
| --- | --- | --- | --- | --- | --- | --- |
| **ADMIN** | rojo | Sí | Sí | Sí | Sí | **Sí (exclusivo)** |
| **OPERADOR** | verde azulado | Sí | Sí | Sí | Sí | No |
| **CONSULTA** | azul | Sí | Sí (solo lectura) | No | Sí | No |

Todos tienen además la pestaña *Mi cuenta* (datos, cambiar contraseña y sonidos).
Los permisos se revisan en el servidor: aunque alguien escriba la dirección a mano, Java no lo
deja hacer lo que su rol no permite.

## Ejecutar en tu computador (IntelliJ IDEA)

Requisitos: **JDK 21**.

1. En IntelliJ IDEA: **File → Open…** y elige la carpeta del proyecto (la que tiene `pom.xml`).
   IntelliJ descarga las librerías solo.
2. Abre `src/main/java/com/moneyos/MoneyOsAplicacion.java` y pulsa **Run** (▶) junto a `main`.
3. Abre en el navegador **http://localhost:8080**

También desde la terminal: `./mvnw spring-boot:run` (o `mvn spring-boot:run`).

La primera vez se crean los usuarios de prueba de la guía (solo en tu computador):

| Usuario | Contraseña | Nombre | Rol |
| --- | --- | --- | --- |
| admin | 1234 | Carlos Mendoza | ADMIN |
| operador | 1234 | Laura Gómez | OPERADOR |
| consulta | 1234 | Andrés Pérez | CONSULTA |

Los datos se guardan en la carpeta `datos/` (base de datos H2). Para empezar de cero, cierra la
app y borra esa carpeta.

## Pruebas

```bash
mvn test
```

`src/test/java/com/moneyos/PlanDePruebasGuiaTest.java` automatiza el **plan de pruebas de la guía
(Tabla 8)** adaptado a la web: campos vacíos, mostrar contraseña, clave incorrecta, bloqueo a los 3
intentos, pestañas por rol (consulta, operador, admin), registrar un movimiento, no poder
eliminarse a sí mismo, crear a «maria» y entrar con ella, además del registro con aprobación.
GitHub ejecuta estas pruebas en cada cambio (`.github/workflows/pruebas.yml`).

## Publicar en internet (Render)

1. Pulsa el botón **Deploy to Render** de arriba (o en Render: **New → Blueprint** y elige este
   repositorio). Render lee `render.yaml` y crea la app y una base de datos PostgreSQL gratis.
2. Te pedirá dos valores:
   - `ADMIN_NOMBRE`: tu nombre completo.
   - `ADMIN_CLAVE`: la contraseña del usuario **admin** (mínimo 4 caracteres; usa una segura).
3. Espera a que termine (unos minutos) y abre el enlace `https://….onrender.com`.
4. Entra con **admin** y la clave que pusiste. En internet **no** existen los usuarios de prueba
   con clave 1234.

Ten en cuenta el plan gratis de Render:
- La app se “duerme” tras 15 minutos sin uso; la primera visita tarda cerca de un minuto en abrir.
- La base de datos PostgreSQL gratis **vence a los 30 días** (con 14 días más para pasarla a un
  plan pago antes de que se borre). Para no perder datos puedes usar una base gratis sin
  vencimiento, como [Neon](https://neon.tech): crea un proyecto, copia su *connection string*
  (`postgresql://…`) y ponla en Render en la variable `DATABASE_URL` del servicio `moneyos`.

## Cómo está organizado (comparado con la guía)

| Guía (Java Swing) | MoneyOS (Java web) |
| --- | --- |
| `DatosSistema.java` (arreglos paralelos, `cargarDatosIniciales`) | `config/DatosSistema.java` (usuarios iniciales) + base de datos con `modelo/Usuario.java` y `repositorio/*` |
| `validarLogin()` con `equalsIgnoreCase` / `equals` | `seguridad/DetallesUsuarioServicio.java` + Spring Security (contraseñas BCrypt) |
| `VentanaLogin.java` (JFrame, JPasswordField, JCheckBox, 3 intentos) | `web/LoginControlador.java` + `templates/login.html` + `seguridad/ManejadorLoginFallido.java` |
| `PanelAdministrativo.java` (JTabbedPane, insignia con `switch`, cerrar sesión) | `web/PanelControlador.java` (pestañas por rol) + `templates/panel.html` |
| `JTable` + `DefaultTableModel` | Tablas HTML en `templates/pestanas/*.html` |
| `JOptionPane` | Ventanas de aviso y confirmación (`web/Aviso.java`, `fragmentos/base.html`) |
| Roles `"ADMIN"`, `"OPERADOR"`, `"CONSULTA"` | `modelo/Rol.java` (con colores y permisos) |

```
src/main/java/com/moneyos/
├── MoneyOsAplicacion.java        ← main: arranca el servidor
├── config/      DatosSistema, SeguridadConfig, BaseDeDatosConfig
├── modelo/      Usuario, Rol, EstadoUsuario, Movimiento, TipoMovimiento, Categoria, Ajuste
├── repositorio/ consultas a la base de datos
├── seguridad/   login, intentos fallidos y bloqueo
├── servicio/    UsuarioServicio (usuarios) y FinanzasServicio (saldo, gráficas, resúmenes)
├── util/        Colombia (hora de Colombia y formato de pesos)
└── web/         controladores de las pantallas
src/main/resources/
├── templates/   login, registro, panel y sus pestañas (Thymeleaf)
└── static/      estilos (css) y detalles visuales (js)
```

## Versión anterior

La versión anterior de MoneyOS (React/TypeScript, con la app de escritorio) quedó guardada en la
rama **`version-react`**.
