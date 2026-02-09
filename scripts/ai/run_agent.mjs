#!/usr/bin/env node
/**
 * Multi-provider agent runner
 * - Reads config, template, schema, context
 * - Calls provider API with structured output where available
 * - Writes normalized agent contract JSON to --out
 *
 * Secrets expected:
 *   OPENAI_API_KEY, ANTHROPIC_API_KEY, GEMINI_API_KEY
 */

import fs from "fs";
import path from "path";

function arg(name) {
  const idx = process.argv.indexOf(name);
  if (idx === -1) return null;
  return process.argv[idx + 1] ?? null;
}

const configPath = arg("--config");
const checkMode = process.argv.includes("--check");
const provider = (arg("--provider") || "").toLowerCase();
const state = (arg("--state") || "").toUpperCase();
const templatePath = arg("--template");
const schemaPath = arg("--schema");
const contextPath = arg("--context");
const outPath = arg("--out") || "ai_result.json";

if (!configPath) {
  console.error("Missing args. Required: --config");
  process.exit(2);
}

const cfg = JSON.parse(fs.readFileSync(configPath, "utf8"));

function assertPathExists(filePath, label) {
  if (!filePath) {
    throw new Error(`${label} path is missing`);
  }
  if (!fs.existsSync(filePath)) {
    throw new Error(`${label} path does not exist: ${filePath}`);
  }
}

function requiredSecretForProvider(p) {
  const kind = cfg.providers?.[p]?.kind;
  if (kind === "openai") return "OPENAI_API_KEY";
  if (kind === "anthropic") return "ANTHROPIC_API_KEY";
  if (kind === "gemini") return "GEMINI_API_KEY";
  return null;
}

if (checkMode) {
  const providersToValidate = provider
    ? [provider]
    : Object.keys(cfg.providers || {});

  const missingSecrets = [];
  const checks = [];

  for (const p of providersToValidate) {
    if (!cfg.providers?.[p]) {
      throw new Error(`Unknown provider in --check: ${p}`);
    }
    const secretName = requiredSecretForProvider(p);
    if (secretName && !process.env[secretName]) {
      missingSecrets.push(secretName);
    }
    checks.push({
      provider: p,
      model: cfg.providers[p].model,
      kind: cfg.providers[p].kind,
      secret: secretName,
      secret_present: Boolean(secretName && process.env[secretName])
    });
  }

  const requiredFiles = [
    { label: "config", path: configPath },
    { label: "template", path: templatePath },
    { label: "schema", path: schemaPath },
    { label: "context", path: contextPath }
  ].filter(entry => entry.path);

  for (const entry of requiredFiles) {
    assertPathExists(entry.path, entry.label);
  }

  const report = {
    ok: missingSecrets.length === 0,
    checked_at: new Date().toISOString(),
    providers: checks,
    missing_secrets: [...new Set(missingSecrets)]
  };

  fs.writeFileSync(outPath, JSON.stringify(report, null, 2));
  console.log(`Wrote ${outPath}`);
  if (missingSecrets.length > 0) {
    console.error(`Missing required secrets: ${[...new Set(missingSecrets)].join(", ")}`);
    process.exit(1);
  }
  process.exit(0);
}

if (!configPath || !provider || !state || !templatePath || !schemaPath || !contextPath) {
  console.error("Missing args. Required: --config --provider --state --template --schema --context --out");
  process.exit(2);
}

const provCfg = cfg.providers?.[provider];
if (!provCfg) {
  console.error(`Unknown provider: ${provider}`);
  process.exit(2);
}

const schema = JSON.parse(fs.readFileSync(schemaPath, "utf8"));
const template = fs.readFileSync(templatePath, "utf8");
const ctx = JSON.parse(fs.readFileSync(contextPath, "utf8"));

function safeString(x) {
  return typeof x === "string" ? x : JSON.stringify(x, null, 2);
}

function renderTemplate(tpl, context) {
  // Very small templating: {{KEY}} from context root
  return tpl.replace(/\{\{(\w+)\}\}/g, (_, key) => safeString(context[key] ?? ""));
}

