// ==UserScript==
// @name         ZETA App Light Theme
// @namespace    zeta-app-light-theme
// @version      2.0.0
// @description  제타 기본 UI를 유지하는 독립 라이트 모드
// @match        https://zeta-ai.io/*
// @updateURL    https://raw.githubusercontent.com/e4493089-cmyk/zeta-android-app/main/scripts/zeta-app-light.user.js
// @downloadURL  https://raw.githubusercontent.com/e4493089-cmyk/zeta-android-app/main/scripts/zeta-app-light.user.js
// @run-at       document-start
// @grant        none
// ==/UserScript==

(() => {
  'use strict';
  if (location.origin !== 'https://zeta-ai.io' || window.top !== window) return;

  const STYLE_ID = 'zeta-app-light-style';
  const CSS = String.raw`
/* 01. 공통 팔레트: 이후 편집기는 이 변수만 변경한다. */
:root {
  --zl-bg: #ffffff;
  --zl-surface: #f5f5f7;
  --zl-control: #eeeef1;
  --zl-text: #191919;
  --zl-secondary: #666666;
  --zl-muted: #73737a;
  --zl-line: #e5e5e8;
  --zl-accent: #6728ff;
  --zl-accent-text: #6728ff;
  --zl-on-accent: #ffffff;
  --zl-overlay: rgba(40,40,43,.84);
  --zl-on-overlay: #ffffff;
}

/* 02. 홈 화면의 공통 바탕과 상단바. 다른 화면은 별도 HTML 확인 후 적용한다. */
html:has(main#contents [data-sentry-component="Home"]),
html:has(main#contents [data-sentry-component="Home"]) > body {
  color-scheme: light;
  background-color: var(--zl-bg) !important;
  color: var(--zl-text) !important;
}
html:has(main#contents [data-sentry-component="Home"]) header[data-sentry-component="Header"] {
  background-color: var(--zl-bg) !important;
  border-color: var(--zl-line) !important;
  color: var(--zl-text) !important;
}
html:has(main#contents [data-sentry-component="Home"]) header[data-sentry-component="Header"] .text-white {
  color: var(--zl-text) !important;
}
html:has(main#contents [data-sentry-component="Home"]) header[data-sentry-component="Header"] .text-white\/50 {
  color: var(--zl-muted) !important;
}

/* 03. 홈 피드: 원래 레이아웃, 이미지, 글자 크기를 유지한다. */
[data-sentry-component="Home"] {
  background-color: var(--zl-bg) !important;
  color: var(--zl-text) !important;
}
[data-sentry-component="Home"] .bg-gray-main {
  background-color: var(--zl-bg) !important;
}
[data-sentry-component="Home"] .bg-gray-sub1 {
  background-color: var(--zl-surface) !important;
}
[data-sentry-component="Home"] .text-white\/50 {
  color: var(--zl-muted) !important;
}
[data-sentry-component="Home"] .text-white\/70 {
  color: var(--zl-secondary) !important;
}
[data-sentry-component="Home"] .text-primary-200 {
  color: var(--zl-accent-text) !important;
}
[data-sentry-component="Home"] .border-white\/\[3\%\] {
  border-color: var(--zl-line) !important;
}

/* 04. 캐릭터 카드: 기존 Tailwind 그라데이션 구조에서 색상만 교체한다. */
[data-sentry-component="FrameProfileCard"] .from-\[rgba\(33\,33\,34\,0\)\] {
  --tw-gradient-from: color-mix(in srgb, var(--zl-surface) 0%, transparent) !important;
}
[data-sentry-component="FrameProfileCard"] .via-\[rgba\(33\,33\,34\,0\.8\)\] {
  --tw-gradient-via: color-mix(in srgb, var(--zl-surface) 80%, transparent) !important;
}
[data-sentry-component="FrameProfileCard"] .to-gray-sub1 {
  --tw-gradient-to: var(--zl-surface) !important;
}
[data-sentry-component="Home"] [data-sentry-component="CountTag"] .bg-gray-800\/\[84\%\],
[data-sentry-component="Home"] .bg-gray-sub1\/\[84\%\] {
  background-color: var(--zl-overlay) !important;
  color: var(--zl-on-overlay) !important;
}
[data-sentry-component="Home"] [data-sentry-component="CountTag"] .text-white\/70 {
  color: var(--zl-on-overlay) !important;
}

/* 05. 피드 탭: 활성 버튼의 보라색과 흰 글자 유지. */
[data-sentry-component="FeedTabNavigator"] .bg-primary-400 {
  background-color: var(--zl-accent) !important;
  color: var(--zl-on-accent) !important;
}

/* 06. 하단 내비게이션: 기존 활성/비활성 클래스에 대응한다. */
html:has(main#contents [data-sentry-component="Home"]) [data-sentry-component="NavigationBar"] {
  background-color: var(--zl-bg) !important;
  border-color: var(--zl-line) !important;
}
html:has(main#contents [data-sentry-component="Home"]) [data-sentry-component="NavigationBar"] .text-gray-200 {
  color: var(--zl-text) !important;
}
html:has(main#contents [data-sentry-component="Home"]) [data-sentry-component="NavigationBar"] .text-gray-700 {
  color: var(--zl-muted) !important;
}
`;

  function install() {
    if (document.getElementById(STYLE_ID)) return;
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = CSS;
    (document.head || document.documentElement).appendChild(style);
  }
  if (document.documentElement) install();
  else document.addEventListener('DOMContentLoaded', install, { once: true });
})();
