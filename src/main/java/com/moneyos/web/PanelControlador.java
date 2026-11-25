package com.moneyos.web;

import com.moneyos.modelo.*;
import com.moneyos.seguridad.ManejadorLoginExitoso;
import com.moneyos.servicio.FinanzasServicio;
import com.moneyos.servicio.FinanzasServicio.TotalCategoria;
import com.moneyos.servicio.UsuarioServicio;
import com.moneyos.util.Colombia;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.YearMonth;
import java.util.*;

/**
 * El Panel Administrativo (PanelAdministrativo.mostrarPanel en la guía).
 * Según el rol se construyen unas pestañas u otras.
 */
@Controller
public class PanelControlador {

    private final UsuarioServicio usuarios;
    private final FinanzasServicio finanzas;

    public PanelControlador(UsuarioServicio usuarios, FinanzasServicio finanzas) {
        this.usuarios = usuarios;
        this.finanzas = finanzas;
    }

    /** Las pestañas que puede ver cada rol (los if de la guía). */
    static List<Pestana> pestanasDe(Rol rol) {
        List<Pestana> lista = new ArrayList<>();
        // Visibles para TODOS los roles
        lista.add(new Pestana("inicio", "Inicio", "inicio"));
        lista.add(new Pestana("movimientos", "Movimientos", "lista"));
        // Solo ADMIN y OPERADOR
        if (rol == Rol.ADMIN || rol == Rol.OPERADOR) {
            lista.add(new Pestana("registrar", "Registrar movimiento", "mas"));
        }
        lista.add(new Pestana("graficas", "Gráficas y resúmenes", "graficas"));
        // SOLO ADMIN
        if (rol == Rol.ADMIN) {
            lista.add(new Pestana("usuarios", "Gestión de Usuarios", "usuarios"));
        }
        lista.add(new Pestana("cuenta", "Mi cuenta", "cuenta"));
        return lista;
    }

    @GetMapping("/panel")
    public String panel(@RequestParam(defaultValue = "inicio") String pestana,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String cat,
                        @RequestParam(required = false) String mes,
                        @RequestParam(required = false) String tipo,
                        @RequestParam(required = false) String denegado,
                        Principal principal, HttpServletRequest request, Model modelo) {
        Optional<Usuario> encontrado = usuarios.buscar(principal.getName());
        if (encontrado.isEmpty() || !encontrado.get().estaActivo()) {
            // El usuario fue eliminado o desactivado mientras tenía la sesión abierta
            request.getSession().invalidate();
            return "redirect:/login";
        }
        Usuario actual = encontrado.get();
        List<Pestana> pestanas = pestanasDe(actual.getRol());

        // Aviso pendiente: bienvenida, resultado de una acción o acceso denegado
        HttpSession sesion = request.getSession();
        Object bienvenida = sesion.getAttribute(ManejadorLoginExitoso.AVISO_PANEL);
        if (bienvenida != null) {
            sesion.removeAttribute(ManejadorLoginExitoso.AVISO_PANEL);
            modelo.addAttribute("aviso", bienvenida);
        }
        boolean permitida = pestanas.stream().anyMatch(p -> p.clave().equals(pestana));
        String activa = permitida ? pestana : "inicio";
        if (!permitida || denegado != null) {
            modelo.addAttribute("aviso", Aviso.advertencia("Acceso restringido",
                "Su rol (" + actual.getRol() + ") no tiene permiso para esa sección."));
        }

        modelo.addAttribute("usuario", actual);
        modelo.addAttribute("rol", actual.getRol());
        modelo.addAttribute("pestanas", pestanas);
        modelo.addAttribute("pestana", activa);

        List<Movimiento> todos = finanzas.todos();
        switch (activa) {
            case "movimientos" -> cargarMovimientos(modelo, todos, q, cat, actual);
            case "registrar" -> cargarRegistrar(modelo, tipo);
            case "graficas" -> cargarGraficas(modelo, todos, mes);
            case "usuarios" -> cargarUsuarios(modelo, q);
            case "cuenta" -> { /* solo usa "usuario" */ }
            default -> cargarInicio(modelo, todos, actual);
        }
        return "panel";
    }

