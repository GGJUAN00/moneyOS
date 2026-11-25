// MoneyOS — pequeños detalles en el navegador.
// La lógica (usuarios, roles, saldo, validaciones) está en Java; aquí solo hay comodidades visuales:
// ventanas emergentes, mostrar contraseña, reloj de Colombia, formato del monto y sonidos.
(function () {
  'use strict';

  // ---------- Sonidos suaves tipo campana (Web Audio, sin archivos) ----------
  var ctx = null;
  function audio() {
    try {
      if (!ctx) {
        var AC = window.AudioContext || window.webkitAudioContext;
        if (!AC) return null;
        ctx = new AC();
      }
      if (ctx.state === 'suspended') ctx.resume();
      return ctx;
    } catch (e) { return null; }
  }
  function campana(freq, inicio, duracion, volumen) {
    var a = audio();
    if (!a) return;
    var t0 = a.currentTime + inicio;
    var env = a.createGain();
    env.gain.setValueAtTime(0.0001, t0);
    env.gain.exponentialRampToValueAtTime(volumen * 0.22, t0 + 0.008);
    env.gain.exponentialRampToValueAtTime(0.0001, t0 + duracion);
    env.connect(a.destination);
    [[1, 1], [2, 0.18], [3.01, 0.05]].forEach(function (p) {
      var osc = a.createOscillator();
      var g = a.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq * p[0], t0);
      g.gain.value = p[1];
      osc.connect(g); g.connect(env);
      osc.start(t0); osc.stop(t0 + duracion + 0.05);
    });
  }
  var sonidos = {
    ingreso: function () { campana(1318.5, 0, 0.9, 0.55); campana(1975.5, 0.085, 1.1, 0.45); },
    gasto: function () { campana(783.99, 0, 0.7, 0.5); campana(587.33, 0.09, 0.9, 0.42); },
    borrar: function () { campana(392, 0, 0.5, 0.35); },
    toque: function () { campana(1760, 0, 0.09, 0.18); }
  };
  var sonidoActivo = document.body.dataset.sonido !== 'false';
  function sonar(nombre) { if (sonidoActivo && sonidos[nombre]) sonidos[nombre](); }

  // ---------- Ventana de aviso (JOptionPane) ----------
  var aviso = document.getElementById('aviso');
  if (aviso && aviso.showModal) {
    aviso.showModal();
    var boton = aviso.querySelector('[data-cerrar]');
    if (boton) boton.focus();
    // El navegador solo deja sonar después de un toque: suena al presionar "Aceptar"
    aviso.addEventListener('close', function () { sonar(aviso.dataset.sonido); });
  }
  document.querySelectorAll('[data-cerrar]').forEach(function (b) {
    b.addEventListener('click', function () { b.closest('dialog').close(); });
  });

  // ---------- Confirmaciones (JOptionPane.YES_NO_OPTION) ----------
  var confirmar = document.getElementById('confirmar');
  var formularioPendiente = null;
  document.querySelectorAll('form[data-confirmar]').forEach(function (f) {
    f.addEventListener('submit', function (e) {
      if (f.dataset.confirmado === 'si' || !confirmar) return;
      e.preventDefault();
      var mensaje = f.dataset.confirmar;
      // getAttribute: f.id devolvería el campo llamado "id" del formulario, no su nombre
      var elegido = document.querySelector('input[name="id"][form="' + f.getAttribute('id') + '"]:checked');
      if (elegido) mensaje = mensaje.replace('{usuario}', elegido.dataset.usuario || '');
      if (mensaje.indexOf('{usuario}') >= 0) {
        f.dataset.confirmado = 'si';
        f.submit();
        return;
      }
      confirmar.querySelector('h2').textContent = f.dataset.tituloConfirmar || 'Confirmar';
      confirmar.querySelector('p').textContent = mensaje;
      formularioPendiente = f;
      confirmar.showModal();
    });
  });
  if (confirmar) {
    confirmar.querySelector('[data-si]').addEventListener('click', function () {
      confirmar.close();
      if (formularioPendiente) {
        formularioPendiente.dataset.confirmado = 'si';
        formularioPendiente.submit();
      }
    });
  }

  // ---------- Mostrar contraseña (JCheckBox de la guía) ----------
  document.querySelectorAll('[data-mostrar-clave]').forEach(function (chk) {
    chk.addEventListener('change', function () {
      document.querySelectorAll(chk.dataset.mostrarClave).forEach(function (campo) {
        campo.type = chk.checked ? 'text' : 'password';
      });
    });
  });

  // ---------- Botón Limpiar ----------
  document.querySelectorAll('[data-limpiar]').forEach(function (b) {
    b.addEventListener('click', function () {
      var f = b.closest('form');
      f.querySelectorAll('input:not([type=hidden]):not([type=checkbox]):not([type=radio]), textarea').forEach(function (i) { i.value = ''; });
      f.querySelectorAll('input[type=checkbox]').forEach(function (i) { i.checked = false; i.dispatchEvent(new Event('change')); });
      var primero = f.querySelector('input:not([type=hidden]):not([type=checkbox]):not([type=radio])');
      if (primero) primero.focus();
    });
  });

  // ---------- Reloj de Colombia ----------
  var relojes = document.querySelectorAll('[data-reloj]');
  function actualizarReloj() {
    var ahora = new Date();
    relojes.forEach(function (el) {
      var opciones = el.dataset.reloj === 'completo'
        ? { timeZone: 'America/Bogota', weekday: 'long', day: 'numeric', month: 'long', hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true }
        : { timeZone: 'America/Bogota', hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true };
      var texto = ahora.toLocaleString('es-CO', opciones);
      el.textContent = texto.charAt(0).toUpperCase() + texto.slice(1);
    });
  }
  if (relojes.length) { actualizarReloj(); setInterval(actualizarReloj, 1000); }

  // ---------- Cuenta regresiva del bloqueo del login ----------
  var bloqueo = document.querySelector('[data-bloqueo]');
  if (bloqueo) {
    var segundos = parseInt(bloqueo.dataset.bloqueo, 10);
    var pintar = function () {
      var m = Math.floor(segundos / 60), s = segundos % 60;
      bloqueo.textContent = m + ':' + (s < 10 ? '0' : '') + s;
      if (segundos <= 0) location.reload();
      segundos--;
    };
    pintar(); setInterval(pintar, 1000);
  }

  // ---------- Monto con puntos de mil mientras se escribe ----------
  document.querySelectorAll('[data-monto]').forEach(function (campo) {
    campo.addEventListener('input', function () {
      var digitos = campo.value.replace(/\D/g, '').slice(0, 12);
      campo.value = digitos ? Number(digitos).toLocaleString('es-CO') : '';
    });
  });

  // ---------- Registrar: cambiar entre Gasto e Ingreso ----------
  var tipos = document.querySelectorAll('input[name="tipo"]');
  function mostrarCategorias() {
    var elegido = document.querySelector('input[name="tipo"]:checked');
    if (!elegido) return;
    document.querySelectorAll('[data-categorias]').forEach(function (grupo) {
      var visible = grupo.dataset.categorias === elegido.value;
      grupo.classList.toggle('oculto', !visible);
      grupo.querySelectorAll('input').forEach(function (i) { i.disabled = !visible; });
    });
    var titulo = document.querySelector('[data-pregunta-categoria]');
    if (titulo) titulo.textContent = elegido.value === 'INGRESO' ? '¿De dónde viene la plata?' : '¿Qué tipo de gasto es?';
    var pregunta = document.querySelector('[data-pregunta-monto]');
    if (pregunta) pregunta.textContent = elegido.value === 'INGRESO' ? '¿Cuánta plata entra?' : '¿Cuánto gastaste?';
  }
  tipos.forEach(function (t) { t.addEventListener('change', function () { sonar('toque'); mostrarCategorias(); }); });
  mostrarCategorias();

  // ---------- Tabla de usuarios: resaltar la fila elegida ----------
  document.querySelectorAll('input[name="id"][type=radio]').forEach(function (r) {
    r.addEventListener('change', function () {
      document.querySelectorAll('tr.seleccionada').forEach(function (tr) { tr.classList.remove('seleccionada'); });
      r.closest('tr').classList.add('seleccionada');
    });
  });
})();
