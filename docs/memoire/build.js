#!/usr/bin/env node
/**
 * Construction du mémoire ITSN (.docx) à partir des sources Markdown de ./src.
 * - styles Word natifs (Titre 1/2/3) => sommaire, table des matières, listes des figures et tableaux générés par champs ;
 * - légendes numérotées (champs SEQ), numérotation automatique des sections de chapitre ;
 * - page de garde, pagination romaine (préliminaires) puis arabe (corps).
 * Usage : node build.js [sortie.docx]
 */
const fs = require('fs');
const path = require('path');
const { marked } = require('marked');
const sharp = require('sharp');
const D = require('docx');

const SRC = path.join(__dirname, 'src');
const OUT = process.argv[2] || path.join(__dirname, 'Memoire_ITSN_ITSM.docx');
const META = JSON.parse(fs.readFileSync(path.join(__dirname, 'meta.json'), 'utf8'));

const FONT = 'Calibri';
const PAGE_W = 11906, PAGE_H = 16838, MARGIN = 1418; // A4, marges 2,5 cm
const CONTENT_W = PAGE_W - 2 * MARGIN;             // 9070 DXA
const CONTENT_W_PX = Math.round((CONTENT_W / 1440) * 96); // ≈ 605 px

// ------------------------------------------------------------------ inline
function inline(tokens, base = {}) {
  const runs = [];
  for (const t of tokens || []) {
    switch (t.type) {
      case 'text':
      case 'escape':
        if (t.tokens) runs.push(...inline(t.tokens, base));
        else runs.push(new D.TextRun({ text: decode(t.text), ...base }));
        break;
      case 'strong': runs.push(...inline(t.tokens, { ...base, bold: true })); break;
      case 'em': runs.push(...inline(t.tokens, { ...base, italics: true })); break;
      case 'codespan': runs.push(new D.TextRun({ text: decode(t.text), font: 'Consolas', size: 19, ...base })); break;
      case 'link': runs.push(...inline(t.tokens, { ...base, color: '1F9D5A' })); break;
      case 'br': runs.push(new D.TextRun({ break: 1 })); break;
      case 'del': runs.push(...inline(t.tokens, { ...base, strike: true })); break;
      default:
        if (t.tokens) runs.push(...inline(t.tokens, base));
        else if (t.text) runs.push(new D.TextRun({ text: decode(t.text), ...base }));
    }
  }
  return runs;
}
const decode = (s) => String(s).replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&#64;/g, '@');

// ------------------------------------------------------------------ état
const state = { figure: 0, table: 0, chapter: 0, h2: 0, h3: 0, numbered: false, pendingTableCaption: null, lists: 0 };
const numberingConfigs = [];

function seqField(label, n) {
  return new D.SimpleField(`SEQ ${label} \\* ARABIC`, String(n));
}

function caption(label, n, text, before, after) {
  const src = /source|réalisation|capture/i.test(text) ? '' : ' (réalisation personnelle)';
  return new D.Paragraph({
    style: label === 'Tableau' ? 'CaptionTableau' : 'CaptionFigure', keepNext: label === 'Tableau', alignment: D.AlignmentType.CENTER,
    spacing: { before, after },
    children: [new D.TextRun({ text: `${label} `, bold: true }), seqField(label, n), new D.TextRun({ text: ` – ${text}${src}`, bold: label === 'Tableau' })],
  });
}

// ------------------------------------------------------------------ blocs
function heading(token) {
  const text = token.text.trim();
  if (token.depth === 1) {
    const m = text.match(/^Chapitre\s+(\d+)/i);
    state.chapter = m ? parseInt(m[1], 10) : 0;
    state.numbered = !!m; state.h2 = 0; state.h3 = 0;
    const isPart = /^Partie\s+\d/i.test(text);
    return new D.Paragraph({
      heading: D.HeadingLevel.HEADING_1, pageBreakBefore: false,
      alignment: isPart ? D.AlignmentType.CENTER : D.AlignmentType.LEFT,
      spacing: isPart ? { before: 4000, after: 400 } : undefined,
      children: inline(token.tokens),
    });
  }
  let prefix = '';
  if (token.depth === 2) { state.h2++; state.h3 = 0; if (state.numbered && !/^Section\s+\d/i.test(text)) prefix = `${state.chapter}.${state.h2} `; }
  if (token.depth === 3) { state.h3++; if (state.numbered) prefix = `${state.chapter}.${state.h2}.${state.h3} `; }
  const level = { 2: D.HeadingLevel.HEADING_2, 3: D.HeadingLevel.HEADING_3, 4: D.HeadingLevel.HEADING_4 }[token.depth] || D.HeadingLevel.HEADING_4;
  return new D.Paragraph({ heading: level, children: [...(prefix ? [new D.TextRun(prefix)] : []), ...inline(token.tokens)] });
}

