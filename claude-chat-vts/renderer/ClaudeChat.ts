import type { MentionItem, ChatSession, ClaudeProbe } from '../shared/chat-types';
import './chat.css';

// ── Internal types ────────────────────────────────────────────────────────────

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  status: 'done' | 'streaming' | 'error';
  toolCalls: ToolCall[];
  error?: string;
  timestamp: string;
}

interface ToolCall {
  id: string;
  name: string;
  summary: string;
  status: 'running' | 'done';
}

interface MsgDom {
  el: HTMLElement;
  bulletEl: HTMLElement;
  textEl: HTMLElement;
  toolsEl: HTMLUListElement;
}

/** DOM node cache — populated in build() */
interface DomRefs {
  bar: HTMLElement;
  barLabel: HTMLSpanElement;
  probeBtn: HTMLButtonElement;
  tabs: HTMLElement;
  newBtn: HTMLButtonElement;
  msgs: HTMLElement;
  composer: HTMLElement;
  frame: HTMLElement;
  mentionPopup: HTMLElement;
  textarea: HTMLTextAreaElement;
  sendBtn: HTMLButtonElement;
}

// Raw events from Claude CLI NDJSON stream
type RawStreamEvent =
  | { type: 'assistant'; message: { content: RawContentPart[] } }
  | { type: 'tool_result'; tool_use_id: string }
  | { type: 'result'; session_id: string }
  | { type: 'done' }
  | { type: 'error'; message: string }
  | { type: string };

type RawContentPart =
  | { type: 'text'; text: string }
  | { type: 'tool_use'; id: string; name: string; input: Record<string, unknown> };

// ── Markdown renderer (no external deps) ─────────────────────────────────────

