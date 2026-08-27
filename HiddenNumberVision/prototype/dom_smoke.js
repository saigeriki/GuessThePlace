const fs = require('fs'), vm = require('vm'), path = require('path');
const dir = '/home/user/GuessThePlace/HiddenNumberVision/prototype';
const core = fs.readFileSync(path.join(dir, 'vision_core.js'), 'utf8');
const html = fs.readFileSync(path.join(dir, 'index.html'), 'utf8');
const inline = html.match(/<script>([\s\S]*?)<\/script>/)[1];

const mkEl = (id) => ({
  id, textContent: '', innerHTML: '', value: '1', style: {}, dataset: {}, children: [],
  width: 760, height: 760, getContext: () => ctx2d,
  addEventListener(t, f) { (this._h ??= {})[t] = f; },
  click() { const h = this._h?.click; if (h) h({ target: this, closest: () => this, preventDefault: () => {} }); },
  setAttribute(k, v) { this[k] = v; },
  closest() { return this; },
});
const els = {};
const sandbox = {
  document: { getElementById: (id) => (els[id] ??= mkEl(id)) },
  console, Math, JSON, Number, String, Array, Object, Set, Map, isNaN, parseInt, parseFloat,
  setInterval: (f, ms) => { sandbox.__int = f; return 1; },
  clearInterval: () => {},
  setTimeout: (f, ms) => { (sandbox.__timeouts ??= []).push(f); return 1; },
  addEventListener: (t, f) => { (sandbox.__win ??= {})[t] = f; },
  requestAnimationFrame: () => 0,
};
sandbox.window = sandbox;
const ctx2d = new Proxy({}, { get: (o, k) => (typeof o[k] === 'undefined' ? () => {} : o[k]), set: () => true });
vm.createContext(sandbox);
vm.runInContext(core.replace(/if \(typeof module[\s\S]*$/, ''), sandbox, { filename: 'vision_core.js' });
vm.runInContext(inline + '\n;globalThis.__state = state; globalThis.__submit = submit; globalThis.__type = type; globalThis.__draw = draw;', sandbox, { filename: 'index.html inline' });
const S = () => sandbox.__state;

let fails = 0;
const step = () => { const q = sandbox.__timeouts || []; sandbox.__timeouts = []; q.forEach((f) => f()); };
const settle = () => { for (let n = 0; n < 8 && (sandbox.__timeouts || []).length; n++) step(); };
const t = (name, fn) => { try { fn(); console.log('  ok  ', name); } catch (e) { fails++; console.log('  FAIL', name, '->', e.message); } };
const fire = (id, ev, arg) => { const h = els[id]?._h?.[ev]; if (!h) throw new Error(`no handler ${id}/${ev}`); h(arg); };

console.log('prototype smoke test (real DOM-less run of index.html logic):');
t('initial plate rendered', () => {
  if (!els.answer.innerHTML.includes('<span>')) throw new Error('answer boxes missing: ' + els.answer.innerHTML);
  if (!els.diag.innerHTML.includes('hue gap')) throw new Error('diagnostics not filled');
  if (!/^[1-9]\d*$/.test(S().puzzle.answer)) throw new Error('bad answer ' + S().puzzle.answer);
});
t('canvas got draw calls', () => { if (!els.plate || !els.plate.width) throw new Error('canvas missing'); });
t('level slider -> new plate', () => { els.level.value = '9'; fire('level', 'input'); if (S().puzzle.digitCount !== 3) throw new Error('L9 should be 3 digits, got ' + S().puzzle.digitCount); });
t('style segmented -> lines', () => {
  const btn = { dataset: { style: 'lines' }, setAttribute: () => {} };
  els.styleSeg._h.click({ target: { closest: () => btn } });
  if (S().style !== 'lines') throw new Error('style not applied');
});
t('typing full answer -> submit -> score up', () => {
  const before = S().score;
  for (const ch of S().puzzle.answer) fire('keys', 'click', { target: { dataset: { k: ch } } });
  step(); // run the deferred submit, but not the "next plate" callback yet
  if (S().score <= before) throw new Error('score did not increase');
  if (!/Correct/.test(els.msg.textContent)) throw new Error('no success banner: ' + els.msg.textContent);
});
t('wrong answer costs a life and reveals', () => {
  settle(); // finish the previous round's transition first
  const lives = S().lives;
  const a = S().puzzle.answer;
  const wrong = a.slice(0, -1) + String((+a[a.length - 1] + 5) % 10); // guaranteed != answer
  for (const ch of wrong) fire('keys', 'click', { target: { dataset: { k: ch } } });
  step();
  if (S().lives !== lives - 1) throw new Error('life not lost');
  if (!S().reveal) throw new Error('reveal flag not set');
  if (!/was/.test(els.msg.textContent)) throw new Error('banner: ' + els.msg.textContent);
});
t('after reveal flush, plate is interactive again', () => { settle(); if (S().locked) throw new Error('still locked'); });
t('hint button spends a hint + redraws', () => {
  const h = S().hints;
  fire('hint', 'click');
  if (S().hints !== h - 1) throw new Error('hint not consumed');
  if (!S().hintOn) throw new Error('hintOn not set');
});
t('reveal + new buttons', () => { fire('reveal', 'click'); fire('again', 'click'); });
t('keyboard input path', () => {
  const k = sandbox.__win.keydown;
  k({ key: S().puzzle.answer[0] });
  k({ key: 'Backspace', preventDefault: () => {} });
  k({ key: 'Enter' });
  k({ key: 'h' }); k({ key: 'n' });
});
t('timer tick decrements + bar updates', () => { settle();
  const before = S().remainMs;
  sandbox.__int();
  if (S().remainMs !== before - 100) throw new Error('tick broken');
});
console.log(fails === 0 ? '\n✅ prototype logic OK' : `\n❌ ${fails} broken`);
process.exit(fails ? 1 : 0);