function paragraph(token) {
  const text = token.text.trim();
  const cap = text.match(/^Tableau\s*:\s*(.+)$/s);
  if (cap) { state.pendingTableCaption = cap[1].replace(/\s+/g, ' '); return []; }
  return [new D.Paragraph({ style: 'Normal', alignment: D.AlignmentType.JUSTIFIED, children: inline(token.tokens) })];
}

async function image(token) {
  const alt = decode(token.text || '').trim();
  state.figure++;
  const out = [];
  if (token.href.startsWith('placeholder:')) {
    out.push(new D.Paragraph({
      alignment: D.AlignmentType.CENTER, spacing: { before: 200, after: 0 },
      border: { top: { style: D.BorderStyle.DASHED, size: 6, color: '9CA3AF' }, bottom: { style: D.BorderStyle.DASHED, size: 6, color: '9CA3AF' }, left: { style: D.BorderStyle.DASHED, size: 6, color: '9CA3AF' }, right: { style: D.BorderStyle.DASHED, size: 6, color: '81B5A1' } },
      shading: { type: D.ShadingType.CLEAR, fill: 'EEF7F2' },
      children: [new D.TextRun({ text: `[Capture d'écran — ${token.href.slice('placeholder:'.length).replace(/_/g, ' ')} — à insérer après exécution de l'application]`, italics: true, color: '6B7280' })],
    }));
  } else {
    const file = path.resolve(__dirname, token.href);
    const buf = fs.readFileSync(file);
    const meta = await sharp(buf).metadata();
    let w = CONTENT_W_PX, h = Math.round((meta.height / meta.width) * w);
    const maxH = Math.round((20 / 2.54) * 96); // 20 cm
    if (h > maxH) { h = maxH; w = Math.round((meta.width / meta.height) * h); }
    out.push(new D.Paragraph({ alignment: D.AlignmentType.CENTER, keepNext: true, spacing: { before: 200, after: 0, line: 240, lineRule: D.LineRuleType.AUTO },
      children: [new D.ImageRun({ type: 'png', data: buf, transformation: { width: w, height: h } })] }));
  }
  out.push(caption('Figure', state.figure, alt, 60, 240));
  return out;
}

function table(token) {
  const cols = token.header.length;
  const widths = colWidths(token);
  const small = cols >= 6;
  const size = small ? 17 : 19; // 8,5 pt / 9,5 pt
  const cell = (cellTok, isHeader, alt = false) => new D.TableCell({
    width: { size: widths[cellTok.__i], type: D.WidthType.DXA },
    shading: isHeader ? { type: D.ShadingType.CLEAR, fill: '032D42' } : (alt ? { type: D.ShadingType.CLEAR, fill: 'F3FAF6' } : undefined),
    margins: { top: 50, bottom: 50, left: 80, right: 80 },
    children: [new D.Paragraph({ spacing: { before: 0, after: 0, line: 240, lineRule: D.LineRuleType.AUTO },
      children: inline(cellTok.tokens, { size, bold: isHeader || undefined, color: isHeader ? 'FFFFFF' : undefined }) })],
  });
  const rows = [];
  rows.push(new D.TableRow({ tableHeader: true, children: token.header.map((c, i) => cell({ ...c, __i: i }, true)) }));
  token.rows.forEach((r, ri) => rows.push(new D.TableRow({ cantSplit: true,
    children: r.map((c, i) => cell({ ...c, __i: i }, false, ri % 2 === 1)) })));
  // bandes alternées
  const t = new D.Table({ width: { size: CONTENT_W, type: D.WidthType.DXA }, columnWidths: widths, rows,
    borders: borders('C9DED5') });
  const out = [];
  state.table++;
  out.push(caption('Tableau', state.table, state.pendingTableCaption || 'Tableau', 200, 80));
  state.pendingTableCaption = null;
  out.push(t);
  out.push(new D.Paragraph({ spacing: { before: 0, after: 160 }, children: [] }));
  return out;
}
function borders(color) {
  const b = { style: D.BorderStyle.SINGLE, size: 4, color };
  return { top: b, bottom: b, left: b, right: b, insideHorizontal: b, insideVertical: b };
}
function colWidths(token) {
  // largeur proportionnelle à la longueur moyenne du contenu, bornée
  const cols = token.header.length;
  const lens = token.header.map((h, i) => Math.max(8, h.text.length, ...token.rows.map((r) => Math.min(60, r[i].text.length))));
  const sum = lens.reduce((a, b) => a + b, 0);
  const raw = lens.map((l) => Math.max(900, Math.round((l / sum) * CONTENT_W)));
  const total = raw.reduce((a, b) => a + b, 0);
  const scaled = raw.map((w) => Math.round((w / total) * CONTENT_W));
  scaled[cols - 1] += CONTENT_W - scaled.reduce((a, b) => a + b, 0);
  return scaled;
}