function renderMarkdown(raw: string): string {
  if (!raw) return '';

  const codeBlocks: string[] = [];
  let text = raw.replace(/```[\w]*\n?([\s\S]*?)```/g, (_, code: string) => {
    const i = codeBlocks.push(`<pre><code>${escHtml(code.trim())}</code></pre>`) - 1;
    return `\x00CB${i}\x00`;
  });

  const inlineCodes: string[] = [];
  text = text.replace(/`([^`\n]+)`/g, (_, code: string) => {
    const i = inlineCodes.push(`<code>${escHtml(code)}</code>`) - 1;
    return `\x00IC${i}\x00`;
  });

  text = escHtml(text);
  text = text.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>');
  text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  text = text.replace(/\*([^*\n]+)\*/g, '<em>$1</em>');
  text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g,
    '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
  text = text.replace(/^### (.+)$/gm, '<h3>$1</h3>');
  text = text.replace(/^## (.+)$/gm,  '<h2>$1</h2>');
  text = text.replace(/^# (.+)$/gm,   '<h1>$1</h1>');
  text = text.replace(/^---+$/gm, '<hr>');
  text = text.replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>');
  text = text.replace(/^[*\-] (.+)$/gm, '<li>$1</li>');
  text = text.replace(/(<li>.*?<\/li>\n?)+/g, m => `<ul>${m}</ul>`);
  text = text.replace(/^\d+\. (.+)$/gm, '<__li>$1</__li>');
  text = text.replace(/(<__li>.*?<\/__li>\n?)+/g, m => `<ol>${m}</ol>`);
  text = text.replace(/<\/?__li>/g, m => m.includes('/') ? '</li>' : '<li>');
  text = text.replace(/\n\n+/g, '</p><p>');
  text = `<p>${text}</p>`;
  text = text.replace(/\n/g, '<br>');
  text = text.replace(/\x00CB(\d+)\x00/g, (_, i) => codeBlocks[+i]);
  text = text.replace(/\x00IC(\d+)\x00/g, (_, i) => inlineCodes[+i]);
  return text;
}

function escHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;')
          .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

// ── <options> tag parser ──────────────────────────────────────────────────────

type MessagePart = { type: 'text'; content: string } | { type: 'options'; items: string[] };

function parseMessageParts(text: string): MessagePart[] {
  const parts: MessagePart[] = [];
  const regex = /<options>([\s\S]*?)<\/options>/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(text)) !== null) {
    const before = text.slice(lastIndex, match.index).trim();
    if (before) parts.push({ type: 'text', content: before });
    const items = match[1].trim().split('\n')
      .map(l => l.replace(/^\d+[.)]\s*/, '').trim()).filter(l => l.length > 0);
    if (items.length) parts.push({ type: 'options', items });
    lastIndex = match.index + match[0].length;
  }
  const after = text.slice(lastIndex).trim();
  if (after) parts.push({ type: 'text', content: after });
  return parts.length ? parts : [{ type: 'text', content: text }];
}

// ── Small DOM/util helpers ────────────────────────────────────────────────────

function el<K extends keyof HTMLElementTagNameMap>(
  tag: K, className?: string
): HTMLElementTagNameMap[K] {
  const e = document.createElement(tag);
  if (className) e.className = className;
  return e;
}

function bulletClass(status: string): string {
  return status === 'streaming' ? 'cld-bullet-live'
       : status === 'error'     ? 'cld-bullet-error'
       : 'cld-bullet-done';
}

let _uid = 0;
function uid(): string { return `m${Date.now()}-${++_uid}`; }

// ── Public options interface ──────────────────────────────────────────────────

export interface ClaudeChatOptions {
  /** Working directory for Claude. Passed to createSession; omit for auto temp dir. */
  cwd?: string;
  mentionItems?: MentionItem[];
  onSessionCreate?: (session: ChatSession) => void;
  /**
   * Called when an option button is clicked.
   * Return `true` to indicate the host handled it (skips Claude re-send).
   */
  onOptionSelect?: (sessionId: string, option: string, index: number) => Promise<boolean>;
  onMentionSelect?: (sessionId: string, item: MentionItem) => void;
  placeholder?: string;
  emptyHint?: string;
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ClaudeChat — vanilla TypeScript class, no React, no Zustand
// ═══════════════════════════════════════════════════════════════════════════════

export class ClaudeChat {

  // ── State ──────────────────────────────────────────────────────────────────
  private probe: ClaudeProbe | null = null;
  private sessions: ChatSession[] = [];
  private activeSessionId: string | null = null;
  private messages = new Map<string, ChatMessage[]>();
  private streaming = false;
  private currentAId: string | null = null;   // current assistant message id

  // @-mention state
  private mentionOpen = false;
  private mentionQuery = '';
  private mentionStart = 0;
  private mentionHl = 0;
  private filteredMentions: MentionItem[] = [];

  // DOM state
  private msgDoms = new Map<string, MsgDom>();
  private ipcCleanups: Array<() => void> = [];
  private dom!: DomRefs;

  // ── Constructor ────────────────────────────────────────────────────────────
  constructor(
    container: string | HTMLElement,
    private opt: ClaudeChatOptions = {}
  ) {
    const root = typeof container === 'string'
      ? (document.querySelector(container) as HTMLElement | null)
      : container;
    if (!root) throw new Error('ClaudeChat: container not found');
    root.className = `${root.className} chat-panel`.trim();
    this.build(root);
    this.runProbe();
  }

  // ── DOM construction ───────────────────────────────────────────────────────
  private build(root: HTMLElement): void {
    // Connection bar
    const bar     = el('div', 'claude-bar');
    const barLabel = el('span', 'claude-bar-label');
    barLabel.textContent = 'Claude 연결 확인 중…';
    const probeBtn = el('button', 'claude-bar-btn');
    probeBtn.type = 'button';
    probeBtn.textContent = '재확인';
    probeBtn.addEventListener('click', () => this.runProbe());
    bar.appendChild(barLabel);
    bar.appendChild(probeBtn);

    // Session tabs
    const tabs = el('div', 'chat-tabs');
    const newBtn = el('button', 'chat-tab-btn chat-tab-btn-active');
    newBtn.type = 'button';
    newBtn.textContent = '+ 새 대화';
    newBtn.addEventListener('click', () => this.setActiveSession(null));
    tabs.appendChild(newBtn);

    // Messages
    const msgs = el('div', 'cld-messages');

    // Composer frame
    const composer = el('div', 'cld-composer');
    const frame    = el('div', 'cld-composer-frame');

    const mentionPopup = el('div', 'cld-mention-popup');
    mentionPopup.style.display = 'none';

    const inputWrap = el('div', 'cld-input-wrap');
    const textarea  = el('textarea', 'cld-composer-input');
    textarea.rows = 3;
    textarea.placeholder = this.opt.placeholder ?? '메시지를 입력하세요 (@항목으로 멘션, Enter: 전송)';
    textarea.addEventListener('input',   () => this.onInput());
    textarea.addEventListener('keydown', e  => this.onKeyDown(e));
    textarea.addEventListener('blur',    () => setTimeout(() => this.closeMention(), 150));
    inputWrap.appendChild(textarea);

    const actions = el('div', 'cld-composer-actions');
    const spacer  = el('div', 'cld-actions-spacer');
    const sendBtn = el('button', 'cld-send-btn');
    sendBtn.type = 'button';
    sendBtn.textContent = '↑';
    sendBtn.disabled = true;
    sendBtn.addEventListener('click', () => this.onSend());
    actions.appendChild(spacer);
    actions.appendChild(sendBtn);

    frame.appendChild(mentionPopup);
    frame.appendChild(inputWrap);
    frame.appendChild(actions);
    composer.appendChild(frame);

    root.appendChild(bar);
    root.appendChild(tabs);
    root.appendChild(msgs);
    root.appendChild(composer);

    this.dom = { bar, barLabel, probeBtn, tabs, newBtn, msgs, composer, frame,
                 mentionPopup, textarea, sendBtn };
    this.renderHint();
  }

  // ── Probe ──────────────────────────────────────────────────────────────────
  private async runProbe(): Promise<void> {
    this.setBarState('checking', '확인 중…');
    this.dom.probeBtn.disabled = true;
    try {
      const p = await window.claudeChat.probe();
      this.probe = p;
      this.dom.probeBtn.disabled = false;
      p.authenticated
        ? this.setBarState('ok', `✓ Claude ${p.version ?? ''}`)
        : this.setBarState('error', `✗ ${p.error ?? '연결 실패'}`);
    } catch {
      this.dom.probeBtn.disabled = false;
      this.setBarState('error', '✗ 연결 실패');
    }
  }

  private setBarState(state: 'checking' | 'ok' | 'error', label: string): void {
    this.dom.bar.className = 'claude-bar'
      + (state === 'ok'    ? ' claude-bar-ok'    : '')
      + (state === 'error' ? ' claude-bar-error'  : '');
    this.dom.barLabel.textContent = label;
  }

  // ── Sessions ───────────────────────────────────────────────────────────────
  private setActiveSession(id: string | null): void {
    this.activeSessionId = id;
    this.unsubscribeAll();
    if (id) this.subscribeSession(id);
    this.renderTabs();
    this.renderMessages();
  }

  private renderTabs(): void {
    const { tabs, newBtn } = this.dom;
    while (tabs.children.length > 1) tabs.removeChild(tabs.lastChild!);
    newBtn.className = 'chat-tab-btn'
      + (this.activeSessionId === null ? ' chat-tab-btn-active' : '');

    [...this.sessions].reverse().slice(0, 10).forEach(s => {
      const btn = el('button',
        'chat-tab-btn chat-tab-btn-job'
        + (s.id === this.activeSessionId ? ' chat-tab-btn-active' : ''));
      btn.type = 'button';
      btn.textContent = (s.label || s.id).slice(0, 20);
      btn.title = s.label || s.id;
      btn.addEventListener('click', () => this.setActiveSession(s.id));
      tabs.appendChild(btn);
    });
  }

  private async createSession(): Promise<ChatSession> {
    const label = `세션 ${new Date().toLocaleTimeString('ko-KR')}`;
    const session = await window.claudeChat.createSession({
      cwd: this.opt.cwd ?? undefined,
      label,
    });
    this.sessions.push(session);
    this.messages.set(session.id, []);
    this.setActiveSession(session.id);
    this.opt.onSessionCreate?.(session);
    return session;
  }

  // ── IPC subscriptions ──────────────────────────────────────────────────────
  private subscribeSession(sessionId: string): void {
    const unsubStream = window.claudeChat.onStream(sessionId, (raw) => {
      const ev = raw as RawStreamEvent;
      if (ev.type !== 'assistant') return;
      const ae = ev as Extract<RawStreamEvent, { type: 'assistant' }>;
      for (const part of ae.message.content) {
        if (!this.currentAId) return; // safety: message must be set before stream arrives
        if (part.type === 'text' && part.text) {
          this.appendText(this.currentAId, part.text);
        } else if (part.type === 'tool_use') {
          this.addToolCall(this.currentAId, part);
        }
      }
    });

    const unsubToolResult = window.claudeChat.onStream(sessionId, (raw) => {
      const ev = raw as RawStreamEvent;
      if (ev.type !== 'tool_result') return;
      const te = ev as Extract<RawStreamEvent, { type: 'tool_result' }>;
      if (this.currentAId) this.updateToolCall(te.tool_use_id, 'done');
    });

    const unsubDone = window.claudeChat.onDone(sessionId, () => {
      if (this.currentAId) this.finalizeMsg(this.currentAId, 'done');
      this.currentAId = null;
      this.setStreaming(false);
    });

    const unsubError = window.claudeChat.onError(sessionId, (err) => {
      if (this.currentAId) this.finalizeMsg(this.currentAId, 'error', err.message);
      this.currentAId = null;
      this.setStreaming(false);
    });

    this.ipcCleanups = [unsubStream, unsubToolResult, unsubDone, unsubError];
  }

  private unsubscribeAll(): void {
    this.ipcCleanups.forEach(fn => fn());
    this.ipcCleanups = [];
  }

  // ── Message rendering ──────────────────────────────────────────────────────
  private renderHint(): void {
    const hint = el('p', 'chat-empty-hint');
    hint.textContent = this.opt.emptyHint ?? 'Claude에게 질문해보세요';
    this.dom.msgs.appendChild(hint);
  }

  private renderMessages(): void {
    this.dom.msgs.innerHTML = '';
    this.msgDoms.clear();
    const list = this.activeSessionId
      ? (this.messages.get(this.activeSessionId) ?? []) : [];
    if (list.length === 0) {
      this.renderHint();
    } else {
      list.forEach(m => this.dom.msgs.appendChild(this.buildMsgEl(m)));
      this.scrollBottom();
    }
  }

  private buildMsgEl(msg: ChatMessage): HTMLElement {
    if (msg.role === 'user') {
      const art    = el('article', 'cld-msg cld-msg-user');
      art.id       = `msg-${msg.id}`;
      const bubble = el('div', 'cld-bubble');
      const txt    = el('div', 'cld-msg-text');
      txt.textContent = msg.text;
      bubble.appendChild(txt);
      art.appendChild(bubble);
      return art;
    }

    const art     = el('article', `cld-msg cld-msg-assistant cld-msg-${msg.status}`);
    art.id        = `msg-${msg.id}`;
    const turn    = el('div', 'cld-turn');
    const bullet  = el('span', `cld-bullet ${bulletClass(msg.status)}`);
    const textDiv = el('div', 'cld-msg-text cld-md');

    if (msg.status === 'streaming' && !msg.text) {
      const t = el('span', 'cld-thinking');
      t.textContent = '응답 생성 중…';
      textDiv.appendChild(t);
    } else if (msg.text) {
      if (msg.status === 'streaming') {
        // plain text during streaming (avoids broken markdown mid-response)
        textDiv.style.whiteSpace = 'pre-wrap';
        textDiv.textContent = msg.text;
      } else {
        this.renderTextWithOptions(textDiv, msg);
      }
    }

    turn.appendChild(bullet);
    turn.appendChild(textDiv);
    art.appendChild(turn);

    const toolsEl = el('ul', 'cld-tool-calls');
    msg.toolCalls.forEach(tc => toolsEl.appendChild(this.buildToolEl(tc)));
    if (msg.toolCalls.length) art.appendChild(toolsEl);

    if (msg.status === 'error' && msg.error) {
      const errDiv = el('div', 'cld-msg-error');
      errDiv.textContent = `⚠ ${msg.error}`;
      art.appendChild(errDiv);
    }

    this.msgDoms.set(msg.id, { el: art, bulletEl: bullet, textEl: textDiv, toolsEl });
    return art;
  }

  private renderTextWithOptions(textDiv: HTMLElement, msg: ChatMessage): void {
    textDiv.style.whiteSpace = '';
    textDiv.innerHTML = '';
    parseMessageParts(msg.text).forEach(part => {
      if (part.type === 'text') {
        const div = el('div');
        div.innerHTML = renderMarkdown(part.content);
        textDiv.appendChild(div);
      } else {
        const optDiv = el('div', 'cld-options');
        part.items.forEach((item, ii) => {
          const btn = el('button', 'cld-option-btn');
          btn.type = 'button';
          btn.textContent = item;
          btn.disabled = this.streaming;
          btn.addEventListener('click', () => this.onOptionClick(item, ii));
          optDiv.appendChild(btn);
        });
        textDiv.appendChild(optDiv);
      }
    });
  }

  private buildToolEl(tc: ToolCall): HTMLLIElement {
    const li = el('li', `cld-tool cld-tool-${tc.status}`);
    li.id = `tool-${tc.id}`;
    li.innerHTML =
      `<span class="cld-bullet ${tc.status === 'done' ? 'cld-bullet-done' : 'cld-bullet-live'}"></span>`
      + `<span class="cld-tool-name">${escHtml(tc.name)}</span>`
      + `<span class="cld-tool-summary">${escHtml(tc.summary)}</span>`;
    return li;
  }

  // ── Streaming state mutators ───────────────────────────────────────────────
  private addMsg(sessionId: string, msg: ChatMessage): void {
    let list = this.messages.get(sessionId);
    if (!list) { list = []; this.messages.set(sessionId, list); }
    list.push(msg);
    if (sessionId !== this.activeSessionId) return;

    const hint = this.dom.msgs.querySelector('.chat-empty-hint');
    hint?.remove();
    this.dom.msgs.appendChild(this.buildMsgEl(msg));
    this.scrollBottom();
  }

  private ensureAssistant(msgId: string): void {
    if (!this.activeSessionId) return;
    this.addMsg(this.activeSessionId, {
      id: msgId, role: 'assistant', text: '', status: 'streaming',
      toolCalls: [], timestamp: new Date().toISOString(),
    });
  }

  private appendText(msgId: string, text: string): void {
    const list = this.messages.get(this.activeSessionId ?? '');
    const msg  = list?.find(m => m.id === msgId);
    if (!msg) return;
    msg.text += text;

    const dom = this.msgDoms.get(msgId);
    if (dom) {
      dom.textEl.style.whiteSpace = 'pre-wrap';
      dom.textEl.textContent = msg.text;
      this.scrollBottom();
    }
  }

  private addToolCall(
    msgId: string,
    tc: { id: string; name: string; input: Record<string, unknown> }
  ): void {
    const list = this.messages.get(this.activeSessionId ?? '');
    const msg  = list?.find(m => m.id === msgId);
    if (!msg) return;
    const tool: ToolCall = {
      id: tc.id, name: tc.name,
      summary: JSON.stringify(tc.input).slice(0, 120),
      status: 'running',
    };
    msg.toolCalls.push(tool);

    const dom = this.msgDoms.get(msgId);
    if (dom) {
      if (!dom.el.contains(dom.toolsEl)) dom.el.appendChild(dom.toolsEl);
      dom.toolsEl.appendChild(this.buildToolEl(tool));
      this.scrollBottom();
    }
  }

  private updateToolCall(toolId: string, status: ToolCall['status']): void {
    const list = this.messages.get(this.activeSessionId ?? '');
    list?.forEach(m => {
      const tc = m.toolCalls.find(c => c.id === toolId);
      if (tc) tc.status = status;
    });
    const toolEl = document.getElementById(`tool-${toolId}`);
    if (toolEl) {
      toolEl.className = `cld-tool cld-tool-${status}`;
      const b = toolEl.querySelector('.cld-bullet');
      if (b) b.className = `cld-bullet ${status === 'done' ? 'cld-bullet-done' : 'cld-bullet-live'}`;
    }
  }

  private finalizeMsg(msgId: string, status: 'done' | 'error', error?: string): void {
    const list = this.messages.get(this.activeSessionId ?? '');
    const msg  = list?.find(m => m.id === msgId);
    if (!msg) return;
    msg.status = status;
    msg.error  = error;
    msg.toolCalls.forEach(tc => { if (tc.status === 'running') tc.status = 'done'; });

    const dom = this.msgDoms.get(msgId);
    if (!dom) return;

    dom.el.className     = `cld-msg cld-msg-assistant cld-msg-${status}`;
    dom.bulletEl.className = `cld-bullet ${bulletClass(status)}`;
    this.renderTextWithOptions(dom.textEl, msg);

    msg.toolCalls.forEach(tc => {
      const toolEl = document.getElementById(`tool-${tc.id}`);
      if (toolEl) {
        toolEl.className = 'cld-tool cld-tool-done';
        const b = toolEl.querySelector('.cld-bullet');
        if (b) b.className = 'cld-bullet cld-bullet-done';
      }
    });

    if (error) {
      const errDiv = dom.el.querySelector('.cld-msg-error') ?? el('div', 'cld-msg-error');
      errDiv.textContent = `⚠ ${error}`;
      if (!dom.el.contains(errDiv)) dom.el.appendChild(errDiv);
    }
  }

  private setStreaming(on: boolean): void {
    this.streaming = on;
    const { sendBtn, textarea, composer } = this.dom;
    if (on) {
      sendBtn.className   = 'cld-send-btn cld-send-stop';
      sendBtn.textContent  = '■';
      sendBtn.disabled     = false;
      textarea.disabled    = true;
      textarea.placeholder = 'Claude가 응답 중입니다…';
      composer.className   = 'cld-composer cld-composer-streaming';
    } else {
      sendBtn.className   = 'cld-send-btn';
      sendBtn.textContent  = '↑';
      sendBtn.disabled     = !textarea.value.trim();
      textarea.disabled    = false;
      textarea.placeholder = this.opt.placeholder ?? '메시지를 입력하세요…';
      composer.className   = 'cld-composer';
      this.currentAId      = null;
      // Re-enable option buttons
      this.dom.msgs.querySelectorAll<HTMLButtonElement>('.cld-option-btn')
        .forEach(b => { b.disabled = false; });
    }
  }

  private scrollBottom(): void {
    this.dom.msgs.scrollTop = this.dom.msgs.scrollHeight;
  }

  // ── Send ───────────────────────────────────────────────────────────────────
  private async onSend(): Promise<void> {
    if (this.streaming) { this.doAbort(); return; }
    const text = this.dom.textarea.value.trim();
    if (!text) return;

    this.closeMention();
    this.dom.textarea.value = '';
    this.dom.sendBtn.disabled = true;

    let sessionId = this.activeSessionId;
    if (!sessionId) {
      try {
        const s = await this.createSession();
        sessionId = s.id;
      } catch (e) {
        console.error('ClaudeChat: session creation failed', e);
        return;
      }
    }

    this.addMsg(sessionId, {
      id: uid(), role: 'user', text,
      status: 'done', toolCalls: [], timestamp: new Date().toISOString(),
    });

    // Reserve assistant message slot BEFORE triggering IPC
    const aId = uid();
    this.currentAId = aId;
    this.ensureAssistant(aId);
    this.setStreaming(true);

    window.claudeChat.sendMessage(sessionId, text).catch(() => {});
  }

  private doAbort(): void {
    if (this.activeSessionId) window.claudeChat.abort(this.activeSessionId);
  }

  // ── Option buttons ─────────────────────────────────────────────────────────
  private async onOptionClick(option: string, index: number): Promise<void> {
    if (this.streaming || !this.activeSessionId) return;
    const sessionId = this.activeSessionId;

    if (this.opt.onOptionSelect) {
      const handled = await this.opt.onOptionSelect(sessionId, option, index);
      if (handled) return;
    }

    this.addMsg(sessionId, {
      id: uid(), role: 'user', text: option,
      status: 'done', toolCalls: [], timestamp: new Date().toISOString(),
    });

    const aId = uid();
    this.currentAId = aId;
    this.ensureAssistant(aId);
    this.setStreaming(true);
    window.claudeChat.sendMessage(sessionId, option).catch(() => {});
  }

  // ── @-mention ──────────────────────────────────────────────────────────────
  private onInput(): void {
    const { textarea, sendBtn } = this.dom;
    const value  = textarea.value;
    const cursor = textarea.selectionStart ?? value.length;
    const before = value.slice(0, cursor);

    sendBtn.disabled = !value.trim() || this.streaming;

    const atMatch = before.match(/@([^\s@]*)$/);
    if (atMatch) {
      this.mentionQuery = atMatch[1];
      this.mentionStart = cursor - atMatch[0].length;
      this.openMention();
    } else {
      this.closeMention();
    }
  }

  private openMention(): void {
    const q = this.mentionQuery.toLowerCase();
    this.filteredMentions = (this.opt.mentionItems ?? [])
      .filter(item => item.name.toLowerCase().includes(q));

    if (!this.filteredMentions.length) { this.closeMention(); return; }

    const popup = this.dom.mentionPopup;
    popup.innerHTML = '';
    this.mentionHl   = 0;
    this.mentionOpen = true;

    this.filteredMentions.forEach((item, i) => {
      const btn = el('button',
        `cld-mention-item${i === 0 ? ' cld-mention-item-active' : ''}`);
      btn.type = 'button';
      btn.innerHTML =
        `<span class="cld-mention-name">@${escHtml(item.name)}</span>`
        + `<span class="cld-mention-type">${escHtml(item.type)}</span>`;
      btn.addEventListener('mousedown', e => { e.preventDefault(); this.selectMention(item); });
      btn.addEventListener('mouseenter', () => {
        this.mentionHl = i;
        this.updateMentionHl();
      });
      popup.appendChild(btn);
    });

    popup.style.display = 'block';
  }

  private closeMention(): void {
    this.mentionOpen = false;
    this.dom.mentionPopup.style.display = 'none';
  }

  private selectMention(item: MentionItem): void {
    const { textarea } = this.dom;
    const input  = textarea.value;
    const after  = input.slice(this.mentionStart + 1 + this.mentionQuery.length);
    const newVal = input.slice(0, this.mentionStart) + `@${item.name} ` + after;
    textarea.value = newVal;
    this.dom.sendBtn.disabled = !newVal.trim();
    this.closeMention();
    if (this.activeSessionId) this.opt.onMentionSelect?.(this.activeSessionId, item);
    const pos = this.mentionStart + item.name.length + 2;
    requestAnimationFrame(() => {
      textarea.focus();
      textarea.setSelectionRange(pos, pos);
    });
  }

  private updateMentionHl(): void {
    this.dom.mentionPopup
      .querySelectorAll<HTMLElement>('.cld-mention-item')
      .forEach((b, i) => {
        b.className = `cld-mention-item${i === this.mentionHl ? ' cld-mention-item-active' : ''}`;
      });
  }

  private onKeyDown(e: KeyboardEvent): void {
    if (this.mentionOpen && this.filteredMentions.length) {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        this.mentionHl = Math.min(this.mentionHl + 1, this.filteredMentions.length - 1);
        this.updateMentionHl(); return;
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        this.mentionHl = Math.max(this.mentionHl - 1, 0);
        this.updateMentionHl(); return;
      }
      if (e.key === 'Enter' && !e.isComposing) {
        e.preventDefault();
        const item = this.filteredMentions[this.mentionHl];
        if (item) this.selectMention(item);
        return;
      }
      if (e.key === 'Escape') { e.preventDefault(); this.closeMention(); return; }
    }
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
      e.preventDefault();
      this.onSend();
    }
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  /** Remove all IPC listeners. Call when the widget is removed from the DOM. */
  public destroy(): void {
    this.unsubscribeAll();
  }
}
