package com.moneyos.servicio;

import com.moneyos.modelo.*;
import com.moneyos.repositorio.AjusteRepositorio;
import com.moneyos.repositorio.MovimientoRepositorio;
import com.moneyos.util.Colombia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Todo lo relacionado con la plata: registrar movimientos y calcular saldo, totales,
 * gráficas, presupuesto y resúmenes. Todas las fechas en hora de Colombia.
 */
@Service
public class FinanzasServicio {

    private static final String CLAVE_PRESUPUESTO = "presupuesto_mensual";

    private final MovimientoRepositorio movimientos;
    private final AjusteRepositorio ajustes;

    public FinanzasServicio(MovimientoRepositorio movimientos, AjusteRepositorio ajustes) {
        this.movimientos = movimientos;
        this.ajustes = ajustes;
    }

    // ===================== Registrar y eliminar =====================

    @Transactional
    public Resultado registrar(Usuario autor, String tipoTexto, String montoTexto, String categoriaTexto,
                               String descripcion, String fechaTexto) {
        if (!autor.getRol().puedeRegistrar()) {
            return Resultado.error("Su rol (" + autor.getRol() + ") no permite registrar movimientos.");
        }
        if (tipoTexto == null || tipoTexto.isBlank() || montoTexto == null || montoTexto.isBlank()) {
            return Resultado.error("Escriba el monto del movimiento.");
        }
        if (categoriaTexto == null || categoriaTexto.isBlank()) {
            return Resultado.error("Elija qué tipo de gasto es (Comida, Transporte, Compras…).");
        }

        TipoMovimiento tipo;
        Categoria categoria;
        try {
            tipo = TipoMovimiento.valueOf(tipoTexto);
            categoria = Categoria.valueOf(categoriaTexto);
        } catch (IllegalArgumentException ex) {
            return Resultado.error("El tipo de movimiento no es válido.");
        }
        if (categoria.getTipo() != tipo) {
            return Resultado.error("La categoría no corresponde a un " + tipo.getNombre().toLowerCase() + ".");
        }

        long monto = Colombia.leerMonto(montoTexto);
        if (monto <= 0) {
            return Resultado.error("El monto debe ser un número mayor que cero (ej: 25000).");
        }

        LocalDate fecha = Colombia.hoy();
        if (fechaTexto != null && !fechaTexto.isBlank()) {
            try {
                fecha = LocalDate.parse(fechaTexto);
            } catch (Exception ex) {
                return Resultado.error("La fecha no es válida.");
            }
        }
        if (fecha.isAfter(Colombia.hoy())) {
            return Resultado.error("La fecha no puede ser futura.");
        }

        String desc = descripcion == null ? "" : descripcion.trim();
        if (desc.length() > 80) desc = desc.substring(0, 80);

        movimientos.save(new Movimiento(tipo, monto, categoria, desc, fecha, autor));
        String que = tipo == TipoMovimiento.INGRESO ? "Ingreso" : "Gasto";
        return Resultado.bien(que + " de " + Colombia.pesos(monto) + " guardado en " + categoria.getNombre() + ".");
    }

    /** ADMIN puede borrar cualquiera; OPERADOR solo los que registró; CONSULTA ninguno. */
    @Transactional
    public Resultado eliminar(Long id, Usuario actual) {
        Optional<Movimiento> encontrado = movimientos.findById(id);
        if (encontrado.isEmpty()) {
            return Resultado.error("Ese movimiento ya no existe.");
        }
        Movimiento m = encontrado.get();
        boolean esSuyo = m.getRegistradoPor() != null && m.getRegistradoPor().getId().equals(actual.getId());
        if (actual.getRol() == Rol.CONSULTA || (actual.getRol() == Rol.OPERADOR && !esSuyo)) {
            return Resultado.error("Su rol no permite eliminar este movimiento.");
        }
        movimientos.delete(m);
        return Resultado.bien("Movimiento eliminado.");
    }

    public boolean puedeEliminar(Movimiento m, Usuario actual) {
        if (actual.getRol() == Rol.ADMIN) return true;
        if (actual.getRol() != Rol.OPERADOR) return false;
        return m.getRegistradoPor() != null && m.getRegistradoPor().getId().equals(actual.getId());
    }