function list(token, level = 0, ref = null) {
  const out = [];
  if (!ref) {
    if (token.ordered) { ref = `numbers-${++state.lists}`; numberingConfigs.push({ reference: ref, levels: [0, 1, 2].map((lvl) => ({ level: lvl, format: D.LevelFormat.DECIMAL, text: `%${lvl + 1}.`, alignment: D.AlignmentType.LEFT, style: { paragraph: { indent: { left: 720 * (lvl + 1), hanging: 360 } } } })) }); }
    else ref = 'bullets';
  }
  token.items.forEach((item, idx) => {
    const paras = [], sub = [];
    for (const t of item.tokens) {
      if (t.type === 'list') sub.push(t);
      else if (t.type === 'text' || t.type === 'paragraph') paras.push(...inline(t.tokens || [{ type: 'text', text: t.text }]));
    }
    out.push(new D.Paragraph({
      numbering: { reference: ref, level },
      alignment: D.AlignmentType.JUSTIFIED, spacing: { after: 60 },
      children: paras,
    }));
    sub.forEach((s) => out.push(...list(s, level + 1, s.ordered === token.ordered ? ref : null)));
  });
  return out;
}

function code(token) {
  const lines = token.text.split('\n');
  return [new D.Paragraph({
    spacing: { before: 120, after: 160, line: 230, lineRule: D.LineRuleType.AUTO },
    shading: { type: D.ShadingType.CLEAR, fill: 'EEF7F2' },
    border: { left: { style: D.BorderStyle.SINGLE, size: 12, color: '1F9D5A' } },
    keepLines: true,
    children: lines.flatMap((l, i) => [new D.TextRun({ text: l.length ? l : ' ', font: 'Consolas', size: 16 }), ...(i < lines.length - 1 ? [new D.TextRun({ break: 1 })] : [])]),
  })];
}

function blockquote(token) {
  const out = [];
  for (const t of token.tokens) {
    if (t.type === 'paragraph') out.push(new D.Paragraph({
      indent: { left: 567, right: 567 }, alignment: D.AlignmentType.JUSTIFIED, spacing: { before: 120, after: 120 },
      border: { left: { style: D.BorderStyle.SINGLE, size: 12, color: '81B5A1' } },
      children: inline(t.tokens, { italics: true }),
    }));
  }
  return out;
}

const pageBreak = () => new D.Paragraph({ children: [new D.PageBreak()] });