function buildPrompt() {
  const rendered = renderTemplate(template, {
    ISSUE_TITLE: ctx.title,
    ISSUE_BODY: ctx.body,
    ISSUE_URL: ctx.url,
    ISSUE_AUTHOR: ctx.author,
    STATE: state,
    PROVIDER: provider
  });

  const conversation = [
    `# Context`,
    `Repo: ${ctx.repo.owner}/${ctx.repo.repo}`,
    `Issue/PR: #${ctx.number} (${ctx.is_pr ? "PR" : "Issue"})`,
    `Title: ${ctx.title}`,
    `URL: ${ctx.url}`,
    ``,
    `## Description`,
    ctx.body || "",
    ``,
    `## Recent comments (most recent last)`,
    ...ctx.comments.map(c => `- @${c.user} (${c.created_at}):\n${c.body}\n`),
    ``,
    `## Trigger`,
    `Event: ${ctx.triggered_by.event}`,
    `Actor: @${ctx.triggered_by.actor}`,
    ctx.triggered_by.comment_body ? `Comment:\n${ctx.triggered_by.comment_body}` : ""
  ].join("\n");

  const system = [
    "You are a GitHub automation agent.",
    "You MUST return only valid JSON matching the provided schema.",
    "Do not include Markdown fences.",
    "If you are unsure, set needs_human=true and explain why in comment_markdown."
  ].join(" ");

  const user = [
    rendered.trim(),
    "",
    "----",
    conversation
  ].join("\n");

  return { system, user };
}

function normalizeContract(obj) {
  const out = {
    comment_markdown: String(obj?.comment_markdown ?? ""),
    labels_add: Array.isArray(obj?.labels_add) ? obj.labels_add.map(String) : [],
    labels_remove: Array.isArray(obj?.labels_remove) ? obj.labels_remove.map(String) : [],
    next_state: String(obj?.next_state ?? state),
    needs_human: Boolean(obj?.needs_human)
  };
  if (!["UNDERSTANDING","PLANNING","READY_TO_CODE"].includes(out.next_state)) out.next_state = state;
  return out;
}

async function callOpenAI({ model, reasoning_effort, max_output_tokens }) {
  const key = process.env.OPENAI_API_KEY;
  if (!key) throw new Error("OPENAI_API_KEY is missing");
  const { system, user } = buildPrompt();

  const body = {
    model,
    instructions: system,
    input: user,
    max_output_tokens,
    // Structured Outputs via Responses API
    text: {
      format: {
        type: "json_schema",
        strict: true,
        schema
      }
    }
  };

  // If model supports reasoning effort, pass it (safe even if ignored for some models)
  if (reasoning_effort) {
    body.reasoning = { effort: reasoning_effort };
  }

  const resp = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${key}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });

  const data = await resp.json();
  if (!resp.ok) throw new Error(`OpenAI error ${resp.status}: ${JSON.stringify(data).slice(0,2000)}`);

  // The SDK exposes output_text; in REST, we read output_text if present, else concatenate output items.
  const jsonText = data.output_text ?? "";
  if (jsonText) return { jsonText, usage: data.usage ?? null };

  // Fallback: try to find text in output
  const outItems = Array.isArray(data.output) ? data.output : [];
  const msg = outItems.find(x => x.type === "message");
  const content = msg?.content || [];
  const t = content.find(p => p.type === "output_text")?.text;
  return { jsonText: t ?? "", usage: data.usage ?? null };
}

