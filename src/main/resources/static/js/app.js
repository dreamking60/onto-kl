/* ==========================================================================
   Ontology Reasoning System - demo UI (vanilla JS, no build step)
   Talks to the Spring Boot REST API on the same origin.
   ========================================================================== */
(function () {
  'use strict';

  // ------------------------------------------------------------------
  // Constants / state
  // ------------------------------------------------------------------

  var NS = 'http://example.org/company#';
  var PREFIXES = {
    'http://www.w3.org/1999/02/22-rdf-syntax-ns#': 'rdf:',
    'http://www.w3.org/2000/01/rdf-schema#': 'rdfs:',
    'http://www.w3.org/2002/07/owl#': 'owl:',
    'http://www.w3.org/2001/XMLSchema#': 'xsd:'
  };

  var state = {
    models: [],
    selectedId: null,
    detail: null,
    activeTab: 'query'
  };

  var QUERY_EXAMPLES = [
    {
      label: '类清单（任意模型）',
      sparql: 'SELECT ?class ?label\nWHERE {\n  ?class a owl:Class .\n  OPTIONAL { ?class rdfs:label ?label }\n}\nORDER BY ?class'
    },
    {
      label: '实例清单（任意模型）',
      sparql: 'SELECT ?s ?type\nWHERE {\n  ?s a ?type .\n  FILTER(?type != owl:NamedIndividual && ?type != owl:Class && ?type != owl:ObjectProperty && ?type != owl:DatatypeProperty)\n}\nLIMIT 50'
    },
    {
      label: '高级经理（业务规则）',
      sparql: 'SELECT ?e ?name\nWHERE {\n  ?e a :SeniorManager ;\n     :name ?name .\n}\nORDER BY ?name'
    },
    {
      label: '总监（业务规则）',
      sparql: 'SELECT ?e ?name\nWHERE {\n  ?e a :Director ;\n     :name ?name .\n}\nORDER BY ?name'
    },
    {
      label: '管理者（OWL 逆属性 + 定义域）',
      sparql: 'SELECT ?e ?name\nWHERE {\n  ?e a :Manager ;\n     :name ?name .\n}\nORDER BY ?name'
    },
    {
      label: '汇报链（递归闭包）',
      sparql: 'SELECT ?employee ?name ?boss ?bossName\nWHERE {\n  ?employee :reportsTo ?boss ;\n            :name ?name .\n  ?boss :name ?bossName .\n}\nORDER BY ?name ?bossName'
    },
    {
      label: '部门传递归属（transitive）',
      sparql: 'SELECT ?unit ?departmentName\nWHERE {\n  ?unit :partOf <http://example.org/company#dept/5> ;\n        :departmentName ?departmentName .\n}\nORDER BY ?departmentName'
    },
    {
      label: '同部门同事（对称属性）',
      sparql: 'SELECT ?colleague ?name\nWHERE {\n  <http://example.org/company#emp/3> :hasColleague ?colleague .\n  ?colleague :name ?name .\n}\nORDER BY ?name'
    },
    {
      label: '部门拥有的项目（逆属性）',
      sparql: 'SELECT ?department ?departmentName ?project ?projectName\nWHERE {\n  ?department :owns ?project ;\n              :departmentName ?departmentName .\n  ?project :projectName ?projectName .\n}\nORDER BY ?departmentName'
    },
    {
      label: 'Staff 等价类',
      sparql: 'SELECT ?e ?name\nWHERE {\n  ?e a :Staff ;\n     :name ?name .\n}\nORDER BY ?name'
    },
    {
      label: 'ASK：传递闭包',
      sparql: 'ASK {\n  <http://example.org/company#dept/2>\n  <http://example.org/company#partOf>\n  <http://example.org/company#dept/5>\n}'
    },
    {
      label: 'CONSTRUCT：汇报链',
      sparql: 'CONSTRUCT { ?e :reportsTo ?m }\nWHERE { ?e :reportsTo ?m }',
      limit: 20
    },
    {
      label: '谓词统计',
      sparql: 'SELECT ?p (COUNT(?s) AS ?n)\nWHERE { ?s ?p ?o }\nGROUP BY ?p\nORDER BY DESC(?n)'
    }
  ];

  var EXPLAIN_EXAMPLES = [':emp/5', ':emp/2', ':emp/3', ':dept/1', ':proj/1'];

  // ------------------------------------------------------------------
  // Small helpers
  // ------------------------------------------------------------------

  function $(selector) {
    return document.querySelector(selector);
  }

  function $all(selector) {
    return Array.prototype.slice.call(document.querySelectorAll(selector));
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function compactTerm(term) {
    if (term == null) {
      return '';
    }
    var text = String(term);
    if (text.indexOf(NS) === 0) {
      return ':' + text.slice(NS.length);
    }
    for (var ns in PREFIXES) {
      if (Object.prototype.hasOwnProperty.call(PREFIXES, ns) && text.indexOf(ns) === 0) {
        return PREFIXES[ns] + text.slice(ns.length);
      }
    }
    return text.replace(/\^\^<([^>]+)>/g, function (match, iri) {
      var compact = compactTerm(iri);
      return compact === iri ? match : '^^' + compact;
    });
  }

  function formatNumber(value) {
    if (value == null || value === '') {
      return '0';
    }
    return Number(value).toLocaleString('zh-CN');
  }

  function formatTimestamp(value) {
    if (!value) {
      return '-';
    }
    var date = new Date(value);
    if (isNaN(date.getTime())) {
      return String(value);
    }
    return date.toLocaleString('zh-CN', { hour12: false });
  }

  function stateChipClass(stateName) {
    var lower = String(stateName || '').toLowerCase();
    return 'chip state-' + lower;
  }

  function setBusy(button, busy, text) {
    if (!button) {
      return;
    }
    if (busy) {
      if (!button.dataset.originalText) {
        button.dataset.originalText = button.textContent;
      }
      button.disabled = true;
      button.textContent = text || '处理中…';
    } else {
      button.disabled = false;
      if (button.dataset.originalText) {
        button.textContent = button.dataset.originalText;
      }
    }
  }

  function toast(message, type, timeout) {
    var container = $('#toasts');
    if (!container) {
      return;
    }
    var element = document.createElement('div');
    element.className = 'toast ' + (type || 'info');
    element.textContent = message;
    container.appendChild(element);
    window.setTimeout(function () {
      element.style.opacity = '0';
      element.style.transform = 'translateY(6px)';
      element.style.transition = 'all .2s ease';
      window.setTimeout(function () {
        element.remove();
      }, 220);
    }, timeout || 4200);
  }

  // ------------------------------------------------------------------
  // REST client
  // ------------------------------------------------------------------

  async function api(path, options) {
    var opts = options || {};
    var request = { method: opts.method || 'GET', headers: {} };
    if (opts.body !== undefined && opts.body !== null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = JSON.stringify(opts.body);
    }
    var response;
    try {
      response = await fetch(path, request);
    } catch (networkError) {
      var network = new Error('无法连接服务端（' + networkError.message + '）');
      network.code = 'NETWORK_ERROR';
      throw network;
    }
    var text = await response.text();
    var data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (parseError) {
        data = text;
      }
    }
    if (!response.ok) {
      var error = new Error((data && data.message) || ('HTTP ' + response.status));
      error.code = data && data.code;
      error.status = response.status;
      error.details = data && data.details;
      throw error;
    }
    return data;
  }

  function renderError(container, error) {
    var code = error && error.code ? String(error.code) : 'ERROR';
    var status = error && error.status ? ' · HTTP ' + error.status : '';
    var message = error && error.message ? error.message : '未知错误';
    container.innerHTML = '<div class="error-box"><b>' + escapeHtml(code) + status +
      '</b>' + escapeHtml(message) + '</div>';
  }

  // ------------------------------------------------------------------
  // Tabs
  // ------------------------------------------------------------------

  function switchTab(name) {
    state.activeTab = name;
    $all('.tab').forEach(function (tab) {
      tab.classList.toggle('active', tab.dataset.tab === name);
    });
    $all('.tab-panel').forEach(function (panel) {
      panel.classList.toggle('active', panel.dataset.panel === name);
    });
  }

  // ------------------------------------------------------------------
  // Health
  // ------------------------------------------------------------------

  async function refreshHealth() {
    var element = $('#health');
    var text = $('#health-text');
    try {
      var response = await fetch('/actuator/health');
      var body = await response.json();
      var up = response.ok && body.status === 'UP';
      element.classList.toggle('up', up);
      element.classList.toggle('down', !up);
      text.textContent = up ? '服务正常' : '服务异常';
    } catch (e) {
      element.classList.remove('up');
      element.classList.add('down');
      text.textContent = '服务不可用';
    }
  }

  // ------------------------------------------------------------------
  // Models
  // ------------------------------------------------------------------

  function renderModels() {
    var list = $('#model-list');
    list.innerHTML = '';
    if (!state.models.length) {
      list.innerHTML = '<div class="placeholder small">暂无已加载模型</div>';
      return;
    }
    state.models.forEach(function (model) {
      var card = document.createElement('button');
      card.type = 'button';
      card.className = 'model-card' + (model.id === state.selectedId ? ' active' : '');
      var virtual = model.virtualAboxTriples || 0;
      var inferred = model.inferredApplicationTriples || 0;
      var localAbox = model.aboxSource === 'local' || model.profile === 'local';
      card.innerHTML =
        '<div class="mc-top">' +
        '  <span class="mc-id">' + escapeHtml(model.id) + '</span>' +
        '  <span class="' + stateChipClass(model.state) + '">' + escapeHtml(model.state) + '</span>' +
        '</div>' +
        '<div class="mc-meta">' +
        '  <span>' + escapeHtml(model.profile) + '</span>' +
        '  <span>' + (localAbox ? 'A-Box ' : '虚拟 A-Box ') + formatNumber(virtual) + '</span>' +
        '  <span>推导 ' + formatNumber(inferred) + '</span>' +
        '</div>';
      card.addEventListener('click', function () {
        selectModel(model.id);
      });
      list.appendChild(card);
    });
  }

  async function refreshModels(selectId) {
    try {
      var models = await api('/models');
      state.models = models || [];
      if (selectId) {
        state.selectedId = selectId;
      }
      var exists = state.models.some(function (m) {
        return m.id === state.selectedId;
      });
      if (!exists) {
        state.selectedId = state.models.length ? state.models[0].id : null;
      }
      renderModels();
      if (state.selectedId) {
        await loadDetail(state.selectedId);
      } else {
        state.detail = null;
        renderWorkspace();
      }
    } catch (error) {
      toast('刷新模型列表失败：' + error.message, 'error');
    }
  }

  async function selectModel(modelId) {
    if (state.selectedId === modelId && state.detail) {
      switchTab(state.activeTab);
      return;
    }
    state.selectedId = modelId;
    renderModels();
    await loadDetail(modelId);
  }

  async function loadDetail(modelId) {
    try {
      var detail = await api('/models/' + encodeURIComponent(modelId));
      if (state.selectedId !== modelId) {
        return;
      }
      state.detail = detail;
      renderWorkspace();
      renderModels();
    } catch (error) {
      toast('获取模型详情失败：' + error.message, 'error');
    }
  }

  // ------------------------------------------------------------------
  // Workspace rendering
  // ------------------------------------------------------------------

  function renderWorkspace() {
    var empty = $('#empty-state');
    var workspace = $('#workspace');
    if (!state.detail) {
      empty.hidden = false;
      workspace.hidden = true;
      return;
    }
    empty.hidden = true;
    workspace.hidden = false;

    var model = state.detail;
    $('#model-name').textContent = model.name || model.id;
    $('#model-id').textContent = model.id + '  ·  ' + (model.description || '无描述');

    var stateChip = $('#chip-state');
    stateChip.textContent = model.state;
    stateChip.className = stateChipClass(model.state);

    var fileBasedAbox = model.profile === 'local' || model.aboxSource === 'local';
    $('#chip-profile').textContent = 'profile: ' + model.profile;
    var dbChip = $('#chip-db');
    dbChip.textContent = fileBasedAbox ? 'A-Box: 本地文件' : (model.database || '数据库');
    dbChip.title = fileBasedAbox ? (model.instancesFile || 'instances.ttl') : (model.jdbcUrl || '');

    renderStats(model, fileBasedAbox);
    renderDetailTab(model);
    switchTab(state.activeTab);
  }

  function renderStats(model, fileBasedAbox) {
    var stats = model.statistics || {};
    var items = [
      { label: 'T-Box 三元组', value: stats.tboxTriples, cls: '' },
      { label: fileBasedAbox ? 'A-Box（本地文件）' : '虚拟 A-Box', value: stats.virtualAboxTriples, cls: '' },
      { label: '断言图', value: stats.assertedTriples, cls: '' },
      { label: '规则闭包后', value: stats.afterBusinessRulesTriples, cls: '' },
      { label: '推理模型三元组', value: stats.inferenceModelTriples, cls: 'accent' },
      { label: '推导应用三元组', value: stats.inferredApplicationTriples, cls: 'green' },
      { label: '加载耗时', value: stats.loadMillis, cls: '', unit: 'ms' },
      { label: '推理耗时', value: stats.reasonMillis, cls: '', unit: 'ms' }
    ];
    var grid = $('#stat-grid');
    grid.innerHTML = items.map(function (item) {
      return '<div class="stat-card ' + item.cls + '">' +
        '<div class="stat-label">' + escapeHtml(item.label) + '</div>' +
        '<div class="stat-value">' + formatNumber(item.value) +
        (item.unit ? '<em>' + item.unit + '</em>' : '') + '</div>' +
        '</div>';
    }).join('');
  }

  function renderDetailTab(model) {
    var stats = model.statistics || {};
    var ontology = stats.ontology || {};
    var entries = [
      ['模型 ID', model.id, true],
      ['名称', model.name || '-', false],
      ['状态', model.state, false],
      ['数据源 profile', model.profile, false],
      ['数据库', model.database || '-', false],
      ['JDBC URL', model.jdbcUrl || '-', true],
      ['创建时间', formatTimestamp(model.createdAt), false],
      ['加载时间', formatTimestamp(model.loadedAt), false],
      ['推理时间', formatTimestamp(model.reasonedAt), false],
      ['物化时间', formatTimestamp(model.materializedAt), false],
      ['物化三元组', model.materializedTriples == null ? '-' : formatNumber(model.materializedTriples), false],
      ['T-Box 三元组', formatNumber(stats.tboxTriples), false],
      ['本体类数量', formatNumber(ontology.classes), false],
      ['对象属性数量', formatNumber(ontology.objectProperties), false],
      ['数据属性数量', formatNumber(ontology.datatypeProperties), false],
      ['虚拟 A-Box 三元组', formatNumber(stats.virtualAboxTriples), false],
      ['推理模型三元组', formatNumber(stats.inferenceModelTriples), false],
      ['推导应用三元组', formatNumber(stats.inferredApplicationTriples), false]
    ];
    $('#detail-json').innerHTML = entries.map(function (entry) {
      return '<div class="detail-item"><div class="k">' + escapeHtml(entry[0]) + '</div>' +
        '<div class="v' + (entry[2] ? ' mono' : '') + '">' + escapeHtml(entry[1]) + '</div></div>';
    }).join('');

    var files = [
      ['本体定义（T-Box）', model.ontologyFile],
      ['OBDA 映射', model.mappingFile],
      ['业务规则', model.rulesFile]
    ];
    $('#detail-files').innerHTML = files.map(function (file) {
      return '<li><b>' + escapeHtml(file[0]) + '</b>' + escapeHtml(file[1] || '（无）') + '</li>';
    }).join('');

    var updated = $('#detail-updated');
    updated.textContent = '更新于 ' + new Date().toLocaleTimeString('zh-CN', { hour12: false });
  }

  // ------------------------------------------------------------------
  // Query
  // ------------------------------------------------------------------

  function renderQueryExamples() {
    var container = $('#query-examples');
    container.innerHTML = '';
    QUERY_EXAMPLES.forEach(function (example, index) {
      var chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'example-chip';
      chip.textContent = example.label;
      chip.addEventListener('click', function () {
        $all('#query-examples .example-chip').forEach(function (other) {
          other.classList.remove('active');
        });
        chip.classList.add('active');
        $('#query-editor').value = example.sparql;
        $('#query-limit').value = example.limit || 100;
      });
      container.appendChild(chip);
    });
  }

  async function runQuery() {
    if (!state.selectedId) {
      toast('请先加载并选择一个模型', 'error');
      return;
    }
    var sparql = $('#query-editor').value.trim();
    if (!sparql) {
      toast('请输入 SPARQL 查询', 'error');
      return;
    }
    var button = $('#btn-run-query');
    setBusy(button, true, '查询中…');
    var container = $('#query-result');
    try {
      var body = { modelId: state.selectedId, sparql: sparql };
      var limit = parseInt($('#query-limit').value, 10);
      if (limit && limit > 0) {
        body.limit = limit;
      }
      var data = await api('/query', { method: 'POST', body: body });
      renderQueryResult(container, data);
    } catch (error) {
      renderError(container, error);
      toast('查询失败：' + error.message, 'error');
    } finally {
      setBusy(button, false);
    }
  }

  function renderQueryResult(container, data) {
    var meta = '<div class="result-meta">' +
      '<span class="chip">' + escapeHtml(data.queryType) + '</span>' +
      '<span>返回 <strong>' + formatNumber(data.rowCount) + '</strong> 行</span>' +
      '<span>耗时 <strong>' + formatNumber(data.durationMs) + ' ms</strong></span>';
    if (data.constructTriples != null) {
      meta += '<span>闭包语句 <strong>' + formatNumber(data.constructTriples) + '</strong></span>';
    }
    if (data.truncated) {
      meta += '<span class="chip chip-ghost">结果已截断</span>';
    }
    meta += '</div>';

    var body;
    if (data.queryType === 'SELECT') {
      body = renderSelectTable(data);
    } else if (data.queryType === 'ASK') {
      body = '<div class="ask-result"><div class="ask-badge ' + data.booleanResult + '">' +
        data.booleanResult + '</div><div class="muted">ASK 查询结果</div></div>';
    } else {
      body = '<pre class="turtle">' + escapeHtml(data.constructTurtle || '（空图）') + '</pre>';
    }

    var raw = '<details class="raw-json"><summary>原始 JSON 响应</summary><pre>' +
      escapeHtml(JSON.stringify(data, null, 2)) + '</pre></details>';

    container.innerHTML = meta + body + raw;
  }

  function renderSelectTable(data) {
    if (!data.rows || !data.rows.length) {
      return '<div class="placeholder">查询成功，但没有匹配结果。</div>';
    }
    var head = '<tr>' + data.columns.map(function (column) {
      return '<th>' + escapeHtml(column) + '</th>';
    }).join('') + '</tr>';
    var body = data.rows.map(function (row) {
      var cells = row.map(function (cell) {
        return '<td class="term' + (cell === '' ? ' empty-cell' : '') + '" title="' +
          escapeHtml(cell) + '">' + escapeHtml(compactTerm(cell)) + '</td>';
      }).join('');
      return '<tr>' + cells + '</tr>';
    }).join('');
    return '<div class="table-wrap"><table class="data-table"><thead>' + head +
      '</thead><tbody>' + body + '</tbody></table></div>';
  }

  // ------------------------------------------------------------------
  // Reasoning
  // ------------------------------------------------------------------

  async function runReason() {
    if (!state.selectedId) {
      toast('请先加载并选择一个模型', 'error');
      return;
    }
    var button = $('#btn-run-reason');
    setBusy(button, true, '推理中…');
    var container = $('#reason-result');
    try {
      var data = await api('/reason', {
        method: 'POST',
        body: {
          modelId: state.selectedId,
          materialize: $('#reason-materialize').checked,
          inferredLimit: 50
        }
      });
      renderReasonResult(container, data);
      await refreshModels(state.selectedId);
      toast(data.computed ? '推理完成（新计算闭包）' : '推理完成（复用缓存闭包）', 'success');
    } catch (error) {
      renderError(container, error);
      toast('推理失败：' + error.message, 'error');
    } finally {
      setBusy(button, false);
    }
  }

  function renderReasonResult(container, data) {
    var stats = data.statistics || {};
    var pills = '<div class="summary-pills">' +
      '<span class="pill">' + (data.computed ? '新计算闭包' : '复用缓存闭包') + '</span>' +
      '<span class="pill">耗时 <b>' + formatNumber(data.durationMs) + ' ms</b></span>' +
      '<span class="pill">推导总数 <b>' + formatNumber(data.inferredTotal) + '</b></span>' +
      '<span class="pill">物化 <b>' + formatNumber(data.materializedTriples) + '</b> 条</span>' +
      '</div>';

    var statRows = [
      ['T-Box', stats.tboxTriples],
      ['虚拟 A-Box（Ontop）', stats.virtualAboxTriples],
      ['断言图', stats.assertedTriples],
      ['规则闭包后', stats.afterBusinessRulesTriples],
      ['推理模型（OWL 闭包）', stats.inferenceModelTriples],
      ['Jena 推导（全部，含簿记）', stats.inferredTriples],
      ['推导应用三元组', stats.inferredApplicationTriples]
    ].map(function (row) {
      return '<tr><td>' + escapeHtml(row[0]) + '</td><td class="term">' + formatNumber(row[1]) + '</td></tr>';
    }).join('');

    var inferred = data.inferredTriples || [];
    var inferredTable = '<div class="placeholder">没有推导出应用三元组。</div>';
    if (inferred.length) {
      inferredTable = '<div class="table-wrap"><table class="data-table"><thead><tr>' +
        '<th>subject</th><th>predicate</th><th>object</th></tr></thead><tbody>' +
        inferred.map(function (triple) {
          var object = triple.objectKind === 'LITERAL'
            ? '"' + triple.object + '"' + (triple.language ? '@' + triple.language : '')
            : compactTerm(triple.object);
          return '<tr>' +
            '<td class="term">' + escapeHtml(compactTerm(triple.subject)) + '</td>' +
            '<td class="term">' + escapeHtml(compactTerm(triple.predicate)) + '</td>' +
            '<td class="term">' + escapeHtml(object) + '</td>' +
            '</tr>';
        }).join('') + '</tbody></table></div>';
    }

    var raw = '<details class="raw-json"><summary>原始 JSON 响应</summary><pre>' +
      escapeHtml(JSON.stringify(data, null, 2)) + '</pre></details>';

    container.innerHTML = pills +
      '<div class="section-title">统计</div>' +
      '<div class="table-wrap"><table class="data-table"><thead><tr><th>指标</th><th>值</th></tr></thead><tbody>' +
      statRows + '</tbody></table></div>' +
      '<div class="section-title">推导三元组（最多 50 条）</div>' + inferredTable + raw;
  }

  // ------------------------------------------------------------------
  // Explanation
  // ------------------------------------------------------------------

  function renderExplainExamples() {
    var container = $('#explain-examples');
    container.innerHTML = '';
    EXPLAIN_EXAMPLES.forEach(function (subject) {
      var chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'example-chip';
      chip.textContent = subject;
      chip.addEventListener('click', function () {
        $('#explain-subject').value = subject;
      });
      container.appendChild(chip);
    });
  }

  async function runExplain() {
    if (!state.selectedId) {
      toast('请先加载并选择一个模型', 'error');
      return;
    }
    var subject = $('#explain-subject').value.trim();
    if (!subject) {
      toast('请输入个体 IRI，例如 :emp/5', 'error');
      return;
    }
    var limit = parseInt($('#explain-limit').value, 10) || 20;
    var button = $('#btn-run-explain');
    setBusy(button, true, '解释中…');
    var container = $('#explain-result');
    try {
      var data = await api('/models/' + encodeURIComponent(state.selectedId) +
        '/explain?subject=' + encodeURIComponent(subject) + '&limit=' + limit);
      var types = (data.types || []).map(function (type) {
        return '<span class="type-chip">' + escapeHtml(compactTerm(type)) + '</span>';
      }).join('');
      var meta = '<div class="result-meta">' +
        '<span class="chip">' + escapeHtml(compactTerm(data.subject)) + '</span>' +
        '<span>推导语句 <strong>' + formatNumber(data.inferredStatements) + '</strong> 条</span>' +
        '</div>';
      var raw = '<details class="raw-json"><summary>原始 JSON 响应</summary><pre>' +
        escapeHtml(JSON.stringify(data, null, 2)) + '</pre></details>';
      container.innerHTML = meta +
        '<div class="types-row">' + (types || '<span class="muted">无本体类型</span>') + '</div>' +
        '<pre class="trace">' + escapeHtml(data.trace || '（无推导过程）') + '</pre>' + raw;
    } catch (error) {
      renderError(container, error);
      toast('解释失败：' + error.message, 'error');
    } finally {
      setBusy(button, false);
    }
  }

  // ------------------------------------------------------------------
  // Unload
  // ------------------------------------------------------------------

  async function unloadModel() {
    if (!state.selectedId) {
      return;
    }
    if (!window.confirm('确定卸载模型 ' + state.selectedId + ' 吗？\n\n会关闭 Ontop/Jena 运行时并删除本地文件，不影响 TDSQL 中的实例数据。')) {
      return;
    }
    try {
      await api('/models/' + encodeURIComponent(state.selectedId), { method: 'DELETE' });
      toast('已卸载模型 ' + state.selectedId, 'success');
      state.selectedId = null;
      state.detail = null;
      await refreshModels();
    } catch (error) {
      toast('卸载失败：' + error.message, 'error');
    }
  }

  // ------------------------------------------------------------------
  // Load dialog
  // ------------------------------------------------------------------

  function modeOf(name) {
    var checked = document.querySelector('input[name="' + name + '-mode"]:checked');
    return checked ? checked.value : 'default';
  }

  function updateSourceVisibility(section) {
    var mode = modeOf(section);
    var path = $('#load-' + section + '-path');
    var content = $('#load-' + section + '-content');
    if (path) {
      path.hidden = mode !== 'path';
    }
    if (content) {
      content.hidden = mode !== 'content';
    }
  }

  function wireSourceToggles() {
    ['ontology', 'mapping', 'rules'].forEach(function (section) {
      $all('input[name="' + section + '-mode"]').forEach(function (radio) {
        radio.addEventListener('change', function () {
          updateSourceVisibility(section);
        });
      });
      updateSourceVisibility(section);
    });
  }

  function openLoadDialog() {
    var dialog = $('#load-dialog');
    $('#load-form').reset();
    $('#load-error').hidden = true;
    wireSourceToggles();
    dialog.showModal();
    $('#load-model-id').focus();
  }

  function buildLoadBody() {
    var body = {};
    var modelId = $('#load-model-id').value.trim();
    var name = $('#load-name').value.trim();
    var profile = $('#load-profile').value;
    var jdbcUrl = $('#load-jdbc-url').value.trim();
    var jdbcUsername = $('#load-jdbc-username').value.trim();
    var jdbcPassword = $('#load-jdbc-password').value;
    var jdbcDriver = $('#load-jdbc-driver').value.trim();

    if (modelId) {
      body.modelId = modelId;
    }
    if (name) {
      body.name = name;
    }
    if (profile) {
      body.profile = profile;
    }
    if ($('#load-init-db').checked) {
      body.initDatabase = true;
    }
    if (jdbcUrl) {
      body.jdbcUrl = jdbcUrl;
    }
    if (jdbcUsername) {
      body.jdbcUsername = jdbcUsername;
    }
    if (jdbcPassword) {
      body.jdbcPassword = jdbcPassword;
    }
    if (jdbcDriver) {
      body.jdbcDriver = jdbcDriver;
    }

    var ontologyMode = modeOf('ontology');
    if (ontologyMode === 'path') {
      var ontologyPath = $('#load-ontology-path').value.trim();
      if (!ontologyPath) {
        throw new Error('请填写本体文件路径，或改为「服务默认」/「内联内容」');
      }
      body.ontologyPath = ontologyPath;
    } else if (ontologyMode === 'content') {
      var ontologyContent = $('#load-ontology-content').value;
      if (!ontologyContent.trim()) {
        throw new Error('请填写内联本体内容');
      }
      body.ontologyContent = ontologyContent;
    }

    var mappingMode = modeOf('mapping');
    if (mappingMode === 'path') {
      var mappingPath = $('#load-mapping-path').value.trim();
      if (!mappingPath) {
        throw new Error('请填写 OBDA 映射文件路径');
      }
      body.mappingPath = mappingPath;
    } else if (mappingMode === 'content') {
      var mappingContent = $('#load-mapping-content').value;
      if (!mappingContent.trim()) {
        throw new Error('请填写内联映射内容');
      }
      body.mappingContent = mappingContent;
    }

    var rulesMode = modeOf('rules');
    if (rulesMode === 'path') {
      var rulesPath = $('#load-rules-path').value.trim();
      if (!rulesPath) {
        throw new Error('请填写业务规则文件路径');
      }
      body.rulesPath = rulesPath;
    } else if (rulesMode === 'content') {
      var rulesContent = $('#load-rules-content').value;
      if (!rulesContent.trim()) {
        throw new Error('请填写内联业务规则内容');
      }
      body.rulesContent = rulesContent;
    }
    return body;
  }

  async function submitLoad(event) {
    event.preventDefault();
    var button = $('#btn-submit-load');
    var errorBox = $('#load-error');
    errorBox.hidden = true;
    var body;
    try {
      body = buildLoadBody();
    } catch (validationError) {
      errorBox.hidden = false;
      errorBox.innerHTML = '<b>请检查表单</b>' + escapeHtml(validationError.message);
      return;
    }
    setBusy(button, true, '加载中…');
    try {
      var detail = await api('/load', { method: 'POST', body: body });
      $('#load-dialog').close();
      await refreshModels(detail.id);
      switchTab('query');
      var stats = detail.statistics || {};
      toast('模型 ' + detail.id + ' 已加载：虚拟 A-Box ' + formatNumber(stats.virtualAboxTriples) +
        ' 三元组（' + formatNumber(stats.loadMillis) + ' ms）', 'success', 5200);
    } catch (error) {
      errorBox.hidden = false;
      errorBox.innerHTML = '<b>' + escapeHtml(error.code || 'LOAD_FAILED') +
        (error.status ? ' · HTTP ' + error.status : '') + '</b>' + escapeHtml(error.message);
    } finally {
      setBusy(button, false);
    }
  }

  // ------------------------------------------------------------------
  // One-click demo
  // ------------------------------------------------------------------

  async function quickDemo(button) {
    setBusy(button, true, '演示初始化中…');
    try {
      // remove previous one-click demo models so the model list stays clean
      var previousDemos = state.models.filter(function (model) {
        return model.id.indexOf('demo-h2-') === 0;
      });
      for (var i = 0; i < previousDemos.length; i++) {
        try {
          await api('/models/' + encodeURIComponent(previousDemos[i].id), { method: 'DELETE' });
        } catch (ignored) {
          // ignore cleanup failures
        }
      }
      var id = 'demo-h2-' + Math.random().toString(36).slice(2, 6);
      toast('正在用内置 H2 加载演示模型…', 'info', 2600);
      var detail = await api('/load', {
        method: 'POST',
        body: { modelId: id, name: '一键演示（H2）', profile: 'h2' }
      });
      toast('模型已加载，正在计算推理闭包…', 'info', 2600);
      var reasonData = await api('/reason', { method: 'POST', body: { modelId: id, materialize: true } });
      renderReasonResult($('#reason-result'), reasonData);
      await refreshModels(detail.id);
      switchTab('query');
      var firstChip = document.querySelector('#query-examples .example-chip');
      if (firstChip) {
        firstChip.click();
      }
      await runQuery();
      toast('演示完成：已加载 H2 模型、完成推理并执行示例查询', 'success', 5600);
    } catch (error) {
      toast('一键演示失败：' + (error.code ? error.code + ' · ' : '') + error.message, 'error', 7000);
    } finally {
      setBusy(button, false);
    }
  }

  // ------------------------------------------------------------------
  // Wiring
  // ------------------------------------------------------------------

  function wireEvents() {
    $('#btn-refresh-models').addEventListener('click', function () {
      refreshModels();
    });
    $('#btn-open-load').addEventListener('click', openLoadDialog);
    $('#btn-empty-load').addEventListener('click', openLoadDialog);
    $('#btn-empty-quick').addEventListener('click', function () {
      quickDemo(this);
    });
    $('#btn-quick-demo').addEventListener('click', function () {
      quickDemo(this);
    });
    $('#btn-close-dialog').addEventListener('click', function () {
      $('#load-dialog').close();
    });
    $('#btn-cancel-load').addEventListener('click', function () {
      $('#load-dialog').close();
    });
    $('#load-form').addEventListener('submit', submitLoad);

    $('#btn-reason').addEventListener('click', runReason);
    $('#btn-run-reason').addEventListener('click', runReason);
    $('#btn-run-query').addEventListener('click', runQuery);
    $('#btn-run-explain').addEventListener('click', runExplain);
    $('#btn-refresh-detail').addEventListener('click', function () {
      refreshModels(state.selectedId);
    });
    $('#btn-unload').addEventListener('click', unloadModel);
    $('#btn-unload-2').addEventListener('click', unloadModel);

    $all('.tab').forEach(function (tab) {
      tab.addEventListener('click', function () {
        switchTab(tab.dataset.tab);
      });
    });

    $('#query-editor').addEventListener('keydown', function (event) {
      if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
        event.preventDefault();
        runQuery();
      }
    });

    document.addEventListener('visibilitychange', function () {
      if (document.visibilityState === 'visible') {
        refreshHealth();
      }
    });
  }

  function init() {
    renderQueryExamples();
    renderExplainExamples();
    wireEvents();
    refreshHealth();
    window.setInterval(refreshHealth, 30000);
    refreshModels();

    // URL shortcuts (handy for presentations / screenshots):
    //   ?demo=1            -> run the one-click H2 demo automatically
    //   &tab=reason        -> switch to a tab after the demo
    //   &explain=:emp/5    -> open the explanation tab and explain an individual
    try {
      var params = new URLSearchParams(window.location.search);
      var startTab = params.get('tab');
      var explainSubject = params.get('explain');
      if (params.get('demo') === '1') {
        window.setTimeout(function () {
          quickDemo($('#btn-quick-demo')).then(function () {
            if (startTab) {
              switchTab(startTab);
            }
            if (explainSubject) {
              switchTab('explain');
              $('#explain-subject').value = explainSubject;
              runExplain();
            }
          });
        }, 400);
      } else if (params.get('model')) {
        // arrived from the ontology-modeling page: select the generated model
        refreshModels(params.get('model'));
      } else if (explainSubject) {
        var timer = window.setInterval(function () {
          if (state.selectedId) {
            window.clearInterval(timer);
            switchTab('explain');
            $('#explain-subject').value = explainSubject;
            runExplain();
          }
        }, 300);
      }
    } catch (ignored) {
      // URLSearchParams not available: ignore
    }
  }

  document.addEventListener('DOMContentLoaded', init);
})();