    // ===================== Consultas =====================

    @Transactional(readOnly = true)
    public List<Movimiento> todos() {
        return movimientos.findAllRecientes();
    }

    public long totalMovimientos() {
        return movimientos.count();
    }

    /** Filtra por texto (descripción o categoría) y por categoría, como el buscador de la guía. */
    public List<Movimiento> filtrar(List<Movimiento> lista, String texto, String categoria) {
        List<Movimiento> resultado = new ArrayList<>();
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        for (Movimiento m : lista) {
            boolean coincideTexto = buscado.isEmpty()
                || m.getTitulo().toLowerCase().contains(buscado)
                || m.getCategoria().getNombre().toLowerCase().contains(buscado);
            boolean coincideCategoria = categoria == null || categoria.isBlank()
                || ("INGRESOS".equals(categoria) ? m.esIngreso() : m.getCategoria().name().equals(categoria));
            if (coincideTexto && coincideCategoria) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    /** Agrupa por día (ya vienen ordenados del más reciente al más antiguo). */
    public List<GrupoDia> agruparPorDia(List<Movimiento> lista) {
        Map<LocalDate, List<Movimiento>> mapa = new LinkedHashMap<>();
        for (Movimiento m : lista) {
            mapa.computeIfAbsent(m.getFecha(), f -> new ArrayList<>()).add(m);
        }
        List<GrupoDia> grupos = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Movimiento>> e : mapa.entrySet()) {
            long total = 0;
            for (Movimiento m : e.getValue()) total += m.getMontoConSigno();
            grupos.add(new GrupoDia(Colombia.diaRelativo(e.getKey()), total, e.getValue()));
        }
        return grupos;
    }

    public long saldo(List<Movimiento> lista) {
        return saldoHasta(lista, null);
    }

    public long saldoHasta(List<Movimiento> lista, LocalDate hasta) {
        long total = 0;
        for (Movimiento m : lista) {
            if (hasta == null || !m.getFecha().isAfter(hasta)) {
                total += m.getMontoConSigno();
            }
        }
        return total;
    }

    public List<Movimiento> delMes(List<Movimiento> lista, YearMonth mes) {
        List<Movimiento> r = new ArrayList<>();
        for (Movimiento m : lista) {
            if (YearMonth.from(m.getFecha()).equals(mes)) r.add(m);
        }
        return r;
    }

    /** Ingresos del periodo sin contar el saldo inicial (que no es plata nueva). */
    public long ingresos(List<Movimiento> lista) {
        long t = 0;
        for (Movimiento m : lista) {
            if (m.esIngreso() && m.getCategoria() != Categoria.SALDO_INICIAL) t += m.getMonto();
        }
        return t;
    }

    public long gastos(List<Movimiento> lista) {
        long t = 0;
        for (Movimiento m : lista) {
            if (!m.esIngreso()) t += m.getMonto();
        }
        return t;
    }

    public List<TotalCategoria> gastosPorCategoria(List<Movimiento> lista) {
        Map<Categoria, long[]> mapa = new EnumMap<>(Categoria.class);
        long total = gastos(lista);
        for (Movimiento m : lista) {
            if (m.esIngreso()) continue;
            long[] acumulado = mapa.computeIfAbsent(m.getCategoria(), c -> new long[2]);
            acumulado[0] += m.getMonto();
            acumulado[1]++;
        }
        List<TotalCategoria> r = new ArrayList<>();
        for (Map.Entry<Categoria, long[]> e : mapa.entrySet()) {
            double pct = total > 0 ? e.getValue()[0] * 100.0 / total : 0;
            r.add(new TotalCategoria(e.getKey(), e.getValue()[0], (int) e.getValue()[1], pct));
        }
        r.sort((a, b) -> Long.compare(b.monto(), a.monto()));
        return r;
    }

    /** Gráfica de dona hecha con CSS: "conic-gradient(#F4F4F5 0% 35%, ...)". */
    public String dona(List<TotalCategoria> categorias) {
        if (categorias.isEmpty()) return "conic-gradient(#262629 0% 100%)";
        StringBuilder sb = new StringBuilder("conic-gradient(");
        double inicio = 0;
        for (int i = 0; i < categorias.size(); i++) {
            TotalCategoria c = categorias.get(i);
            double fin = i == categorias.size() - 1 ? 100 : inicio + c.porcentaje();
            if (i > 0) sb.append(", ");
            sb.append(c.categoria().getColor()).append(' ')
              .append(String.format(Locale.ROOT, "%.2f%% %.2f%%", inicio, fin));
            inicio = fin;
        }
        return sb.append(')').toString();
    }

    /** Barras del gasto de los últimos 6 meses (el actual resaltado). */
    public List<Barra> gastoMensual(List<Movimiento> lista, int meses) {
        YearMonth actual = Colombia.mesActual();
        List<long[]> valores = new ArrayList<>();
        List<YearMonth> etiquetas = new ArrayList<>();
        long maximo = 0;
        for (int i = meses - 1; i >= 0; i--) {
            YearMonth mes = actual.minusMonths(i);
            long g = gastos(delMes(lista, mes));
            valores.add(new long[]{g});
            etiquetas.add(mes);
            maximo = Math.max(maximo, g);
        }
        List<Barra> barras = new ArrayList<>();
        for (int i = 0; i < etiquetas.size(); i++) {
            long v = valores.get(i)[0];
            barras.add(new Barra(Colombia.mesCorto(etiquetas.get(i)), v, maximo > 0 ? v * 100.0 / maximo : 0,
                i == etiquetas.size() - 1));
        }
        return barras;
    }

    /** Barras del gasto de cada día de esta semana (lunes a domingo). */
    public List<Barra> gastoSemanal(List<Movimiento> lista) {
        LocalDate hoy = Colombia.hoy();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        String[] nombres = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        long[] valores = new long[7];
        long maximo = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunes.plusDays(i);
            for (Movimiento m : lista) {
                if (!m.esIngreso() && m.getFecha().equals(dia)) valores[i] += m.getMonto();
            }
            maximo = Math.max(maximo, valores[i]);
        }
        List<Barra> barras = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            barras.add(new Barra(nombres[i], valores[i], maximo > 0 ? valores[i] * 100.0 / maximo : 0,
                lunes.plusDays(i).equals(hoy)));
        }
        return barras;
    }

    /** Saldo al final de cada uno de los últimos meses, como puntos de una línea SVG (600×160). */
    public LineaSaldo lineaSaldo(List<Movimiento> lista, int meses) {
        YearMonth actual = Colombia.mesActual();
        List<String> etiquetas = new ArrayList<>();
        long[] valores = new long[meses];
        for (int i = 0; i < meses; i++) {
            YearMonth mes = actual.minusMonths(meses - 1 - i);
            LocalDate corte = mes.equals(actual) ? Colombia.hoy() : mes.atEndOfMonth();
            valores[i] = saldoHasta(lista, corte);
            etiquetas.add(Colombia.mesCorto(mes));
        }
        long min = Arrays.stream(valores).min().orElse(0);
        long max = Arrays.stream(valores).max().orElse(0);
        long rango = Math.max(1, max - min);
        StringBuilder puntos = new StringBuilder();
        List<double[]> coords = new ArrayList<>();
        for (int i = 0; i < meses; i++) {
            double x = 30 + i * (540.0 / (meses - 1));
            double y = 125 - (valores[i] - min) * 95.0 / rango;
            coords.add(new double[]{x, y});
            puntos.append(String.format(Locale.ROOT, "%.1f,%.1f ", x, y));
        }
        return new LineaSaldo(puntos.toString().trim(), coords, etiquetas, valores);
    }

    // ===================== Presupuesto =====================

    public long presupuesto() {
        return ajustes.findById(CLAVE_PRESUPUESTO).map(a -> Long.parseLong(a.getValor())).orElse(0L);
    }

    @Transactional
    public Resultado guardarPresupuesto(Usuario actual, String montoTexto) {
        if (!actual.getRol().puedeRegistrar()) {
            return Resultado.error("Su rol no permite cambiar el presupuesto.");
        }
        long monto = montoTexto == null || montoTexto.isBlank() ? 0 : Colombia.leerMonto(montoTexto);
        if (monto < 0) {
            return Resultado.error("El presupuesto debe ser un número (ej: 1500000).");
        }
        ajustes.save(new Ajuste(CLAVE_PRESUPUESTO, String.valueOf(monto)));
        return Resultado.bien(monto == 0 ? "Presupuesto quitado." : "Presupuesto mensual: " + Colombia.pesos(monto) + ".");
    }

    // ===================== Resumen mensual =====================

    public List<YearMonth> mesesTerminadosConDatos(List<Movimiento> lista) {
        TreeSet<YearMonth> meses = new TreeSet<>(Comparator.reverseOrder());
        YearMonth actual = Colombia.mesActual();
        for (Movimiento m : lista) {
            YearMonth mes = YearMonth.from(m.getFecha());
            if (mes.isBefore(actual)) meses.add(mes);
        }
        return new ArrayList<>(meses);
    }

    /** El mes que acaba de terminar, si tiene movimientos y este usuario aún no vio su resumen. */
    public Optional<YearMonth> resumenPendiente(List<Movimiento> lista, Usuario usuario) {
        YearMonth anterior = Colombia.mesActual().minusMonths(1);
        if (delMes(lista, anterior).isEmpty()) return Optional.empty();
        if (anterior.toString().equals(usuario.getResumenVisto())) return Optional.empty();
        return Optional.of(anterior);
    }

    public ResumenMes resumen(List<Movimiento> lista, YearMonth mes) {
        List<Movimiento> items = delMes(lista, mes);
        long ingresos = ingresos(items);
        long gastos = gastos(items);
        List<TotalCategoria> categorias = gastosPorCategoria(items);

        Map<LocalDate, Long> porDia = new HashMap<>();
        List<Movimiento> gastosLista = new ArrayList<>();
        for (Movimiento m : items) {
            if (!m.esIngreso()) {
                porDia.merge(m.getFecha(), m.getMonto(), Long::sum);
                gastosLista.add(m);
            }
        }
        gastosLista.sort((a, b) -> Long.compare(b.getMonto(), a.getMonto()));
        Map.Entry<LocalDate, Long> diaMayor = porDia.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);

        long gastosAnterior = gastos(delMes(lista, mes.minusMonths(1)));
        boolean huboAnterior = !delMes(lista, mes.minusMonths(1)).isEmpty();
        Double comparacion = huboAnterior && gastosAnterior > 0 ? (gastos - gastosAnterior) * 100.0 / gastosAnterior : null;

        return new ResumenMes(
            mes, Colombia.nombreMes(mes), ingresos, gastos, ingresos - gastos,
            ingresos > 0 ? (ingresos - gastos) * 100.0 / ingresos : null,
            items.size(), gastosLista.size(), porDia.size(),
            categorias, gastosLista.subList(0, Math.min(5, gastosLista.size())),
            diaMayor == null ? null : Colombia.diaRelativo(diaMayor.getKey()),
            diaMayor == null ? 0 : diaMayor.getValue(),
            Math.round(gastos / (double) mes.lengthOfMonth()),
            saldoHasta(lista, mes.atDay(1).minusDays(1)),
            saldoHasta(lista, mes.atEndOfMonth()),
            comparacion
        );
    }

    // ===================== Registros (records) para las pantallas =====================

    public record GrupoDia(String dia, long total, List<Movimiento> movimientos) {}

    public record TotalCategoria(Categoria categoria, long monto, int cantidad, double porcentaje) {}

    public record Barra(String etiqueta, long valor, double altura, boolean resaltada) {}

    public record LineaSaldo(String puntos, List<double[]> coordenadas, List<String> etiquetas, long[] valores) {}

    public record ResumenMes(YearMonth mes, String nombre, long ingresos, long gastos, long ahorro, Double tasaAhorro,
                             int movimientos, int cantidadGastos, int diasConGasto,
                             List<TotalCategoria> categorias, List<Movimiento> mayoresGastos,
                             String diaMayorGasto, long montoDiaMayor, long promedioDiario,
                             long saldoInicio, long saldoFin, Double comparacionMesAnterior) {}
}