// ------------------------------------------------------------------ pages spéciales
function coverPage() {
  const c = (text, opts = {}, spacing = {}) => new D.Paragraph({ alignment: D.AlignmentType.CENTER, spacing: { after: 120, ...spacing }, children: [new D.TextRun({ text, font: FONT, ...opts })] });
  const children = [
    c(META.universite, { bold: true, size: 28 }, { before: 200 }),
    c(META.ecole, { size: 26 }),
    c(META.departement || '', { size: 22 }),
  ];
  if (META.logo && fs.existsSync(path.resolve(__dirname, META.logo))) {
    children.push(new D.Paragraph({ alignment: D.AlignmentType.CENTER, spacing: { before: 200, after: 200 },
      children: [new D.ImageRun({ type: 'png', data: fs.readFileSync(path.resolve(__dirname, META.logo)), transformation: { width: 140, height: 140 } })], spacing: { line: 240, lineRule: D.LineRuleType.AUTO } }));
  } else {
    children.push(c('[LOGO DE L\'ÉTABLISSEMENT]', { color: '9CA3AF', size: 20 }, { before: 300, after: 300 }));
  }
  children.push(
    new D.Paragraph({ alignment: D.AlignmentType.CENTER, spacing: { before: 200, after: 100, line: 240, lineRule: D.LineRuleType.AUTO },
      children: [new D.ImageRun({ type: 'png', data: fs.readFileSync(path.resolve(__dirname, 'itsn-wordmark.png')), transformation: { width: 360, height: 70 } })] }),
    c('Mémoire de fin de formation', { size: 24 }, { before: 200 }),
    c(`Licence professionnelle — ${META.parcours}`, { size: 24 }),
    c(`Code ${META.code}`, { size: 22 }, { after: 600 }),
    new D.Paragraph({ alignment: D.AlignmentType.CENTER, spacing: { before: 200, after: 200, line: 300, lineRule: D.LineRuleType.AUTO },
      border: { top: { style: D.BorderStyle.SINGLE, size: 12, color: '032D42' }, bottom: { style: D.BorderStyle.SINGLE, size: 12, color: '1F9D5A' } },
      children: [new D.TextRun({ text: META.titre, bold: true, size: 40, font: FONT, color: '032D42' })] }),
    c(META.sousTitre, { italics: true, size: 26, color: '1F9D5A' }, { after: 800 }),
    c('Présenté par', { size: 22 }, { before: 400 }),
    ...META.etudiants.map((e) => c(e, { bold: true, size: 26 })),
    c('Sous la direction de', { size: 22 }, { before: 300 }),
    c(META.encadreur, { bold: true, size: 26 }),
    c(`Année académique ${META.annee}`, { size: 24 }, { before: 600 }),
  );
  return children;
}

function tocField(title, opts) {
  return [new D.Paragraph({ heading: D.HeadingLevel.HEADING_1, children: [new D.TextRun(title)] }),
    new D.TableOfContents(title, opts)];
}

// ------------------------------------------------------------------ assemblage
// Les images nécessitent un traitement asynchrone : on pré-transforme les tokens.
async function preprocessImages(tokens) {
  const out = [];
  for (const tok of tokens) {
    if (tok.type === 'paragraph' && tok.tokens && tok.tokens.length === 1 && tok.tokens[0].type === 'image') {
      out.push({ type: 'figure', children: await image(tok.tokens[0]) });
    } else out.push(tok);
  }
  return out;
}

