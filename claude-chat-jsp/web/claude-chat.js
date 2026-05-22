/**
 * claude-chat.js — Vanilla JS Claude Code chat widget for JSP/Servlet projects.
 * No React, no bundler, no external runtime dependencies required.
 *
 * Usage:
 *   const chat = new ClaudeChat('#container', { apiBase: '/api/claude-chat', ... });
 */
(function (global) {
  'use strict';

  // ── Minimal markdown renderer ────────────────────────────────────────────────
  function renderMarkdown(raw) {
    if (!raw) return '';

    // Protect code blocks from other replacements
    const codeBlocks = [];
    let text = raw.replace(/```([\w]*)\n?([\s\S]*?)```/g, function (_, lang, code) {
      var i = codeBlocks.length;
      codeBlocks.push('<pre><code>' + escHtml(code.trim()) + '</code></pre>');
      return '\x00CB' + i + '\x00';
    });

    const inlineCodes = [];
    text = text.replace(/`([^`\n]+)`/g, function (_, code) {
      var i = inlineCodes.length;
      inlineCodes.push('<code>' + escHtml(code) + '</code>');
      return '\x00IC' + i + '\x00';
    });

    // Escape HTML in remaining text
    text = escHtml(text);

    // Bold + italic
    text = text.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>');
    text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    text = text.replace(/\*([^*\n]+)\*/g, '<em>$1</em>');

    // Links
    text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');

    // Headings
    text = text.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    text = text.replace(/^## (.+)$/gm,  '<h2>$1</h2>');
    text = text.replace(/^# (.+)$/gm,   '<h1>$1</h1>');

    // Horizontal rule
    text = text.replace(/^---+$/gm, '<hr>');

    // Blockquote (after HTML escape, > becomes &gt;)
    text = text.replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>');

    // Unordered list items
    text = text.replace(/^[*\-] (.+)$/gm, '<li>$1</li>');
    text = text.replace(/(<li>.*?<\/li>\n?)+/g, '<ul>$&</ul>');

    // Ordered list items
    text = text.replace(/^\d+\. (.+)$/gm, '<__oli>$1</__oli>');
    text = text.replace(/(<__oli>.*?<\/__oli>\n?)+/g, '<ol>$&</ol>');
    text = text.replace(/<\/?__oli>/g, function (m) {
      return m.includes('/') ? '</li>' : '<li>';
    });

    // Paragraphs (double newline)
    text = text.replace(/\n\n+/g, '</p><p>');
    text = '<p>' + text + '</p>';

    // Single line breaks
    text = text.replace(/\n/g, '<br>');

    // Restore protected blocks
    text = text.replace(/\x00CB(\d+)\x00/g, function (_, i) { return codeBlocks[+i]; });
    text = text.replace(/\x00IC(\d+)\x00/g, function (_, i) { return inlineCodes[+i]; });

    return text;
  }

  function escHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // ── <options> tag parser (same as Electron version) ─────────────────────────
  function parseMessageParts(text) {
    var parts = [];
    var regex = /<options>([\s\S]*?)<\/options>/g;
    var lastIndex = 0, match;
    while ((match = regex.exec(text)) !== null) {
      var before = text.slice(lastIndex, match.index).trim();
      if (before) parts.push({ type: 'text', content: before });
      var items = match[1].trim().split('\n')
        .map(function (l) { return l.replace(/^\d+[.)]\s*/, '').trim(); })
        .filter(function (l) { return l.length > 0; });
      if (items.length) parts.push({ type: 'options', items: items });
      lastIndex = match.index + match[0].length;
    }
    var after = text.slice(lastIndex).trim();
    if (after) parts.push({ type: 'text', content: after });
    return parts.length ? parts : [{ type: 'text', content: text }];
  }

  // ── Unique ID helper ─────────────────────────────────────────────────────────
  var _idCounter = 0;
  function uid() { return 'cld-' + Date.now() + '-' + (++_idCounter); }

  // ═══════════════════════════════════════════════════════════════════════════
  //  ClaudeChat class
  // ═══════════════════════════════════════════════════════════════════════════
  function ClaudeChat(container, options) {
    options = options || {};
    this._el = typeof container === 'string'
      ? document.querySelector(container) : container;
    if (!this._el) throw new Error('ClaudeChat: container element not found');

    this._opt = {
      apiBase:       options.apiBase       || '/api/claude-chat',
      mentionItems:  options.mentionItems  || [],
      onOptionSelect:  options.onOptionSelect  || null,
      onMentionSelect: options.onMentionSelect || null,
      onSessionCreate: options.onSessionCreate || null,
      placeholder: options.placeholder || '메시지를 입력하세요 (@항목으로 멘션, Enter: 전송)',
      emptyHint:   options.emptyHint   || 'Claude에게 질문하거나 작업을 요청해보세요',
    };

    // Internal state
    this._probe            = null;
    this._sessions         = [];          // ClaudeSession[]
    this._activeSessionId  = null;
    this._messages         = {};          // { sessionId: message[] }
    this._streaming        = false;
    this._abortCtrl        = null;
    this._currentAId       = null;        // current assistant message id
    this._mentionOpen      = false;
    this._mentionQuery     = '';
    this._mentionStart     = 0;
    this._mentionHl        = 0;
    this._filteredMentions = [];
    this._msgDoms          = {};          // { msgId: { el, bulletEl, textEl, toolsEl } }

    this._dom = {};
    this._build();
    this._runProbe();
  }

  // ── DOM construction ─────────────────────────────────────────────────────────
  ClaudeChat.prototype._build = function () {
    var self = this;
    this._el.innerHTML = '';
    this._el.className = (this._el.className + ' chat-panel').trim();

    // ─ Connection bar
    var bar = el('div', 'claude-bar');
    var barLabel = el('span', 'claude-bar-label');
    barLabel.textContent = 'Claude 연결 확인 중…';
    var probeBtn = el('button', 'claude-bar-btn');
    probeBtn.type = 'button';
    probeBtn.textContent = '재확인';
    probeBtn.addEventListener('click', function () { self._runProbe(); });
    bar.appendChild(barLabel);
    bar.appendChild(probeBtn);
    this._dom.bar = bar;
    this._dom.barLabel = barLabel;
    this._dom.probeBtn = probeBtn;

    // ─ Session tabs
    var tabs = el('div', 'chat-tabs');
    var newBtn = el('button', 'chat-tab-btn chat-tab-btn-active');
    newBtn.type = 'button';
    newBtn.textContent = '+ 새 대화';
    newBtn.addEventListener('click', function () { self._newSession(); });
    tabs.appendChild(newBtn);
    this._dom.tabs = tabs;
    this._dom.newBtn = newBtn;

    // ─ Messages list
    var msgs = el('div', 'cld-messages');
    this._dom.msgs = msgs;
    this._renderHint();

    // ─ Composer
    var composer = el('div', 'cld-composer');
    var frame = el('div', 'cld-composer-frame');
    this._dom.composer = composer;
    this._dom.frame = frame;

    // @-mention popup
    var popup = el('div', 'cld-mention-popup');
    popup.style.display = 'none';
    this._dom.mentionPopup = popup;

    var inputWrap = el('div', 'cld-input-wrap');
    var textarea = el('textarea', 'cld-composer-input');
    textarea.rows = 3;
    textarea.placeholder = this._opt.placeholder;
    textarea.addEventListener('input',   function (e) { self._onInput(e); });
    textarea.addEventListener('keydown', function (e) { self._onKeyDown(e); });
    textarea.addEventListener('blur',    function ()  { setTimeout(function () { self._closeMention(); }, 150); });
    inputWrap.appendChild(textarea);
    this._dom.textarea = textarea;

    var actions = el('div', 'cld-composer-actions');
    var spacer  = el('div', 'cld-actions-spacer');
    var sendBtn = el('button', 'cld-send-btn');
    sendBtn.type = 'button';
    sendBtn.textContent = '↑';
    sendBtn.disabled = true;
    sendBtn.addEventListener('click', function () { self._onSend(); });
    actions.appendChild(spacer);
    actions.appendChild(sendBtn);
    this._dom.sendBtn = sendBtn;

    frame.appendChild(popup);
    frame.appendChild(inputWrap);
    frame.appendChild(actions);
    composer.appendChild(frame);

    this._el.appendChild(bar);
    this._el.appendChild(tabs);
    this._el.appendChild(msgs);
    this._el.appendChild(composer);
  };

  // ── Probe ────────────────────────────────────────────────────────────────────
  ClaudeChat.prototype._runProbe = function () {
    var self = this;
    this._setBarState('checking', '확인 중…');
    this._dom.probeBtn.disabled = true;
    fetch(this._opt.apiBase + '/probe')
      .then(function (r) { return r.json(); })
      .then(function (p) {
        self._probe = p;
        self._dom.probeBtn.disabled = false;
        if (p.authenticated) {
          self._setBarState('ok', '✓ Claude ' + (p.version || ''));
        } else {
          self._setBarState('error', '✗ ' + (p.error || '연결 실패'));
        }
      })
      .catch(function () {
        self._dom.probeBtn.disabled = false;
        self._setBarState('error', '✗ API 연결 실패');
      });
  };

  ClaudeChat.prototype._setBarState = function (state, label) {
    var bar = this._dom.bar;
    bar.className = 'claude-bar'
      + (state === 'ok'    ? ' claude-bar-ok'    : '')
      + (state === 'error' ? ' claude-bar-error'  : '');
    this._dom.barLabel.textContent = label;
  };

  // ── Sessions ─────────────────────────────────────────────────────────────────
  ClaudeChat.prototype._newSession = function () {
    this._activeSessionId = null;
    this._renderTabs();
    this._renderMessages();
  };

  ClaudeChat.prototype._renderTabs = function () {
    var self = this;
    var tabs = this._dom.tabs;
    while (tabs.children.length > 1) tabs.removeChild(tabs.lastChild);
    this._dom.newBtn.className = 'chat-tab-btn'
      + (this._activeSessionId === null ? ' chat-tab-btn-active' : '');

    var recent = this._sessions.slice().reverse().slice(0, 10);
    recent.forEach(function (s) {
      var btn = el('button', 'chat-tab-btn chat-tab-btn-job'
        + (s.id === self._activeSessionId ? ' chat-tab-btn-active' : ''));
      btn.type = 'button';
      btn.textContent = (s.label || s.id).slice(0, 20);
      btn.title = s.label || s.id;
      btn.addEventListener('click', function () {
        self._activeSessionId = s.id;
        self._renderTabs();
        self._renderMessages();
      });
      tabs.appendChild(btn);
    });
  };

  ClaudeChat.prototype._createSession = function () {
    var self = this;
    var label = '세션 ' + new Date().toLocaleTimeString('ko-KR');
    return fetch(this._opt.apiBase + '/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
      body: JSON.stringify({ label: label }),
    })
      .then(function (r) { return r.json(); })
      .then(function (session) {
        self._sessions.push(session);
        self._activeSessionId = session.id;
        if (!self._messages[session.id]) self._messages[session.id] = [];
        self._renderTabs();
        if (self._opt.onSessionCreate) self._opt.onSessionCreate(session);
        return session;
      });
  };

  // ── Message rendering ─────────────────────────────────────────────────────────
  ClaudeChat.prototype._renderHint = function () {
    var hint = el('p', 'chat-empty-hint');
    hint.textContent = this._opt.emptyHint;
    this._dom.msgs.appendChild(hint);
  };

  ClaudeChat.prototype._renderMessages = function () {
    var self = this;
    var msgs = this._dom.msgs;
    msgs.innerHTML = '';
    this._msgDoms = {};

    var list = this._activeSessionId
      ? (this._messages[this._activeSessionId] || []) : [];

    if (list.length === 0) {
      this._renderHint();
    } else {
      list.forEach(function (m) { msgs.appendChild(self._buildMsgEl(m)); });
      msgs.scrollTop = msgs.scrollHeight;
    }
  };

  ClaudeChat.prototype._buildMsgEl = function (msg) {
    var self = this;
    if (msg.role === 'user') {
      var art = el('article', 'cld-msg cld-msg-user');
      art.id = 'msg-' + msg.id;
      var bubble = el('div', 'cld-bubble');
      var txt = el('div', 'cld-msg-text');
      txt.textContent = msg.text;
      bubble.appendChild(txt);
      art.appendChild(bubble);
      return art;
    }

    // Assistant
    var art = el('article', 'cld-msg cld-msg-assistant cld-msg-' + msg.status);
    art.id = 'msg-' + msg.id;

    var turn = el('div', 'cld-turn');
    var bullet = el('span', 'cld-bullet ' + bulletClass(msg.status));
    var textDiv = el('div', 'cld-msg-text cld-md');

    if (msg.status === 'streaming' && !msg.text) {
      var thinking = el('span', 'cld-thinking');
      thinking.textContent = '응답 생성 중…';
      textDiv.appendChild(thinking);
    } else if (msg.text) {
      // During streaming: plain text; after done: markdown with options
      if (msg.status === 'streaming') {
        textDiv.style.whiteSpace = 'pre-wrap';
        textDiv.textContent = msg.text;
      } else {
        self._renderTextWithOptions(textDiv, msg);
      }
    }

    turn.appendChild(bullet);
    turn.appendChild(textDiv);
    art.appendChild(turn);

    var toolsEl = el('ul', 'cld-tool-calls');
    (msg.toolCalls || []).forEach(function (tc) {
      toolsEl.appendChild(self._buildToolEl(tc));
    });
    if (msg.toolCalls && msg.toolCalls.length) art.appendChild(toolsEl);

    if (msg.status === 'error' && msg.error) {
      var errDiv = el('div', 'cld-msg-error');
      errDiv.textContent = '⚠ ' + msg.error;
      art.appendChild(errDiv);
    }

    this._msgDoms[msg.id] = { el: art, bulletEl: bullet, textEl: textDiv, toolsEl: toolsEl };
    return art;
  };

  ClaudeChat.prototype._renderTextWithOptions = function (textDiv, msg) {
    var self = this;
    textDiv.style.whiteSpace = '';
    textDiv.innerHTML = '';
    var parts = parseMessageParts(msg.text || '');
    parts.forEach(function (part) {
      if (part.type === 'text') {
        var div = el('div');
        div.innerHTML = renderMarkdown(part.content);
        textDiv.appendChild(div);
      } else if (part.type === 'options') {
        var optDiv = el('div', 'cld-options');
        part.items.forEach(function (item, ii) {
          var btn = el('button', 'cld-option-btn');
          btn.type = 'button';
          btn.textContent = item;
          btn.disabled = self._streaming;
          btn.addEventListener('click', function () { self._onOptionClick(item, ii); });
          optDiv.appendChild(btn);
        });
        textDiv.appendChild(optDiv);
      }
    });
  };

  ClaudeChat.prototype._buildToolEl = function (tc) {
    var li = el('li', 'cld-tool cld-tool-' + tc.status);
    li.id = 'tool-' + tc.id;
    li.innerHTML =
      '<span class="cld-bullet ' + (tc.status === 'done' ? 'cld-bullet-done' : 'cld-bullet-live') + '"></span>'
      + '<span class="cld-tool-name">' + escHtml(tc.name) + '</span>'
      + '<span class="cld-tool-summary">' + escHtml(tc.summary) + '</span>';
    return li;
  };

  // ── Streaming helpers ─────────────────────────────────────────────────────────
  ClaudeChat.prototype._addMsg = function (sessionId, msg) {
    if (!this._messages[sessionId]) this._messages[sessionId] = [];
    this._messages[sessionId].push(msg);
    if (sessionId !== this._activeSessionId) return;

    var hint = this._dom.msgs.querySelector('.chat-empty-hint');
    if (hint) hint.remove();

    var el_ = this._buildMsgEl(msg);
    this._dom.msgs.appendChild(el_);
    this._dom.msgs.scrollTop = this._dom.msgs.scrollHeight;
  };

  ClaudeChat.prototype._ensureAssistant = function (msgId) {
    this._addMsg(this._activeSessionId, {
      id: msgId, role: 'assistant', text: '', status: 'streaming', toolCalls: [], error: null
    });
  };

  ClaudeChat.prototype._appendText = function (msgId, text) {
    var list = this._messages[this._activeSessionId];
    if (!list) return;
    var msg = findById(list, msgId);
    if (!msg) return;
    msg.text += text;

    var dom = this._msgDoms[msgId];
    if (dom) {
      dom.textEl.style.whiteSpace = 'pre-wrap';
      dom.textEl.textContent = msg.text;
      this._dom.msgs.scrollTop = this._dom.msgs.scrollHeight;
    }
  };

  ClaudeChat.prototype._addToolCall = function (msgId, tc) {
    var list = this._messages[this._activeSessionId];
    if (!list) return;
    var msg = findById(list, msgId);
    if (!msg) return;
    var tool = { id: tc.id, name: tc.name,
      summary: JSON.stringify(tc.input || {}).slice(0, 120), status: 'running' };
    msg.toolCalls.push(tool);

    var dom = this._msgDoms[msgId];
    if (dom) {
      if (!dom.el.contains(dom.toolsEl)) dom.el.appendChild(dom.toolsEl);
      dom.toolsEl.appendChild(this._buildToolEl(tool));
      this._dom.msgs.scrollTop = this._dom.msgs.scrollHeight;
    }
  };

  ClaudeChat.prototype._updateToolCall = function (msgId, toolId, status) {
    var list = this._messages[this._activeSessionId];
    if (!list) return;
    var msg = findById(list, msgId);
    if (!msg) return;
    var tc = findById(msg.toolCalls, toolId);
    if (tc) tc.status = status;

    var toolEl = document.getElementById('tool-' + toolId);
    if (toolEl) {
      toolEl.className = 'cld-tool cld-tool-' + status;
      var b = toolEl.querySelector('.cld-bullet');
      if (b) b.className = 'cld-bullet ' + (status === 'done' ? 'cld-bullet-done' : 'cld-bullet-live');
    }
  };

  ClaudeChat.prototype._finalizeMsg = function (msgId, status, error) {
    var self = this;
    var list = this._messages[this._activeSessionId];
    if (!list) return;
    var msg = findById(list, msgId);
    if (!msg) return;
    msg.status = status;
    msg.error  = error || null;
    (msg.toolCalls || []).forEach(function (tc) {
      if (tc.status === 'running') tc.status = 'done';
    });

    var dom = this._msgDoms[msgId];
    if (!dom) return;

    dom.el.className = 'cld-msg cld-msg-assistant cld-msg-' + status;
    dom.bulletEl.className = 'cld-bullet ' + bulletClass(status);

    // Re-render with full markdown + options parsing
    self._renderTextWithOptions(dom.textEl, msg);

    // Finalize all tool bullets
    (msg.toolCalls || []).forEach(function (tc) {
      var toolEl = document.getElementById('tool-' + tc.id);
      if (toolEl) {
        toolEl.className = 'cld-tool cld-tool-done';
        var b = toolEl.querySelector('.cld-bullet');
        if (b) b.className = 'cld-bullet cld-bullet-done';
      }
    });

    if (error) {
      var errDiv = dom.el.querySelector('.cld-msg-error') || el('div', 'cld-msg-error');
      errDiv.textContent = '⚠ ' + error;
      if (!dom.el.contains(errDiv)) dom.el.appendChild(errDiv);
    }
  };

  ClaudeChat.prototype._setStreaming = function (on) {
    var self = this;
    this._streaming = on;
    var btn = this._dom.sendBtn;
    if (on) {
      btn.className  = 'cld-send-btn cld-send-stop';
      btn.textContent = '■';
      btn.disabled   = false;
      this._dom.textarea.disabled = true;
      this._dom.textarea.placeholder = 'Claude가 응답 중입니다…';
      this._dom.composer.className = 'cld-composer cld-composer-streaming';
    } else {
      btn.className  = 'cld-send-btn';
      btn.textContent = '↑';
      btn.disabled   = !this._dom.textarea.value.trim();
      this._dom.textarea.disabled = false;
      this._dom.textarea.placeholder = this._opt.placeholder;
      this._dom.composer.className = 'cld-composer';
      this._currentAId = null;
      this._abortCtrl  = null;
      // Re-enable option buttons
      this._dom.msgs.querySelectorAll('.cld-option-btn')
        .forEach(function (b) { b.disabled = false; });
    }
  };

  // ── Send & SSE stream ─────────────────────────────────────────────────────────
  ClaudeChat.prototype._onSend = function () {
    var self = this;
    if (this._streaming) { this._doAbort(); return; }
    var text = this._dom.textarea.value.trim();
    if (!text) return;

    this._closeMention();
    this._dom.textarea.value = '';
    this._dom.sendBtn.disabled = true;

    var doSend = function (sessionId) {
      self._addMsg(sessionId, {
        id: uid(), role: 'user', text: text, status: 'done', toolCalls: []
      });
      var aId = uid();
      self._currentAId = aId;
      self._ensureAssistant(aId);
      self._setStreaming(true);
      self._streamSend(sessionId, text, aId);
    };

    if (this._activeSessionId) {
      doSend(this._activeSessionId);
    } else {
      this._createSession()
        .then(function (s) { doSend(s.id); })
        .catch(function (e) { alert('세션 생성 실패: ' + e.message); });
    }
  };

  ClaudeChat.prototype._streamSend = function (sessionId, message, aId) {
    var self = this;
    this._abortCtrl = typeof AbortController !== 'undefined' ? new AbortController() : null;

    var fetchOpts = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
      body: JSON.stringify({ message: message }),
    };
    if (this._abortCtrl) fetchOpts.signal = this._abortCtrl.signal;

    fetch(this._opt.apiBase + '/sessions/' + sessionId + '/send', fetchOpts)
      .then(function (resp) {
        if (!resp.ok) throw new Error('HTTP ' + resp.status);
        return self._readSSE(resp.body.getReader(), sessionId, aId);
      })
      .catch(function (e) {
        if (e.name === 'AbortError') {
          self._finalizeMsg(aId, 'done');
        } else {
          self._finalizeMsg(aId, 'error', e.message);
        }
        self._setStreaming(false);
      });
  };

  ClaudeChat.prototype._readSSE = function (reader, sessionId, aId) {
    var self = this;
    var decoder = new TextDecoder();
    var buffer  = '';

    function pump() {
      return reader.read().then(function (chunk) {
        if (chunk.done) {
          self._finalizeMsg(aId, 'done');
          self._setStreaming(false);
          return;
        }
        buffer += decoder.decode(chunk.value, { stream: true });

        var blocks = buffer.split('\n\n');
        buffer = blocks.pop(); // keep incomplete last block

        blocks.forEach(function (block) {
          block.split('\n').forEach(function (line) {
            if (!line.startsWith('data: ')) return;
            var raw = line.slice(6);
            if (!raw || raw === '{"type":"ping"}') return;
            var ev;
            try { ev = JSON.parse(raw); } catch (_) { return; }
            self._handleEvent(ev, aId);
          });
        });

        return pump();
      });
    }

    return pump().catch(function (e) {
      if (e.name === 'AbortError') {
        self._finalizeMsg(aId, 'done');
      } else {
        self._finalizeMsg(aId, 'error', e.message);
      }
      self._setStreaming(false);
    });
  };

  ClaudeChat.prototype._handleEvent = function (ev, aId) {
    if (ev.type === 'assistant') {
      var content = (ev.message && ev.message.content) ? ev.message.content : [];
      for (var i = 0; i < content.length; i++) {
        var part = content[i];
        if (part.type === 'text' && part.text) {
          this._appendText(aId, part.text);
        } else if (part.type === 'tool_use') {
          this._addToolCall(aId, part);
        }
      }
    } else if (ev.type === 'tool_result') {
      this._updateToolCall(aId, ev.tool_use_id, 'done');
    } else if (ev.type === 'result' || ev.type === 'done') {
      this._finalizeMsg(aId, 'done');
      this._setStreaming(false);
    } else if (ev.type === 'error') {
      this._finalizeMsg(aId, 'error', ev.message);
      this._setStreaming(false);
    }
  };

  ClaudeChat.prototype._doAbort = function () {
    if (this._abortCtrl) this._abortCtrl.abort();
    if (this._activeSessionId) {
      fetch(this._opt.apiBase + '/sessions/' + this._activeSessionId + '/abort',
        { method: 'POST' }).catch(function () {});
    }
  };

  // ── Option buttons ────────────────────────────────────────────────────────────
  ClaudeChat.prototype._onOptionClick = function (option, index) {
    var self = this;
    if (this._streaming) return;
    var sessionId = this._activeSessionId;
    if (!sessionId) return;

    var doSend = function () {
      self._addMsg(sessionId, {
        id: uid(), role: 'user', text: option, status: 'done', toolCalls: []
      });
      var aId = uid();
      self._currentAId = aId;
      self._ensureAssistant(aId);
      self._setStreaming(true);
      self._streamSend(sessionId, option, aId);
    };

    if (this._opt.onOptionSelect) {
      Promise.resolve(this._opt.onOptionSelect(sessionId, option, index))
        .then(function (handled) { if (!handled) doSend(); });
    } else {
      doSend();
    }
  };

  // ── @-mention ─────────────────────────────────────────────────────────────────
  ClaudeChat.prototype._onInput = function () {
    var value  = this._dom.textarea.value;
    var cursor = this._dom.textarea.selectionStart || value.length;
    var before = value.slice(0, cursor);

    this._dom.sendBtn.disabled = !value.trim() || this._streaming;

    var atMatch = before.match(/@([^\s@]*)$/);
    if (atMatch) {
      this._mentionQuery = atMatch[1];
      this._mentionStart = cursor - atMatch[0].length;
      this._openMention();
    } else {
      this._closeMention();
    }
  };

  ClaudeChat.prototype._openMention = function () {
    var self = this;
    var q = this._mentionQuery.toLowerCase();
    this._filteredMentions = this._opt.mentionItems.filter(function (item) {
      return item.name.toLowerCase().includes(q);
    });
    if (!this._filteredMentions.length) { this._closeMention(); return; }

    var popup = this._dom.mentionPopup;
    popup.innerHTML = '';
    this._mentionHl   = 0;
    this._mentionOpen = true;

    this._filteredMentions.forEach(function (item, i) {
      var btn = el('button', 'cld-mention-item' + (i === 0 ? ' cld-mention-item-active' : ''));
      btn.type = 'button';
      btn.innerHTML =
        '<span class="cld-mention-name">@' + escHtml(item.name) + '</span>'
        + '<span class="cld-mention-type">' + escHtml(item.type) + '</span>';
      btn.addEventListener('mousedown', function (e) { e.preventDefault(); self._selectMention(item); });
      btn.addEventListener('mouseenter', function () {
        self._mentionHl = i;
        self._updateMentionHl();
      });
      popup.appendChild(btn);
    });

    popup.style.display = 'block';
  };

  ClaudeChat.prototype._closeMention = function () {
    this._mentionOpen = false;
    this._dom.mentionPopup.style.display = 'none';
  };

  ClaudeChat.prototype._selectMention = function (item) {
    var input = this._dom.textarea.value;
    var after = input.slice(this._mentionStart + 1 + this._mentionQuery.length);
    var newVal = input.slice(0, this._mentionStart) + '@' + item.name + ' ' + after;
    this._dom.textarea.value = newVal;
    this._dom.sendBtn.disabled = !newVal.trim();
    this._closeMention();
    if (this._activeSessionId && this._opt.onMentionSelect) {
      this._opt.onMentionSelect(this._activeSessionId, item);
    }
    var pos = this._mentionStart + item.name.length + 2;
    requestAnimationFrame(function () {
      this._dom.textarea.focus();
      this._dom.textarea.setSelectionRange(pos, pos);
    }.bind(this));
  };

  ClaudeChat.prototype._updateMentionHl = function () {
    var items = this._dom.mentionPopup.querySelectorAll('.cld-mention-item');
    var hl = this._mentionHl;
    items.forEach(function (b, i) {
      b.className = 'cld-mention-item' + (i === hl ? ' cld-mention-item-active' : '');
    });
  };

  ClaudeChat.prototype._onKeyDown = function (e) {
    var items = this._filteredMentions;
    if (this._mentionOpen && items.length) {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        this._mentionHl = Math.min(this._mentionHl + 1, items.length - 1);
        this._updateMentionHl(); return;
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        this._mentionHl = Math.max(this._mentionHl - 1, 0);
        this._updateMentionHl(); return;
      }
      if (e.key === 'Enter' && !e.isComposing) {
        e.preventDefault();
        if (items[this._mentionHl]) this._selectMention(items[this._mentionHl]);
        return;
      }
      if (e.key === 'Escape') { e.preventDefault(); this._closeMention(); return; }
    }
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
      e.preventDefault();
      this._onSend();
    }
  };

  // ── Helpers ──────────────────────────────────────────────────────────────────
  function el(tag, className) {
    var e = document.createElement(tag);
    if (className) e.className = className;
    return e;
  }

  function bulletClass(status) {
    return status === 'streaming' ? 'cld-bullet-live'
         : status === 'error'     ? 'cld-bullet-error'
         : 'cld-bullet-done';
  }

  function findById(arr, id) {
    if (!arr) return null;
    for (var i = 0; i < arr.length; i++) {
      if (arr[i].id === id) return arr[i];
    }
    return null;
  }

  // ── Export ───────────────────────────────────────────────────────────────────
  global.ClaudeChat = ClaudeChat;

})(window);
