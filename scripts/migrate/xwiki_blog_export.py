#!/usr/bin/env python3
"""Export the XWiki 'Blog' space into Nuxt Content Markdown + local media.

AC1 of xwiki-blog-to-nuxt-content (.o4g/work/xwiki-blog-to-nuxt-content.yml): a repeatable
export that turns each Blog.BlogPostClass page into validated Markdown with stable slug,
language, author, dates, tags and state, plus its attachments saved as local media.

Input is either a raw XWiki .xar export (a zip of per-page XML documents, one per
`<web>/<name>.xml`) or an already-extracted directory of the same layout -- e.g. the file
produced by XWiki's own "Export as XAR" administration action. Only pages whose XWiki object
class is exactly `Blog.BlogPostClass` are exported; the Blog application's own template/code
pages and `Blog.CategoryClass` taxonomy pages are skipped.

Usage:
    python3 scripts/migrate/xwiki_blog_export.py --xar /path/to/xwiki-export.xar

Writes Markdown under frontend/content/blog/<lang>/<slug>.md and media under
frontend/public/images/blog/<slug>/, both relative to the repository root (two parents up
from this script), unless --out-content / --out-media override them.
"""

from __future__ import annotations

import argparse
import base64
import re
import sys
import tempfile
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CONTENT_DIR = ROOT / "frontend" / "content" / "blog"
DEFAULT_MEDIA_DIR = ROOT / "frontend" / "public" / "images" / "blog"

POST_CLASS = "Blog.BlogPostClass"

# Union of scripts/python/text_replacements.py's mapping (AGENTS.md 6.1) and
# frontend/config/lint-forbidden.json's rules; applied here so repeated exports
# are lint-clean by construction instead of needing a manual follow-up pass.
_ASCII_PUNCTUATION = {
    "—": "-",
    "–": "-",
    "«": '"',
    "»": '"',
    "…": "...",
    "‘": "'",
    "’": "'",
    "•": "-",
    "●": "-",
    "×": "x",
}


def normalize_ascii_punctuation(text: str) -> str:
    for source, target in _ASCII_PUNCTUATION.items():
        text = text.replace(source, target)
    return text


@dataclass
class Attachment:
    filename: str
    content_b64: str


@dataclass
class BlogPost:
    slug: str
    language: str
    title: str
    extract: str
    author: str
    category: list[str]
    publish_date: str | None
    modified_date: str | None
    hidden: bool
    published: bool
    image_filename: str | None
    body_wiki: str
    attachments: list[Attachment] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


def strip_xwiki_prefix(author: str) -> str:
    return author[6:] if author.startswith("XWiki.") else author


