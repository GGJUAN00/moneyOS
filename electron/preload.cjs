// Safe bridge between the desktop shell and the web app (no Node access in the page).
const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('moneyosDesktop', {
  platform: process.platform, // 'darwin' | 'win32' | 'linux'
  biometricAvailable: () => ipcRenderer.invoke('moneyos:bio-available'),
  verifyBiometric: reason => ipcRenderer.invoke('moneyos:bio-verify', reason),
  onLockRequest: callback => {
    const listener = () => callback()
    ipcRenderer.on('moneyos:lock', listener)
    return () => ipcRenderer.removeListener('moneyos:lock', listener)
  },
})
