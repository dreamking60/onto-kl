/* ==========================================================================
   Ontology modeling UI: source document -> T-Box + A-Box -> reasoning models
   ========================================================================== */
(function () {
  'use strict';

  var SAMPLE_TEXT = [
    '中华人民共和国示例合同法',
    '',
    '第一章 总则',
    '第一条 为了规范合同行为，保护当事人的合法权益，制定本法。',
    '第二条 “合同”是指平等主体之间设立、变更、终止民事权利义务关系的协议。',
    '第三条 合同当事人应当遵循公平原则。',
    '',
    '第二章 合同的订立',
    '第四条 “要约”是指希望与他人订立合同的意思表示。',
    '第五条 “承诺”是指受要约人同意要约的意思表示。',
    '第六条 当事人订立合同，可以采用书面形式、口头形式或者其他形式。',
    '',
    '第三章 合同的履行',
    '第七条 当事人应当按照约定全面履行自己的义务。',
    '第八条 “违约责任”是指当事人不履行合同义务或者履行合同义务不符合约定时应当承担的责任。'
  ].join('\n');

  var state = {
    project: null,
    modelId: null,
    preview: 'ontology',
    ai: null,
    busy: false
  };

  function $(selector) { return document.querySelector(selector); }
  function $all(selector) { return Array.prototype.slice.call(document.querySelectorAll(selector)); }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function formatNumber(value) {
    if (value == null || value === '') { return '0'; }
    var number = Number(value);
    return Number.isFinite(number) ? number.toLocaleString('zh-CN') : String(value);
  }

  function formatTime(value) {
    if (!value) { return '-'; }
    try { return new Date(value).toLocaleString('zh-CN'); } catch (ignored) { return String(value); }
  }

  function toast(message, type, timeout) {
    var container = $('#toasts');
    var element = document.createElement('div');
    element.className = 'toast ' + (type || 'info');
    element.innerHTML = escapeHtml(message);
    container.appendChild(element);
    window.setTimeout(function () {
      element.classList.add('hide');
      window.setTimeout(function () { element.remove(); }, 250);
    }, timeout || 3600);
  }

  function setBusy(button, busy, text) {
    if (!button) { return; }
    if (busy) {
      button.dataset.originalText = button.textContent;
      button.disabled = true;
      button.textContent = text || '处理中…';
    } else {
      button.disabled = false;
      button.textContent = button.dataset.originalText || button.textContent;
    }
  }

  async function api(path, options) {
    var opts = options || {};
    var request = { method: opts.method || 'GET', headers: {} };
    if (opts.formData) {
      request.body = opts.formData;
    } else if (opts.body !== undefined && opts.body !== null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = JSON.stringify(opts.body);
    }
    var response;
    try {
      response = await fetch(path, request);
    } catch (networkError) {
      throw new Error('无法连接服务端（' + networkError.message + '）');
    }
    var text = await response.text();
    var data = null;
    if (text) {
      try { data = JSON.parse(text); } catch (parseError) { data = text; }
    }
    if (!response.ok) {
      var message = (data && data.message) || ('HTTP ' + response.status);
      var error = new Error(message);
      error.code = data && data.code;
      error.status = response.status;
      throw error;
    }
    return data;
  }

  async function fetchText(path) {
    var response = await fetch(path);
    var text = await response.text();
    if (!response.ok) {
      throw new Error('读取内容失败：HTTP ' + response.status);
    }
    try { return JSON.stringify(JSON.parse(text), null, 2); } catch (ignored) { return text; }
  }

  // ------------------------------------------------------------------
  // AI configuration
  // ------------------------------------------------------------------

  var PROVIDER_DEFAULTS = {
    ollama: { baseUrl: 'http://127.0.0.1:11434', model: 'qwen2.5:7b' },
    openai: { baseUrl: 'http://127.0.0.1:8000', model: 'gpt-4o-mini' },
    heuristic: { baseUrl: '', model: '' },
    mock: { baseUrl: 'in-process', model: 'mock' }
  };

  function applyProviderDefaults() {
    var provider = $('#ai-provider').value;
    var defaults = PROVIDER_DEFAULTS[provider] || {};
    $('#ai-base').value = defaults.baseUrl || '';
    $('#ai-model').value = defaults.model || '';
    if (provider === 'ollama' && state.ai && state.ai.models && state.ai.models.length) {
      $('#ai-model').value = state.ai.models[0];
    }
    var fallback = $('#ai-fallback');
    fallback.disabled = provider === 'heuristic' || provider === 'mock';
    if (fallback.disabled) { fallback.checked = provider === 'mock'; }
  }

  async function refreshAiStatus() {
    var health = $('#ai-health');
    var text = $('#ai-health-text');
    try {
      var status = await api('/modeling/ai/status');
      state.ai = status;
      health.classList.remove('down');
      health.classList.add(status.reachable ? 'up' : 'down');
      text.textContent = status.reachable
        ? (status.provider + ' 可用' + (status.models && status.models.length ? ' · ' + status.models.length + ' 个模型' : ''))
        : ('AI 不可用 · ' + (status.message || status.provider));
      $('#ai-status-text').textContent = (status.message || '') +
        (status.models && status.models.length ? '（已安装：' + status.models.join(', ') + '）' : '');
      var datalist = $('#ai-models');
      datalist.innerHTML = '';
      (status.models || []).forEach(function (model) {
        var option = document.createElement('option');
        option.value = model;
        datalist.appendChild(option);
      });
      if (status.provider) {
        var select = $('#ai-provider');
        select.value = status.provider;
        if (select.value !== status.provider) { select.value = 'ollama'; }
      }
      $('#ai-base').value = status.baseUrl || $('#ai-base').value;
      $('#ai-model').value = status.model || $('#ai-model').value;
      if (status.reachable && status.models && status.models.length
          && (!status.model || status.models.indexOf(status.model) < 0)) {
        $('#ai-model').value = status.models[0];
      }
      applyFallbackState();
    } catch (error) {
      health.classList.remove('up');
      health.classList.add('down');
      text.textContent = 'AI 状态检测失败';
      $('#ai-status-text').textContent = error.message;
    }
  }

  function applyFallbackState() {
    var provider = $('#ai-provider').value;
    var fallback = $('#ai-fallback');
    fallback.disabled = provider === 'heuristic' || provider === 'mock';
  }

  function currentAiConfig() {
    var provider = $('#ai-provider').value;
    var config = {
      provider: provider,
      temperature: Number($('#ai-temperature').value || 0.2),
      fallbackToHeuristic: $('#ai-fallback').checked
    };
    if (provider !== 'heuristic' && provider !== 'mock') {
      config.baseUrl = $('#ai-base').value.trim();
      config.model = $('#ai-model').value.trim();
    }
    return config;
  }

  // ------------------------------------------------------------------
  // Project operations
  // ------------------------------------------------------------------

  async function createProject() {
    var fileInput = $('#source-file');
    var file = fileInput.files && fileInput.files[0];
    var name = $('#project-name').value.trim();
    var data;
    if (file) {
      var form = new FormData();
      form.append('file', file);
      if (name) { form.append('name', name); }
      data = await api('/modeling/projects/upload', { method: 'POST', formData: form });
    } else {
      var content = $('#source-text').value.trim();
      if (!content) {
        throw new Error('请先粘贴文档内容，或选择要上传的文件');
      }
      data = await api('/modeling/projects', {
        method: 'POST',
        body: { name: name || undefined, fileName: 'source.txt', content: content }
      });
    }
    state.project = data;
    state.preview = 'ontology';
    render();
    refreshProjects();
    toast('项目已创建：' + data.name, 'success');
    return data;
  }

  async function ensureProject() {
    return state.project || await createProject();
  }

  async function runExtract(button) {
    setBusy(button, true, 'AI 抽取中…');
    try {
      var project = await ensureProject();
      if (!project) { return; }
      var data = await api('/modeling/projects/' + encodeURIComponent(project.id) + '/extract', {
        method: 'POST',
        body: currentAiConfig()
      });
      state.project = data;
      state.preview = 'extraction';
      render();
      toast('抽取完成：' + (data.statistics.classes || 0) + ' 个类、' +
        (data.statistics.individuals || 0) + ' 个个体', 'success', 4600);
    } catch (error) {
      toast(error.message, 'error', 6200);
    } finally {
      setBusy(button, false);
    }
  }

  async function runGenerate(button) {
    setBusy(button, true, '生成 Turtle…');
    try {
      var project = await ensureProject();
      if (!project) { return; }
      var data = await api('/modeling/projects/' + encodeURIComponent(project.id) + '/generate', {
        method: 'POST',
        body: { includeSourceComments: true }
      });
      state.project = data;
      state.preview = 'ontology';
      render();
      toast('已生成 T-Box ' + formatNumber(data.statistics.tboxTriples) + ' 三元组、A-Box ' +
        formatNumber(data.statistics.aboxTriples) + ' 三元组', 'success', 5000);
    } catch (error) {
      toast(error.message, 'error', 6200);
    } finally {
      setBusy(button, false);
    }
  }

  async function runLoad(button) {
    setBusy(button, true, '载入推理系统…');
    try {
      var project = await ensureProject();
      if (!project) { return; }
      var data = await api('/modeling/projects/' + encodeURIComponent(project.id) + '/load', {
        method: 'POST',
        body: { autoReason: true }
      });
      state.project = data.project;
      showLoaded(data);
      render();
      toast('已载入推理系统，模型 ID：' + data.model.id, 'success', 6000);
    } catch (error) {
      toast(error.message, 'error', 6200);
    } finally {
      setBusy(button, false);
    }
  }

  async function runOneClick(button) {
    setBusy(button, true, '生成并载入中…');
    try {
      var project = await ensureProject();
      if (!project) { return; }
      var result = await api('/modeling/projects/' + encodeURIComponent(project.id) + '/pipeline', {
        method: 'POST',
        body: {
          extract: currentAiConfig(),
          generate: { includeSourceComments: true },
          load: { autoReason: true }
        }
      });
      state.project = result.project;
      state.preview = 'ontology';
      showLoaded(result.loaded);
      render();
      toast('一键完成：生成并载入模型 ' + result.loaded.model.id, 'success', 6500);
    } catch (error) {
      toast(error.message, 'error', 7000);
    } finally {
      setBusy(button, false);
    }
  }

  function showLoaded(loaded) {
    state.modelId = loaded.model.id;
    var card = $('#load-success');
    card.hidden = false;
    $('#load-model-chip').textContent = loaded.model.id + ' · ' + loaded.model.state;
    var stats = loaded.model.statistics || {};
    var reasoning = loaded.reasoning || {};
    $('#load-success-detail').textContent =
      'A-Box 来源：' + (loaded.model.aboxSource || '-') +
      '；推理状态：' + (loaded.model.state || '-') +
      '；推导三元组：' + formatNumber(reasoning.inferredTotal || 0) +
      '；可在推理工作台中查询、运行推理并查看解释。';
    $('#open-workspace').href = '/?model=' + encodeURIComponent(loaded.model.id);
  }

  async function refreshProjects() {
    var container = $('#project-list');
    try {
      var projects = await api('/modeling/projects');
      if (!projects || projects.length === 0) {
        container.innerHTML = '<div class="placeholder small">还没有建模项目</div>';
        return;
      }
      container.innerHTML = projects.map(function (project) {
        var active = state.project && state.project.id === project.id;
        return '<button class="modeling-project' + (active ? ' active' : '') +
          '" data-project="' + escapeHtml(project.id) + '">' +
          '<b>' + escapeHtml(project.name) + '</b>' +
          '<span>' + escapeHtml(project.status) + ' · ' + escapeHtml(project.sourceType || '') +
          ' · ' + formatNumber(project.statistics && project.statistics.textChars) + ' 字符</span>' +
          '</button>';
      }).join('');
      $all('.modeling-project').forEach(function (button) {
        button.addEventListener('click', function () { openProject(button.dataset.project); });
      });
    } catch (error) {
      container.innerHTML = '<div class="error-box"><b>加载失败</b>' + escapeHtml(error.message) + '</div>';
    }
  }

  async function openProject(projectId) {
    try {
      var data = await api('/modeling/projects/' + encodeURIComponent(projectId));
      state.project = data;
      state.modelId = data.modelId;
      state.preview = 'ontology';
      render();
      if (data.modelId) {
        showLoaded({
          model: { id: data.modelId, state: 'LOADED', aboxSource: 'local', statistics: {} },
          reasoning: {}
        });
        $('#load-success-detail').textContent = '该项目已载入推理系统，模型 ID：' + data.modelId;
      }
      refreshProjects();
    } catch (error) {
      toast(error.message, 'error', 5200);
    }
  }

  // ------------------------------------------------------------------
  // Rendering
  // ------------------------------------------------------------------

  var STATUS_RANK = { CREATED: 0, EXTRACTED: 1, GENERATED: 2, LOADED: 3, FAILED: -1 };
  var STATUS_LABEL = {
    CREATED: '已创建', EXTRACTED: '已抽取', GENERATED: '已生成', LOADED: '已载入', FAILED: '失败'
  };

  function render() {
    var project = state.project;
    if (!project) {
      $('#project-title').textContent = '尚未创建项目';
      $('#project-meta').textContent = '填写左侧内容，点击「创建项目」或直接「一键生成并载入」。';
      $('#status-chip').textContent = '-';
      $('#status-chip').className = 'chip';
      $('#ai-chip').textContent = '-';
      $('#modeling-stats').innerHTML = '';
      $('#modeling-warnings').hidden = true;
      $('#load-success').hidden = true;
      renderSteps(null);
      renderPreview();
      return;
    }
    $('#project-title').textContent = project.name;
    $('#project-meta').textContent = [
      project.sourceFileName,
      project.sourceType ? project.sourceType.toUpperCase() : '',
      '字符 ' + formatNumber(project.statistics && project.statistics.textChars),
      '更新于 ' + formatTime(project.updatedAt)
    ].filter(Boolean).join(' · ');

    var statusChip = $('#status-chip');
    statusChip.textContent = STATUS_LABEL[project.status] || project.status;
    statusChip.className = 'chip status-' + String(project.status || '').toLowerCase();

    var aiChip = $('#ai-chip');
    aiChip.textContent = (project.aiProvider || '未抽取') +
      (project.aiModel ? ' · ' + project.aiModel : '');

    renderSteps(project.status);
    renderStats(project.statistics || {});
    renderWarnings(project.warnings || []);
    renderPreview();
  }

  function renderSteps(status) {
    var rank = STATUS_RANK[status] == null ? -1 : STATUS_RANK[status];
    $all('.wizard-step').forEach(function (step) {
      var stepRank = Number(step.dataset.rank);
      step.classList.toggle('done', rank >= stepRank && rank !== -1);
      step.classList.toggle('current', rank === stepRank);
    });
  }

  function renderStats(stats) {
    var items = [
      { label: '文本字符', value: stats.textChars, cls: '' },
      { label: '抽取分块', value: stats.chunkCount, cls: '' },
      { label: '本体类', value: stats.classes, cls: 'accent' },
      { label: '对象属性', value: stats.objectProperties, cls: '' },
      { label: '数据属性', value: stats.datatypeProperties, cls: '' },
      { label: '实例', value: stats.individuals, cls: 'green' },
      { label: '对象关系', value: stats.relations, cls: '' },
      { label: 'T-Box 三元组', value: stats.tboxTriples, cls: 'accent' },
      { label: 'A-Box 三元组', value: stats.aboxTriples, cls: 'green' }
    ];
    $('#modeling-stats').innerHTML = items.map(function (item) {
      return '<div class="stat-card ' + item.cls + '"><div class="stat-label">' + item.label +
        '</div><div class="stat-value">' + formatNumber(item.value) + '</div></div>';
    }).join('');
  }

  function renderWarnings(warnings) {
    var container = $('#modeling-warnings');
    if (!warnings || warnings.length === 0) {
      container.hidden = true;
      container.innerHTML = '';
      return;
    }
    container.hidden = false;
    container.innerHTML = '<b>处理提示（' + warnings.length + '）</b><ul>' +
      warnings.map(function (warning) { return '<li>' + escapeHtml(warning) + '</li>'; }).join('') + '</ul>';
  }

  function previewContent() {
    var project = state.project;
    if (!project) { return null; }
    if (state.preview === 'instances') { return project.instancesPreview; }
    if (state.preview === 'extraction') { return project.extractionPreview; }
    return project.ontologyPreview;
  }

  function renderPreview() {
    var content = previewContent();
    $all('.preview-tab').forEach(function (tab) {
      tab.classList.toggle('active', tab.dataset.preview === state.preview);
    });
    var body = $('#preview-body');
    if (content) {
      body.textContent = content;
    } else if (state.project) {
      body.innerHTML = '<span class="placeholder">该步骤还没有内容，请先完成抽取/生成，或点击「刷新」。</span>';
    } else {
      body.innerHTML = '<span class="placeholder">创建项目并完成抽取/生成后，这里显示生成的 Turtle 文档。</span>';
    }
  }

  async function refreshPreview(force) {
    var project = state.project;
    if (!project) { return; }
    if (!force && previewContent()) { return; }
    var path = state.preview === 'instances'
      ? '/modeling/projects/' + encodeURIComponent(project.id) + '/instances'
      : state.preview === 'extraction'
        ? '/modeling/projects/' + encodeURIComponent(project.id) + '/extraction'
        : '/modeling/projects/' + encodeURIComponent(project.id) + '/ontology';
    try {
      var content = await fetchText(path);
      if (state.preview === 'instances') { project.instancesPreview = content; }
      else if (state.preview === 'extraction') { project.extractionPreview = content; }
      else { project.ontologyPreview = content; }
      renderPreview();
    } catch (error) {
      toast(error.message, 'error');
    }
  }

  // ------------------------------------------------------------------
  // Events / bootstrap
  // ------------------------------------------------------------------

  function wireEvents() {
    $('#btn-sample').addEventListener('click', function () {
      $('#source-text').value = SAMPLE_TEXT;
      $('#source-file').value = '';
      $('#file-hint').textContent = '已载入示例文本。点击「创建项目」或「一键生成并载入」。';
    });
    $('#source-file').addEventListener('change', function () {
      var file = $('#source-file').files[0];
      $('#file-hint').textContent = file
        ? '已选择文件：' + file.name + '（服务端解析，支持 PDF/DOCX）'
        : '未选择文件。若选择文件，将优先使用上传文件。';
    });
    $('#ai-provider').addEventListener('change', function () {
      applyProviderDefaults();
      applyFallbackState();
    });
    $('#btn-create').addEventListener('click', async function () {
      setBusy(this, true, '创建中…');
      try { await createProject(); } catch (error) { toast(error.message, 'error', 6200); } finally { setBusy(this, false); }
    });
    $('#btn-extract').addEventListener('click', function () { runExtract(this); });
    $('#btn-generate').addEventListener('click', function () { runGenerate(this); });
    $('#btn-load').addEventListener('click', function () { runLoad(this); });
    $('#btn-one-click').addEventListener('click', function () { runOneClick(this); });
    $('#btn-refresh-projects').addEventListener('click', refreshProjects);
    $('#btn-refresh-preview').addEventListener('click', function () { refreshPreview(true); });
    $all('.preview-tab').forEach(function (tab) {
      tab.addEventListener('click', function () {
        state.preview = tab.dataset.preview;
        renderPreview();
        refreshPreview(false);
      });
    });
    $('#btn-copy-model').addEventListener('click', function () {
      if (!state.modelId) { return; }
      if (navigator.clipboard) {
        navigator.clipboard.writeText(state.modelId).then(function () {
          toast('已复制模型 ID：' + state.modelId, 'success');
        });
      } else {
        toast('模型 ID：' + state.modelId, 'info');
      }
    });
  }

  function bootstrap() {
    wireEvents();
    refreshAiStatus();
    refreshProjects();
    try {
      var projectId = new URLSearchParams(window.location.search).get('project');
      if (projectId) { openProject(projectId); }
    } catch (ignored) {
      // ignore
    }
  }

  document.addEventListener('DOMContentLoaded', bootstrap);
})();