async function main() {
  const files = fs.readdirSync(SRC).filter((f) => f.endsWith('.md')).sort();
  const md = files.map((f) => fs.readFileSync(path.join(SRC, f), 'utf8')).join('\n\n');
  const tokens = await preprocessImages(marked.lexer(md));

  const front = [], body = [];
  let cur = front;
  const push = (x) => cur.push(...(Array.isArray(x) ? x : [x]));

  for (const tok of tokens) {
    if (tok.type === 'paragraph' && /^<<<(\w+)>>>$/.test(tok.text.trim())) {
      const marker = tok.text.trim().slice(3, -3);
      if (marker === 'pagebreak') push(pageBreak());
      else if (marker === 'listes') { push(pageBreak()); push(tocField('Liste des tableaux', { stylesWithLevels: [{ styleName: 'Legende tableau', level: 1 }], hyperlink: true, useAppliedParagraphOutlineLevel: false })); push(pageBreak()); push(tocField('Liste des figures', { stylesWithLevels: [{ styleName: 'Legende figure', level: 1 }], hyperlink: true, useAppliedParagraphOutlineLevel: false })); }
      else if (marker === 'sommaire') { push(pageBreak()); push(tocField('Sommaire', { headingStyleRange: '1-2', hyperlink: true })); }
      else if (marker === 'body') { cur = body; }
      else if (marker === 'tdm') { push(tocField('Table des matières', { headingStyleRange: '1-3', hyperlink: true })); }
      continue;
    }
    switch (tok.type) {
      case 'heading': push(heading(tok)); break;
      case 'paragraph': push(paragraph(tok)); break;
      case 'figure': push(tok.children); break;
      case 'table': push(table(tok)); break;
      case 'list': push(list(tok)); break;
      case 'code': push(code(tok)); break;
      case 'blockquote': push(blockquote(tok)); break;
      case 'hr': push(pageBreak()); break;
      case 'space': case 'html': break;
      default: console.warn('Bloc non géré :', tok.type);
    }
  }

  const footer = (label) => new D.Footer({ children: [new D.Paragraph({ alignment: D.AlignmentType.CENTER, children: [
    new D.TextRun({ text: `${META.titreCourt} — `, size: 18, color: '6B7280' }), new D.TextRun({ children: [D.PageNumber.CURRENT], size: 18, color: '6B7280' })] })] });
  const header = new D.Header({ children: [new D.Paragraph({ alignment: D.AlignmentType.RIGHT, border: { bottom: { style: D.BorderStyle.SINGLE, size: 6, color: '1F9D5A' } },
    children: [new D.TextRun({ text: 'Plateforme de gestion des interventions techniques (ITSM) — ITSN', size: 16, color: '6B7280', italics: true })] })] });

  const pageProps = { size: { width: PAGE_W, height: PAGE_H }, margin: { top: MARGIN, bottom: MARGIN, left: MARGIN, right: MARGIN } };

  const hStyle = (level, size, color, before, after, extra = {}) => ({ run: { font: FONT, size, bold: true, color, ...extra }, paragraph: { outlineLevel: level, spacing: { before, after, line: 276, lineRule: D.LineRuleType.AUTO }, keepNext: true, keepLines: true } });

  const doc = new D.Document({
    creator: META.etudiants.join(', '),
    title: META.titre,
    description: META.sousTitre,
    features: { updateFields: true },
    styles: {
      default: { document: { run: { font: FONT, size: 22 }, paragraph: { spacing: { line: 276, after: 120 } } } },
      paragraphStyles: [
        { id: 'Normal', name: 'Normal', run: { font: FONT, size: 22 }, paragraph: { spacing: { line: 276, after: 120 } } },
        { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...hStyle(0, 34, '032D42', 480, 240, {}) },
        { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...hStyle(1, 28, '1F9D5A', 360, 160) },
        { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...hStyle(2, 24, '032D42', 240, 120) },
        { id: 'Heading4', name: 'Heading 4', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...hStyle(3, 22, '4B6B5E', 200, 80, { italics: true }) },
        { id: 'CaptionFigure', name: 'Legende figure', basedOn: 'Normal', next: 'Normal', run: { font: FONT, size: 19, color: '032D42', italics: true }, paragraph: { spacing: { before: 60, after: 240 } } },
        { id: 'CaptionTableau', name: 'Legende tableau', basedOn: 'Normal', next: 'Normal', run: { font: FONT, size: 19, color: '032D42', italics: true }, paragraph: { spacing: { before: 200, after: 80 }, keepNext: true } },
      ],
    },
    numbering: {
      config: [
        { reference: 'bullets', levels: [0, 1, 2].map((lvl) => ({ level: lvl, format: D.LevelFormat.BULLET, text: ['•', '–', '·'][lvl], alignment: D.AlignmentType.LEFT, style: { paragraph: { indent: { left: 720 * (lvl + 1), hanging: 360 } } } })) },
        ...numberingConfigs,
      ],
    },
    sections: [
      { properties: { page: pageProps, titlePage: true }, children: coverPage() },
      { properties: { page: { ...pageProps, pageNumbers: { start: 1, formatType: D.NumberFormat.LOWER_ROMAN } } }, headers: { default: header }, footers: { default: footer() }, children: front },
      { properties: { page: { ...pageProps, pageNumbers: { start: 1, formatType: D.NumberFormat.DECIMAL } } }, headers: { default: header }, footers: { default: footer() }, children: body },
    ],
  });

  const buf = await D.Packer.toBuffer(doc);
  fs.writeFileSync(OUT, buf);
  console.log(`Écrit : ${OUT} (${(buf.length / 1024).toFixed(0)} Ko) — ${state.figure} figures, ${state.table} tableaux`);
}

main().catch((e) => { console.error(e); process.exit(1); });
