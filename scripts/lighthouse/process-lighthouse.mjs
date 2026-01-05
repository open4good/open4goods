import { promises as fs } from "node:fs";
import path from "node:path";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 1)
{
    const key = process.argv[index];
    const value = process.argv[index + 1];
    if (key?.startsWith("--") && value)
    {
        args.set(key, value);
        index += 1;
    }
}

const inputDir = args.get("--input-dir") ?? ".lighthouseci";
const outputDir = args.get("--output-dir") ?? "lighthouse";
const historyFile = args.get("--history-file") ?? path.join(outputDir, "HISTORY.md");
const latestDir = args.get("--latest-dir") ?? path.join(outputDir, "latest");
const badgesDir = args.get("--badges-dir") ?? path.join(outputDir, "badges");

const now = new Date();
const timestamp = now.toISOString().replace(/[:]/g, "-").replace(/\..+/, "");

const safeSlug = (url) =>
{
    try
    {
        const parsed = new URL(url);
        const raw = `${parsed.hostname}${parsed.pathname}`.replace(/\/+$/, "");
        return raw.replace(/[^a-zA-Z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "lighthouse";
    }
    catch
    {
        return "lighthouse";
    }
};

const formatScore = (value) => Math.round(value * 100);
const formatSeconds = (value) => (value / 1000).toFixed(2);
const formatMilliseconds = (value) => Math.round(value);

const pickColor = (value) =>
{
    if (value >= 90)
    {
        return "#4c1";
    }
    if (value >= 80)
    {
        return "#dfb317";
    }
    return "#e05d44";
};

const buildBadgeSvg = (label, value, color) =>
{
    const labelText = label;
    const valueText = value;
    const charWidth = 7;
    const labelWidth = labelText.length * charWidth + 10;
    const valueWidth = valueText.length * charWidth + 10;
    const totalWidth = labelWidth + valueWidth;
    const height = 20;
    const labelX = Math.round(labelWidth / 2);
    const valueX = labelWidth + Math.round(valueWidth / 2);

    return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${totalWidth}" height="${height}" role="img" aria-label="${labelText}: ${valueText}">
  <linearGradient id="s" x2="0" y2="100%">
    <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
    <stop offset="1" stop-opacity=".1"/>
  </linearGradient>
  <rect width="${labelWidth}" height="${height}" fill="#555"/>
  <rect x="${labelWidth}" width="${valueWidth}" height="${height}" fill="${color}"/>
  <rect width="${totalWidth}" height="${height}" fill="url(#s)"/>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,DejaVu Sans,sans-serif" font-size="11">
    <text x="${labelX}" y="14">${labelText}</text>
    <text x="${valueX}" y="14">${valueText}</text>
  </g>
</svg>`;
};

const ensureDir = async (dir) =>
{
    await fs.mkdir(dir, { recursive: true });
};

const readDirSafe = async (dir) =>
{
    try
    {
        return await fs.readdir(dir);
    }
    catch
    {
        return [];
    }
};

const findReports = async () =>
{
    const files = await readDirSafe(inputDir);
    const jsonFiles = files.filter((file) => file.startsWith("lhr-") && file.endsWith(".json"));
    const htmlFiles = files.filter((file) => file.endsWith(".html"));
    return { jsonFiles, htmlFiles };
};

const selectHtmlForJson = (jsonFile, htmlFiles) =>
{
    const base = jsonFile.replace(/\.json$/, "");
    const match = htmlFiles.find((file) => file.includes(base.replace(/^lhr-/, "")));
    if (match)
    {
        return match;
    }
    return htmlFiles.find((file) => file.replace(/\.html$/, "") === base) ?? null;
};

const historyHeader = [
    "| Date (UTC) | URL | Perf | Acc | BP | SEO | LCP (s) | INP (ms) | CLS | Report |",
    "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"
].join("\n");

const appendHistory = async (rows) =>
{
    const existing = await fs.readFile(historyFile, "utf8").catch(() => "");
    const content = existing.trim().length > 0 ? existing.trim() : historyHeader;
    const updated = `${content}\n${rows.join("\n")}\n`;
    await fs.writeFile(historyFile, updated, "utf8");
};

const updateBadges = async (scores) =>
{
    await ensureDir(badgesDir);
    const badges = [
        { label: "performance", value: scores.performance },
        { label: "accessibility", value: scores.accessibility },
        { label: "best-practices", value: scores.bestPractices },
        { label: "seo", value: scores.seo }
    ];

    await Promise.all(
        badges.map(async (badge) =>
        {
            const color = pickColor(badge.value);
            const svg = buildBadgeSvg(badge.label, `${badge.value}`, color);
            const badgePath = path.join(badgesDir, `${badge.label}.svg`);
            await fs.writeFile(badgePath, svg, "utf8");
        })
    );
};

const run = async () =>
{
    const { jsonFiles, htmlFiles } = await findReports();
    if (jsonFiles.length === 0)
    {
        throw new Error(`No Lighthouse JSON reports found in ${inputDir}`);
    }

    const runDir = path.join(outputDir, "history", timestamp);
    await ensureDir(runDir);
    await ensureDir(latestDir);

    const rows = [];
    let primaryScores = null;

    for (const jsonFile of jsonFiles)
    {
        const jsonPath = path.join(inputDir, jsonFile);
        const lhr = JSON.parse(await fs.readFile(jsonPath, "utf8"));
        const url = lhr.finalUrl || lhr.requestedUrl || "unknown";
        const slug = safeSlug(url);
        const jsonTarget = path.join(runDir, `${slug}.json`);
        const latestJson = path.join(latestDir, `${slug}.json`);

        await fs.copyFile(jsonPath, jsonTarget);
        await fs.copyFile(jsonPath, latestJson);

        const htmlMatch = selectHtmlForJson(jsonFile, htmlFiles);
        if (htmlMatch)
        {
            const htmlPath = path.join(inputDir, htmlMatch);
            const htmlTarget = path.join(runDir, `${slug}.html`);
            const latestHtml = path.join(latestDir, `${slug}.html`);
            await fs.copyFile(htmlPath, htmlTarget);
            await fs.copyFile(htmlPath, latestHtml);
        }

        const scores = {
            performance: formatScore(lhr.categories.performance.score),
            accessibility: formatScore(lhr.categories.accessibility.score),
            bestPractices: formatScore(lhr.categories["best-practices"].score),
            seo: formatScore(lhr.categories.seo.score)
        };

        const lcp = formatSeconds(lhr.audits["largest-contentful-paint"].numericValue);
        const inp = formatMilliseconds(lhr.audits["interaction-to-next-paint"].numericValue);
        const cls = lhr.audits["cumulative-layout-shift"].numericValue.toFixed(2);

        const reportLink = path.posix.join("history", timestamp, `${slug}.html`);

        rows.push(
            `| ${timestamp.replace("T", " ")} | ${url} | ${scores.performance} | ${scores.accessibility} | ${scores.bestPractices} | ${scores.seo} | ${lcp} | ${inp} | ${cls} | ${reportLink} |`
        );

        if (!primaryScores)
        {
            primaryScores = scores;
        }
    }

    await appendHistory(rows);
    if (primaryScores)
    {
        await updateBadges(primaryScores);
    }
};

run().catch((error) =>
{
    console.error(error);
    process.exit(1);
});
