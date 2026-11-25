package com.moneyos;

import com.moneyos.modelo.Movimiento;
import com.moneyos.repositorio.MovimientoRepositorio;
import com.moneyos.repositorio.UsuarioRepositorio;
import com.moneyos.seguridad.ManejadorLoginExitoso;
import com.moneyos.seguridad.ManejadorLoginFallido;
import com.moneyos.web.Aviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plan de Pruebas de Funcionamiento del Login y Panel Administrativo (Tabla 8 de la guía),
 * adaptado a la versión web. Se ejecuta con:  mvn test
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PlanDePruebasGuiaTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired MovimientoRepositorio movimientos;

    // ---------- ayudas ----------

    private MockHttpSession ingresar(MockHttpSession sesion, String usuario, String clave) throws Exception {
        mvc.perform(post("/login").session(sesion).with(csrf()).param("username", usuario).param("password", clave));
        return sesion;
    }

    private Aviso avisoLogin(MockHttpSession sesion) {
        return (Aviso) sesion.getAttribute(ManejadorLoginFallido.AVISO);
    }

    private String panel(MockHttpSession sesion, String pestana) throws Exception {
        MvcResult r = mvc.perform(get("/panel").session(sesion).param("pestana", pestana)).andReturn();
        return r.getResponse().getContentAsString();
    }

    // ---------- pruebas ----------

    @Test
    @DisplayName("1. Usuario y contraseña vacíos → «Debe escribir su usuario y su contraseña.»")
    void camposVacios() throws Exception {
        MockHttpSession s = ingresar(new MockHttpSession(), "", "");
        assertThat(avisoLogin(s).mensaje()).isEqualTo("Debe escribir su usuario y su contraseña.");
        assertThat(avisoLogin(s).tipo()).isEqualTo("ADVERTENCIA");
    }

    @Test
    @DisplayName("2. La pantalla de login tiene «Mostrar contraseña» e intentos disponibles")
    void mostrarContrasena() throws Exception {
        String html = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Mostrar contraseña").contains("Intentos disponibles: 3").contains("Limpiar").contains("Ingresar");
    }

    @Test
    @DisplayName("3. admin / 9999 → error y quedan 2 intentos")
    void claveIncorrecta() throws Exception {
        MockHttpSession s = ingresar(new MockHttpSession(), "admin", "9999");
        assertThat(avisoLogin(s).mensaje()).contains("Usuario o contraseña incorrectos").contains("Le quedan 2 intento(s)");
        String html = mvc.perform(get("/login").session(s)).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Intentos disponibles: 2");
    }

    @Test
    @DisplayName("4. Tres fallos seguidos → «Acceso bloqueado» y el formulario queda deshabilitado")
    void tresFallos() throws Exception {
        MockHttpSession s = new MockHttpSession();
        ingresar(s, "admin", "1");
        ingresar(s, "admin", "2");
        ingresar(s, "admin", "3");
        assertThat(avisoLogin(s).titulo()).isEqualTo("Acceso bloqueado");
        // Aunque ahora escriba la clave correcta, esta ventana sigue bloqueada
        mvc.perform(post("/login").session(s).with(csrf()).param("username", "admin").param("password", "1234"))
            .andExpect(redirectedUrl("/login"));
        String html = mvc.perform(get("/login").session(s)).andReturn().getResponse().getContentAsString();
        assertThat(html).containsPattern("<fieldset[^>]*disabled").contains("Bloqueado");
    }

    @Test
    @DisplayName("5. consulta / 1234 → insignia CONSULTA y solo pestañas de lectura")
    void rolConsulta() throws Exception {
        MockHttpSession s = ingresar(new MockHttpSession(), "consulta", "1234");
        Aviso bienvenida = (Aviso) s.getAttribute(ManejadorLoginExitoso.AVISO_PANEL);
        assertThat(bienvenida.mensaje()).contains("Andrés Pérez").contains("Rol asignado: CONSULTA");
        String html = panel(s, "inicio");
        assertThat(html).contains("ROL: CONSULTA").contains("Movimientos")
            .doesNotContain("Registrar movimiento").doesNotContain("Gestión de Usuarios");
    }

    @Test
    @DisplayName("6. operador / 1234 → puede registrar pero NO ve Gestión de Usuarios")
    void rolOperador() throws Exception {
        MockHttpSession s = ingresar(new MockHttpSession(), "operador", "1234");
        String html = panel(s, "inicio");
        assertThat(html).contains("ROL: OPERADOR").contains("Registrar movimiento").doesNotContain("Gestión de Usuarios");
        // Ni siquiera escribiendo la dirección a mano puede crear usuarios
        mvc.perform(post("/panel/usuarios").session(s).with(csrf())
                .param("usuario", "intruso").param("clave", "1234").param("nombreCompleto", "X Y").param("rol", "ADMIN"))
            .andExpect(redirectedUrl("/panel?denegado"));
        assertThat(usuarios.existsByUsuarioIgnoreCase("intruso")).isFalse();
    }

    @Test
    @DisplayName("7. Como operador, registrar un gasto: aparece en Movimientos y cambia el saldo")
    void operadorRegistra() throws Exception {
        MockHttpSession s = ingresar(new MockHttpSession(), "operador", "1234");
        mvc.perform(post("/panel/movimientos").session(s).with(csrf())
            .param("tipo", "INGRESO").param("monto", "850.000").param("categoria", "SALDO_INICIAL"));
        mvc.perform(post("/panel/movimientos").session(s).with(csrf())
            .param("tipo", "GASTO").param("monto", "25000").param("categoria", "COMIDA").param("descripcion", "Cable HDMI"));
        List<Movimiento> lista = movimientos.findAllRecientes();
        assertThat(lista).hasSize(2);
        assertThat(panel(s, "movimientos")).contains("Cable HDMI");
        assertThat(panel(s, "inicio")).contains("$ 825.000");

        // CONSULTA no puede registrar
        MockHttpSession c = ingresar(new MockHttpSession(), "consulta", "1234");
        mvc.perform(post("/panel/movimientos").session(c).with(csrf())
                .param("tipo", "GASTO").param("monto", "1000").param("categoria", "OTROS"))
            .andExpect(redirectedUrl("/panel?denegado"));
        assertThat(movimientos.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("8. admin / 1234 → insignia ADMIN y todas las pestañas, incluida Gestión de Usuarios")
    void rolAdmin() throws Exception {
        MockHttpSession s = ingresar(new MockHttpSession(), "admin", "1234");
        String html = panel(s, "usuarios");
        assertThat(html).contains("ROL: ADMIN").contains("Registrar movimiento").contains("Gestión de Usuarios")
            .contains("Crear nuevo usuario").contains("Eliminar usuario seleccionado")
            // La tabla de usuarios nunca muestra contraseñas
            .doesNotContain("$2a$");
    }

    @Test
    @DisplayName("9. El admin no puede eliminar su propia cuenta en sesión")
    void noEliminarseASiMismo() throws Exception {
        MockHttpSession s = ingresar(new MockHttpSession(), "admin", "1234");
        Long idAdmin = usuarios.findByUsuarioIgnoreCase("admin").orElseThrow().getId();
        MvcResult r = mvc.perform(post("/panel/usuarios/eliminar").session(s).with(csrf()).param("id", idAdmin.toString())).andReturn();
        Aviso aviso = (Aviso) r.getFlashMap().get("aviso");
        assertThat(aviso.mensaje()).isEqualTo("No puede eliminar su propia cuenta mientras está en sesión.");
        assertThat(usuarios.existsByUsuarioIgnoreCase("admin")).isTrue();
    }

    @Test
    @DisplayName("10. Crear «maria» (clave abcd, OPERADOR) y entrar con ella")
    void crearMaria() throws Exception {
        MockHttpSession admin = ingresar(new MockHttpSession(), "admin", "1234");
        mvc.perform(post("/panel/usuarios").session(admin).with(csrf())
            .param("usuario", "maria").param("clave", "abcd").param("nombreCompleto", "María Gómez").param("rol", "OPERADOR"));
        assertThat(usuarios.existsByUsuarioIgnoreCase("maria")).isTrue();

        MockHttpSession s = ingresar(new MockHttpSession(), "MARIA", "abcd"); // el usuario ignora mayúsculas
        assertThat(panel(s, "inicio")).contains("ROL: OPERADOR").contains("María Gómez");
        // y la contraseña sí exige coincidencia exacta
        MockHttpSession otra = ingresar(new MockHttpSession(), "maria", "ABCD");
        assertThat(avisoLogin(otra).mensaje()).contains("Usuario o contraseña incorrectos");
    }

    @Test
    @DisplayName("Reto 1: la contraseña de un usuario nuevo debe tener al menos 4 caracteres")
    void claveMinima() throws Exception {
        MockHttpSession admin = ingresar(new MockHttpSession(), "admin", "1234");
        MvcResult r = mvc.perform(post("/panel/usuarios").session(admin).with(csrf())
            .param("usuario", "pepe").param("clave", "123").param("nombreCompleto", "Pepe Ruiz").param("rol", "CONSULTA")).andReturn();
        assertThat(((Aviso) r.getFlashMap().get("aviso")).mensaje()).contains("al menos 4");
        assertThat(usuarios.existsByUsuarioIgnoreCase("pepe")).isFalse();
    }

    @Test
    @DisplayName("Registro público: la cuenta queda pendiente hasta que el admin la apruebe")
    void registroConAprobacion() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                .param("nombreCompleto", "Juan Pérez").param("usuario", "juanp").param("correo", "juan@ejemplo.com")
                .param("celular", "3101234567").param("clave", "secreto1").param("confirmar", "secreto1").param("aceptaDatos", "true"))
            .andExpect(redirectedUrl("/login"));

        MockHttpSession s = ingresar(new MockHttpSession(), "juanp", "secreto1");
        assertThat(avisoLogin(s).titulo()).isEqualTo("Cuenta pendiente");

        MockHttpSession admin = ingresar(new MockHttpSession(), "admin", "1234");
        Long id = usuarios.findByUsuarioIgnoreCase("juanp").orElseThrow().getId();
        mvc.perform(post("/panel/usuarios/" + id + "/aprobar").session(admin).with(csrf()).param("rol", "OPERADOR"));

        MockHttpSession juan = ingresar(new MockHttpSession(), "juanp", "secreto1");
        assertThat(panel(juan, "inicio")).contains("ROL: OPERADOR");
    }

    @Test
    @DisplayName("Sin iniciar sesión no se puede ver el panel")
    void panelProtegido() throws Exception {
        mvc.perform(get("/panel")).andExpect(status().is3xxRedirection());
    }
}
