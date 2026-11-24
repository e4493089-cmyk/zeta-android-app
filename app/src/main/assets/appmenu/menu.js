(() => {
  'use strict';
  if (window.top !== window || location.origin !== 'https://zeta-ai.io') return;
  const ID = 'zeta-app-settings-entry';
  let queued = false;
  function mount() {
    const hub = document.querySelector('[data-sentry-component="ProfileHubHeader"]');
    const old = document.getElementById(ID);
    if (!hub || !hub.parentElement) { old?.remove(); return; }
    if (old && old.previousElementSibling === hub) return;
    old?.remove();
    const link = document.createElement('a');
    link.id = ID;
    link.href = 'https://zeta-ai.io/__zeta_tools_app_settings__';
    link.textContent = '⚙ ZETA Tools 설정  ›';
    link.style.cssText = 'display:block;box-sizing:border-box;width:calc(100% - 32px);margin:12px 16px;padding:14px 16px;border:1px solid #d9d9d9;border-radius:12px;background:#fff;color:#191919;text-decoration:none;font:600 14px system-ui;position:static;';
    hub.insertAdjacentElement('afterend', link);
  }
  function schedule(){ if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;mount();}); }
  new MutationObserver(schedule).observe(document.documentElement,{subtree:true,childList:true});
  window.addEventListener('popstate',schedule);
  schedule();
})();
