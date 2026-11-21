// MoneyOS desktop app (Electron). Loads the same React app that runs on the web, from disk.
const { app, BrowserWindow, ipcMain, Menu, nativeTheme, powerMonitor, shell, systemPreferences } = require('electron')
const { execFile } = require('node:child_process')
const path = require('node:path')

app.setName('MoneyOS')
app.setAppUserModelId('co.moneyos.app') // Windows taskbar grouping & notifications

// Only one MoneyOS window at a time
if (!app.requestSingleInstanceLock()) app.quit()

nativeTheme.themeSource = 'dark'

let win = null

function createWindow() {
  win = new BrowserWindow({
    width: 1240,
    height: 820,
    minWidth: 400,
    minHeight: 640,
    show: false,
    title: 'MoneyOS',
    backgroundColor: '#050505',
    autoHideMenuBar: true,
    titleBarStyle: process.platform === 'darwin' ? 'hiddenInset' : 'default',
    icon: path.join(__dirname, 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      sandbox: true,
      nodeIntegration: false,
      spellcheck: false,
    },
  })

  win.loadFile(path.join(__dirname, '..', 'dist', 'index.html'))
  win.once('ready-to-show', () => win.show())

  // Links never open inside the app: send them to the normal browser
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:\/\//.test(url)) shell.openExternal(url)
    return { action: 'deny' }
  })
  win.webContents.on('will-navigate', (event, url) => {
    if (!url.startsWith('file://')) {
      event.preventDefault()
      if (/^https?:\/\//.test(url)) shell.openExternal(url)
    }
  })

  win.on('closed', () => { win = null })
}

// Ask the app to lock itself (computer locked / going to sleep)
function requestLock() {
  if (win && !win.isDestroyed()) win.webContents.send('moneyos:lock')
}

// ---- Biometrics: Touch ID (macOS) and Windows Hello (Windows) ----

const HELLO_PREAMBLE = `
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation\`1' })[0]
function Await($op, $type) { $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(-1) | Out-Null; $t.Result }
[Windows.Security.Credentials.UI.UserConsentVerifier, Windows.Security.Credentials.UI, ContentType = WindowsRuntime] | Out-Null
`

function powershell(script) {
  return new Promise(resolve => {
    execFile(
      'powershell.exe',
      ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-Command', script],
      { windowsHide: true, timeout: 120000 },
      (err, stdout) => resolve(err ? '' : String(stdout).trim()),
    )
  })
}

let helloAvailable = null
async function biometricAvailable() {
  if (process.platform === 'darwin') return systemPreferences.canPromptTouchID()
  if (process.platform === 'win32') {
    if (helloAvailable === null) {
      const out = await powershell(HELLO_PREAMBLE + `Await ([Windows.Security.Credentials.UI.UserConsentVerifier]::CheckAvailabilityAsync()) ([Windows.Security.Credentials.UI.UserConsentVerifierAvailability])`)
      helloAvailable = out === 'Available'
    }
    return helloAvailable
  }
  return false
}

async function verifyBiometric(reason) {
  const text = String(reason || 'Desbloquear MoneyOS').replace(/['"`$]/g, '')
  if (process.platform === 'darwin') {
    try {
      await systemPreferences.promptTouchID(text)
      return true
    } catch {
      return false
    }
  }
  if (process.platform === 'win32') {
    if (win) win.focus()
    const out = await powershell(HELLO_PREAMBLE + `Await ([Windows.Security.Credentials.UI.UserConsentVerifier]::RequestVerificationAsync('${text}')) ([Windows.Security.Credentials.UI.UserConsentVerificationResult])`)
    if (win) win.focus()
    return out === 'Verified'
  }
  return false
}

ipcMain.handle('moneyos:bio-available', () => biometricAvailable())
ipcMain.handle('moneyos:bio-verify', (_e, reason) => verifyBiometric(reason))

// ---- Menu (Spanish) ----

function buildMenu() {
  const mac = process.platform === 'darwin'
  const template = [
    ...(mac
      ? [{ label: 'MoneyOS', submenu: [
          { role: 'about', label: 'Acerca de MoneyOS' },
          { type: 'separator' },
          { role: 'hide', label: 'Ocultar MoneyOS' },
          { role: 'hideOthers', label: 'Ocultar otros' },
          { role: 'unhide', label: 'Mostrar todo' },
          { type: 'separator' },
          { role: 'quit', label: 'Salir de MoneyOS' },
        ] }]
      : [{ label: 'Archivo', submenu: [{ role: 'quit', label: 'Salir' }] }]),
    { label: 'Editar', submenu: [
      { role: 'undo', label: 'Deshacer' },
      { role: 'redo', label: 'Rehacer' },
      { type: 'separator' },
      { role: 'cut', label: 'Cortar' },
      { role: 'copy', label: 'Copiar' },
      { role: 'paste', label: 'Pegar' },
      { role: 'selectAll', label: 'Seleccionar todo' },
    ] },
    { label: 'Ver', submenu: [
      { role: 'reload', label: 'Recargar' },
      { type: 'separator' },
      { role: 'resetZoom', label: 'Tamaño real' },
      { role: 'zoomIn', label: 'Acercar' },
      { role: 'zoomOut', label: 'Alejar' },
      { type: 'separator' },
      { role: 'togglefullscreen', label: 'Pantalla completa' },
    ] },
    { label: 'Ventana', submenu: [
      { role: 'minimize', label: 'Minimizar' },
      { role: 'close', label: 'Cerrar' },
    ] },
  ]
  Menu.setApplicationMenu(Menu.buildFromTemplate(template))
}

app.on('second-instance', () => {
  if (win) {
    if (win.isMinimized()) win.restore()
    win.focus()
  }
})

app.whenReady().then(() => {
  buildMenu()
  createWindow()
  powerMonitor.on('lock-screen', requestLock)
  powerMonitor.on('suspend', requestLock)
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
