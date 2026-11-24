// Bundled code. Only the CSS string is updated remotely.
(() => {
  'use strict';
  if (location.origin !== 'https://zeta-ai.io' || window.top !== window) return;
  const ID = 'zeta-app-light-style';
  let latest = typeof ZETA_BUNDLED_THEME === 'object' ? ZETA_BUNDLED_THEME : null;
  function apply() {
    if (!latest || typeof latest.css !== 'string' || !document.documentElement) return;
    let style = document.getElementById(ID);
    if (!style) {
      style = document.createElement('style');
      style.id = ID;
      style.textContent = latest.css;
      (document.head || document.documentElement).appendChild(style);
    } else if (style.textContent !== latest.css) {
      style.textContent = latest.css;
    }
  }
  apply();
  if (!document.documentElement) document.addEventListener('DOMContentLoaded',apply,{once:true});
  let port;
  function connect() {
    if (port) return;
    try {
      port = browser.runtime.connectNative('zeta_theme');
      port.onMessage.addListener(data => {
        if (!data || typeof data.css !== 'string' || typeof data.version !== 'string') return;
        latest = data;
        apply();
      });
      port.onDisconnect.addListener(() => { port = null; });
      port.postMessage({type:'ready'});
    } catch (_) { port = null; }
  }
  connect();
  window.addEventListener('pageshow',connect);
})();