def epoch_ms_to_iso(value: str) -> str:
    import datetime

    return datetime.datetime.fromtimestamp(int(value) / 1000, tz=datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def xwiki_date_to_iso(value: str) -> str:
    """'2025-08-06 03:37:30.0' -> '2025-08-06T03:37:30Z' (dates in this export are UTC)."""
    import datetime

    cleaned = value.split(".")[0]
    dt = datetime.datetime.strptime(cleaned, "%Y-%m-%d %H:%M:%S")
    return dt.replace(tzinfo=datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_page(xml_path: Path) -> BlogPost | None:
    tree = ET.parse(xml_path)
    root = tree.getroot()

    obj = None
    for candidate in root.findall("object"):
        class_name = candidate.findtext("className")
        if class_name == POST_CLASS:
            obj = candidate
            break
    if obj is None:
        return None

    props: dict[str, ET.Element] = {}
    for prop in obj.findall("property"):
        for child in prop:
            props[child.tag] = child

    def prop_text(name: str, default: str = "") -> str:
        el = props.get(name)
        return (el.text or default) if el is not None else default

    category = [
        (v.text or "").removeprefix("Blog.")
        for v in props.get("category", ET.Element("category")).findall("value")
        if v.text
    ]

    raw_author = root.findtext("author") or ""
    slug = root.findtext("name") or xml_path.stem
    language = root.findtext("defaultLanguage") or "fr"

    publish_date_raw = prop_text("publishDate")
    publish_date = xwiki_date_to_iso(publish_date_raw) if publish_date_raw else None
    modified_raw = root.findtext("contentUpdateDate") or root.findtext("date")
    modified_date = epoch_ms_to_iso(modified_raw) if modified_raw else None

    attachments = [
        Attachment(filename=a.findtext("filename") or "", content_b64=a.findtext("content") or "")
        for a in root.findall("attachment")
    ]

    post = BlogPost(
        slug=slug,
        language=language,
        title=prop_text("title") or slug,
        extract=prop_text("extract"),
        author=strip_xwiki_prefix(raw_author),
        category=category,
        publish_date=publish_date,
        modified_date=modified_date,
        hidden=prop_text("hidden", "0") == "1",
        published=prop_text("published", "0") == "1",
        image_filename=prop_text("image") or None,
        body_wiki=prop_text("content"),
        attachments=attachments,
    )
    if not publish_date:
        post.warnings.append("missing publishDate")
    if post.image_filename and not any(a.filename == post.image_filename for a in attachments):
        post.warnings.append(f"image property '{post.image_filename}' has no matching attachment")
    return post


# ---------------------------------------------------------------------------
# XWiki 2.1 wiki syntax -> Markdown (covers the constructs actually present in
# the Blog space: headings, bold, italic, bullet/numbered lists, links, tables,
# horizontal rules; unrecognized constructs are left as-is and flagged).
# ---------------------------------------------------------------------------

_HEADING_RE = re.compile(r"^(=+)\s*(.*?)\s*=+\s*$")
_LIST_ITEM_RE = re.compile(r"^(\*+|1+\.)\s+(.*)$")
_LINK_RE = re.compile(r"\[\[(.*?)>>(.*?)\]\]")
_TABLE_ROW_RE = re.compile(r"^\s*\|(.+)\|?\s*$")


def _convert_link(match: re.Match, warnings: list[str]) -> str:
    label, target = match.group(1), match.group(2)
    # strip a trailing ||attr="..." parameter block XWiki allows on links
    target = target.split("||", 1)[0].strip()
    for scheme in ("url:", "https:", "http:"):
        if target.startswith(scheme):
            url = target if scheme in ("https:", "http:") else target[len("url:"):]
            return f"[{_convert_inline(label, warnings)}]({url})"
    if target.startswith("path:"):
        warnings.append(f"unresolved 'path:' link target left as plain text: {target!r}")
        return _convert_inline(label, warnings)
    # bare internal wiki reference (no scheme): keep as text, flag for manual review
    warnings.append(f"internal wiki link target left as plain text: {target!r}")
    return _convert_inline(label, warnings)


def _convert_inline(text: str, warnings: list[str]) -> str:
    text = _LINK_RE.sub(lambda m: _convert_link(m, warnings), text)
    text = re.sub(r"(?<!/)//(?!/)([^/]+?)(?<!/)//(?!/)", r"_\1_", text)
    text = re.sub(r"(?<!-)--(?!-)([^-]+?)(?<!-)--(?!-)", r"~~\1~~", text)
    text = re.sub(r"__([^_]+?)__", r"<u>\1</u>", text)
    return text


def convert_body(wiki_text: str, warnings: list[str]) -> str:
    lines = wiki_text.replace("\r\n", "\n").split("\n")
    out: list[str] = []
    table_buffer: list[list[str]] = []

    def flush_table():
        if not table_buffer:
            return
        header, *rows = table_buffer
        out.append("| " + " | ".join(_convert_inline(c, warnings) for c in header) + " |")
        out.append("| " + " | ".join("---" for _ in header) + " |")
        for row in rows:
            out.append("| " + " | ".join(_convert_inline(c, warnings) for c in row) + " |")
        table_buffer.clear()

    for raw_line in lines:
        line = raw_line.rstrip()

        table_match = _TABLE_ROW_RE.match(line)
        if table_match:
            table_buffer.append([c.strip() for c in table_match.group(1).split("|")])
            continue
        flush_table()

        if line.strip() == "----":
            out.append("---")
            continue

        heading_match = _HEADING_RE.match(line)
        if heading_match:
            level = min(len(heading_match.group(1)), 6)
            out.append("#" * level + " " + _convert_inline(heading_match.group(2), warnings))
            continue

        list_match = _LIST_ITEM_RE.match(line)
        if list_match:
            marker, rest = list_match.groups()
            depth = len(marker) if marker.endswith(".") is False else len(marker) - 1
            bullet = "-" if not marker.endswith(".") else "1."
            out.append("  " * (depth - 1) + f"{bullet} " + _convert_inline(rest, warnings))
            continue

        out.append(_convert_inline(line, warnings))

    flush_table()
    return "\n".join(out).strip() + "\n"


def _residual_syntax_warnings(markdown: str) -> list[str]:
    warnings = []
    if re.search(r"\[\[.*?>>.*?\]\]", markdown):
        warnings.append("residual XWiki-style [[label>>target]] link left unconverted")
    if re.search(r"^=+.*=+\s*$", markdown, re.M):
        warnings.append("residual '=' heading syntax left unconverted")
    if "{{" in markdown and "}}" in markdown:
        warnings.append("residual '{{macro}}' syntax found (not handled by this converter)")
    if markdown.count("~~") % 2 == 1:
        warnings.append("odd number of '~~' sequences -- likely literal source text (e.g. an author-typed "
                         "'approximately' tilde), not a strikethrough pair; will render as literal in Markdown")
    return warnings


def yaml_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def write_post(post: BlogPost, content_dir: Path, media_dir: Path, dry_run: bool) -> list[str]:
    warnings = list(post.warnings)
    body_md = convert_body(post.body_wiki, warnings)
    extract_md = _convert_inline(post.extract, warnings) if post.extract else ""
    warnings.extend(_residual_syntax_warnings(body_md))

    post_media_dir = media_dir / post.slug
    image_public_path = None
    for attachment in post.attachments:
        safe_name = re.sub(r"[^A-Za-z0-9._-]+", "-", attachment.filename).strip("-")
        target = post_media_dir / safe_name
        if not dry_run:
            post_media_dir.mkdir(parents=True, exist_ok=True)
            target.write_bytes(base64.b64decode(attachment.content_b64))
        if attachment.filename == post.image_filename:
            image_public_path = f"/images/blog/{post.slug}/{safe_name}"

    frontmatter_lines = [
        "---",
        f'title: "{yaml_escape(post.title)}"',
        f'description: "{yaml_escape(extract_md)}"',
        f"author: {post.author}",
        f"language: {post.language}",
        "tags:",
        *[f'  - "{yaml_escape(tag)}"' for tag in post.category],
        f"date: {post.publish_date}" if post.publish_date else "date: null",
        f"updatedAt: {post.modified_date}" if post.modified_date else "updatedAt: null",
        f"draft: {'true' if post.hidden else 'false'}",
        f"published: {'true' if post.published else 'false'}",
    ]
    if image_public_path:
        frontmatter_lines.append(f"image: {image_public_path}")
    frontmatter_lines.append("---")

    doc = normalize_ascii_punctuation("\n".join(frontmatter_lines) + "\n\n" + body_md)
    lang_dir = content_dir / post.language
    target_md = lang_dir / f"{post.slug}.md"
    if not dry_run:
        lang_dir.mkdir(parents=True, exist_ok=True)
        target_md.write_text(doc, encoding="utf-8")

    return warnings


def iter_blog_pages(root_dir: Path):
    blog_dir = root_dir / "Blog"
    if not blog_dir.is_dir():
        raise SystemExit(f"no 'Blog' space found under {root_dir}")
    for xml_path in sorted(blog_dir.glob("*.xml")):
        yield xml_path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--xar", required=True, type=Path, help="path to the .xar file or an already-extracted directory")
    parser.add_argument("--out-content", type=Path, default=DEFAULT_CONTENT_DIR)
    parser.add_argument("--out-media", type=Path, default=DEFAULT_MEDIA_DIR)
    parser.add_argument("--dry-run", action="store_true", help="parse and report without writing any file")
    args = parser.parse_args(argv)

    with tempfile.TemporaryDirectory() as tmp:
        if args.xar.is_dir():
            source_dir = args.xar
        else:
            source_dir = Path(tmp)
            with zipfile.ZipFile(args.xar) as zf:
                zf.extractall(source_dir)

        exported, skipped, all_warnings = 0, 0, []
        for xml_path in iter_blog_pages(source_dir):
            post = parse_page(xml_path)
            if post is None:
                skipped += 1
                continue
            warnings = write_post(post, args.out_content, args.out_media, args.dry_run)
            exported += 1
            for w in warnings:
                all_warnings.append(f"{post.slug}: {w}")

    print(f"exported {exported} posts, skipped {skipped} non-post pages")
    if all_warnings:
        print(f"\n{len(all_warnings)} warnings (manual review suggested):")
        for w in all_warnings:
            print(f"  - {w}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