async function callAnthropic({ model, max_tokens, betas }) {
  const key = process.env.ANTHROPIC_API_KEY;
  if (!key) throw new Error("ANTHROPIC_API_KEY is missing");
  const { system, user } = buildPrompt();

  const body = {
    model,
    max_tokens,
    system,
    messages: [{ role: "user", content: user }],
    output_format: {
      type: "json_schema",
      schema
    }
  };

  const headers = {
    "x-api-key": key,
    "anthropic-version": "2023-06-01",
    "content-type": "application/json"
  };
  if (Array.isArray(betas) && betas.length) {
    headers["anthropic-beta"] = betas.join(",");
  }

  const resp = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers,
    body: JSON.stringify(body)
  });

  const data = await resp.json();
  if (!resp.ok) throw new Error(`Anthropic error ${resp.status}: ${JSON.stringify(data).slice(0,2000)}`);

  const block = Array.isArray(data.content) ? data.content.find(b => b.type === "text") : null;
  const jsonText = block?.text ?? "";
  return { jsonText, usage: data.usage ?? null };
}

async function callGemini({ model, max_output_tokens }) {
  const key = process.env.GEMINI_API_KEY;
  if (!key) throw new Error("GEMINI_API_KEY is missing");

  const { system, user } = buildPrompt();
  const prompt = `${system}\n\n${user}`;

  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(key)}`;

  const body = {
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      responseMimeType: "application/json",
      responseJsonSchema: schema,
      maxOutputTokens: max_output_tokens
    }
  };

  const resp = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });

  const data = await resp.json();
  if (!resp.ok) throw new Error(`Gemini error ${resp.status}: ${JSON.stringify(data).slice(0,2000)}`);

  const cand = Array.isArray(data.candidates) ? data.candidates[0] : null;
  const part = cand?.content?.parts?.[0];
  const jsonText = part?.text ?? "";
  return { jsonText, usage: data.usageMetadata ?? null };
}

async function main() {
  let jsonText = "";
  let usage = null;

  if (provCfg.kind === "openai") {
    const max_output_tokens = provCfg.max_output_tokens_by_state?.[state] ?? 900;
    const model = provCfg.model;
    const effort = provCfg.reasoning_effort || null;
    ({ jsonText, usage } = await callOpenAI({ model, reasoning_effort: effort, max_output_tokens }));
  } else if (provCfg.kind === "anthropic") {
    const max_tokens = provCfg.max_tokens_by_state?.[state] ?? 1000;
    const model = provCfg.model;
    const betas = provCfg.betas || [];
    ({ jsonText, usage } = await callAnthropic({ model, max_tokens, betas }));
  } else if (provCfg.kind === "gemini") {
    const max_output_tokens = provCfg.max_output_tokens_by_state?.[state] ?? 1000;
    const model = provCfg.model;
    ({ jsonText, usage } = await callGemini({ model, max_output_tokens }));
  } else {
    throw new Error(`Unsupported provider kind: ${provCfg.kind}`);
  }

  let obj;
  try {
    obj = JSON.parse(String(jsonText).trim());
  } catch (e) {
    obj = {
      comment_markdown: `⚠️ Provider returned non‑JSON output.\n\nRaw (truncated):\n\`\`\`\n${String(jsonText).slice(0,4000)}\n\`\`\``,
      labels_add: [cfg.labels?.needs_human || "agent:needs_human"],
      labels_remove: [],
      next_state: state,
      needs_human: true
    };
  }

  // Add lightweight usage info (optional) into comment if not already present
  const normalized = normalizeContract(obj);
  if (usage && normalized.comment_markdown && !normalized.comment_markdown.includes("Usage:")) {
    const u = safeString(usage);
    normalized.comment_markdown += `\n\n<details><summary>Usage</summary>\n\n\`\`\`json\n${u}\n\`\`\`\n</details>\n`;
  }

  fs.writeFileSync(outPath, JSON.stringify(normalized, null, 2));
  console.log(`Wrote ${outPath}`);
}

main().catch(err => {
  console.error(err?.stack || String(err));
  // Fallback contract
  const fail = {
    comment_markdown: `❌ Agent execution failed.\n\n\`\`\`\n${String(err?.stack || err).slice(0,4000)}\n\`\`\``,
    labels_add: ["agent:needs_human"],
    labels_remove: [],
    next_state: state,
    needs_human: true
  };
  fs.writeFileSync(outPath, JSON.stringify(fail, null, 2));
  process.exit(1);
});