    private void cargarInicio(Model modelo, List<Movimiento> todos, Usuario actual) {
        YearMonth mes = Colombia.mesActual();
        List<Movimiento> delMes = finanzas.delMes(todos, mes);
        long ingresos = finanzas.ingresos(delMes);
        long gastos = finanzas.gastos(delMes);
        long presupuesto = finanzas.presupuesto();
        List<TotalCategoria> categorias = finanzas.gastosPorCategoria(delMes);

        modelo.addAttribute("saldo", finanzas.saldo(todos));
        modelo.addAttribute("ingresosMes", ingresos);
        modelo.addAttribute("gastosMes", gastos);
        modelo.addAttribute("ahorroMes", ingresos - gastos);
        modelo.addAttribute("totalMovimientos", todos.size());
        modelo.addAttribute("totalUsuarios", usuarios.totalActivos());
        modelo.addAttribute("pendientes", usuarios.totalPendientes());
        modelo.addAttribute("presupuesto", presupuesto);
        modelo.addAttribute("presupuestoPct", presupuesto > 0 ? Math.round(gastos * 100.0 / presupuesto) : 0);
        modelo.addAttribute("diasRestantes", mes.lengthOfMonth() - Colombia.hoy().getDayOfMonth() + 1);
        modelo.addAttribute("categorias", categorias);
        modelo.addAttribute("dona", finanzas.dona(categorias));
        modelo.addAttribute("recientes", todos.subList(0, Math.min(5, todos.size())));
        modelo.addAttribute("nombreMes", Colombia.nombreMes(mes));
        finanzas.resumenPendiente(todos, actual)
            .ifPresent(m -> modelo.addAttribute("resumenPendiente", finanzas.resumen(todos, m)));
    }

    private void cargarMovimientos(Model modelo, List<Movimiento> todos, String q, String cat, Usuario actual) {
        List<Movimiento> filtrados = finanzas.filtrar(todos, q, cat);
        Set<Long> borrables = new HashSet<>();
        for (Movimiento m : filtrados) {
            if (finanzas.puedeEliminar(m, actual)) borrables.add(m.getId());
        }
        modelo.addAttribute("grupos", finanzas.agruparPorDia(filtrados));
        modelo.addAttribute("cantidad", filtrados.size());
        modelo.addAttribute("totalGastos", finanzas.gastos(filtrados));
        modelo.addAttribute("totalIngresos", finanzas.ingresos(filtrados));
        modelo.addAttribute("borrables", borrables);
        modelo.addAttribute("q", q == null ? "" : q);
        modelo.addAttribute("cat", cat == null ? "" : cat);
        modelo.addAttribute("categoriasGasto", Categoria.delTipo(TipoMovimiento.GASTO));
    }

    private void cargarRegistrar(Model modelo, String tipo) {
        modelo.addAttribute("tipoInicial", "INGRESO".equals(tipo) ? "INGRESO" : "GASTO");
        modelo.addAttribute("categoriasGasto", Categoria.delTipo(TipoMovimiento.GASTO));
        modelo.addAttribute("categoriasIngreso", Categoria.delTipo(TipoMovimiento.INGRESO));
        modelo.addAttribute("hoy", Colombia.hoy().toString());
        modelo.addAttribute("saldo", finanzas.saldo(finanzas.todos()));
        modelo.addAttribute("presupuesto", finanzas.presupuesto());
    }

    private void cargarGraficas(Model modelo, List<Movimiento> todos, String mes) {
        YearMonth actual = Colombia.mesActual();
        List<TotalCategoria> categorias = finanzas.gastosPorCategoria(finanzas.delMes(todos, actual));
        modelo.addAttribute("barrasMes", finanzas.gastoMensual(todos, 6));
        modelo.addAttribute("barrasSemana", finanzas.gastoSemanal(todos));
        modelo.addAttribute("linea", finanzas.lineaSaldo(todos, 6));
        modelo.addAttribute("categorias", categorias);
        modelo.addAttribute("dona", finanzas.dona(categorias));
        modelo.addAttribute("nombreMes", Colombia.nombreMes(actual));
        modelo.addAttribute("mesesResumen", finanzas.mesesTerminadosConDatos(todos));
        if (mes != null) {
            try {
                modelo.addAttribute("resumen", finanzas.resumen(todos, YearMonth.parse(mes)));
            } catch (Exception ignorado) {
                // mes inválido: se muestra la lista
            }
        }
    }

    private void cargarUsuarios(Model modelo, String q) {
        modelo.addAttribute("listaUsuarios", usuarios.buscarPorTexto(q));
        modelo.addAttribute("roles", Rol.values());
        modelo.addAttribute("q", q == null ? "" : q);
        modelo.addAttribute("ahora", Colombia.ahora());
    }
}
