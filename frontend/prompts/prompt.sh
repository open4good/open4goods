#!/usr/bin/env bash
set -euo pipefail

VERSION="1.3"

usage() {
  cat <<'EOF'
prompt-exec — exécuteur de templates .prompt

USAGE
  prompt-exec [-i fichier.prompt] [-o sortie.txt] [-D KEY=VALUE ...]
              [--embed-files on|off] [--max-bytes N]
              [--non-interactive] [--clipboard] [--clipboard-only]

OPTIONS
  -i, --input FILE         Fichier .prompt d'entrée
  -o, --output FILE        Fichier de sortie (en plus de stdout)
  -D, --define KEY=VALUE   Définit une valeur de variable (répétable)
  --embed-files on|off     Injecter le contenu des fichiers référencés (défaut: on)
  --max-bytes N            Taille max par fichier injecté (défaut: 200000)
  --non-interactive        Ne pose pas de questions : échoue si une variable manque
  --clipboard              Copie le résultat dans le presse-papiers (best-effort)
  --clipboard-only         Copie dans le presse-papiers uniquement (ne sort rien sur stdout)
  -h, --help               Affiche cette aide
  -v, --version            Affiche la version

PLACEHOLDERS
  - {NAME} ou {{NAME}}
  - NAME: [A-Za-z_][A-Za-z0-9_-]*

ASTUCE SAISIE
  À la question d’une variable, tu peux répondre:
    @chemin/vers/fichier
  → la valeur sera le contenu de ce fichier.

NOTE
  Si un chemin de fichier "ressemble à un path" (contient / ou commence par / ./ ../ ~/)
  et qu’il est introuvable/non lisible, le script injecte:
    /////UNRESOLVED FILE CONTENT \\\\\\\
EOF
}

die() { echo "Erreur: $*" >&2; exit 1; }

INPUT=""
OUTPUT=""
EMBED_FILES="on"
MAX_BYTES=200000
NON_INTERACTIVE="no"
DO_CLIPBOARD="no"
CLIPBOARD_ONLY="no"

declare -A VARS

# --- Parse args ---
while [[ $# -gt 0 ]]; do
  case "$1" in
    -i|--input) [[ $# -ge 2 ]] || die "Option $1 nécessite un argument"; INPUT="$2"; shift 2;;
    -o|--output) [[ $# -ge 2 ]] || die "Option $1 nécessite un argument"; OUTPUT="$2"; shift 2;;
    -D|--define)
      [[ $# -ge 2 ]] || die "Option $1 nécessite KEY=VALUE"
      kv="$2"; [[ "$kv" == *"="* ]] || die "Format invalide pour -D: attendu KEY=VALUE"
      key="${kv%%=*}"; val="${kv#*=}"
      [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_-]*$ ]] || die "Nom de variable invalide: $key"
      VARS["$key"]="$val"
      shift 2;;
    --embed-files) [[ $# -ge 2 ]] || die "Option $1 nécessite on|off"; EMBED_FILES="$2"; [[ "$EMBED_FILES" =~ ^(on|off)$ ]] || die "--embed-files attend on|off"; shift 2;;
    --max-bytes) [[ $# -ge 2 ]] || die "Option $1 nécessite un entier"; MAX_BYTES="$2"; [[ "$MAX_BYTES" =~ ^[0-9]+$ ]] || die "--max-bytes attend un entier"; shift 2;;
    --non-interactive) NON_INTERACTIVE="yes"; shift;;
    --clipboard) DO_CLIPBOARD="yes"; shift;;
    --clipboard-only) DO_CLIPBOARD="yes"; CLIPBOARD_ONLY="yes"; shift;;
    -h|--help) usage; exit 0;;
    -v|--version) echo "$VERSION"; exit 0;;
    -*) die "Option inconnue: $1 (utilise --help)";;
    *) [[ -z "$INPUT" ]] && INPUT="$1" && shift || die "Argument inattendu: $1";;
  esac
done

# --- Input auto-detect ---
pick_input_interactive() {
  local prompts=()
  shopt -s nullglob
  prompts=( ./*.prompt )
  shopt -u nullglob

  if [[ ${#prompts[@]} -eq 0 ]]; then
    usage >&2
    die "Aucun fichier .prompt trouvé et aucun -i fourni."
  elif [[ ${#prompts[@]} -eq 1 ]]; then
    INPUT="${prompts[0]}"
  else
    usage >&2
    echo
    echo "Plusieurs fichiers .prompt détectés. Choisis-en un :"
    select f in "${prompts[@]}" "Annuler"; do
      [[ -n "${f:-}" ]] || { echo "Choix invalide." >&2; continue; }
      [[ "$f" == "Annuler" ]] && exit 1
      INPUT="$f"
      break
    done
  fi
}

[[ -z "$INPUT" ]] && pick_input_interactive
[[ -f "$INPUT" ]] || die "Fichier introuvable: $INPUT"
[[ -r "$INPUT" ]] || die "Fichier non lisible: $INPUT"

TEMPLATE="$(cat -- "$INPUT")"

# --- Extract placeholders {VAR} and {{VAR}} ---
mapfile -t PLACEHOLDERS < <(
  perl -0777 -ne '
    while (/\{\{?([A-Za-z_][A-Za-z0-9_-]*)\}\}?/g) { print "$1\n"; }
  ' <<<"$TEMPLATE" | sort -u
)

read_value_for() {
  local name="$1" v=""
  [[ "$NON_INTERACTIVE" == "yes" ]] && die "Variable manquante en mode non-interactif: $name"

  # shellcheck disable=SC2162
  read -r -p "Valeur pour ${name}: " v

  if [[ "$v" == @* ]]; then
    local f="${v#@}"
    [[ -f "$f" ]] || die "Référence @... introuvable: $f (pour $name)"
    [[ -r "$f" ]] || die "Référence @... non lisible: $f (pour $name)"
    local sz; sz=$(wc -c <"$f" | tr -d ' ')
    (( sz <= MAX_BYTES )) || die "Fichier trop gros pour @... ($sz > $MAX_BYTES): $f"
    v="$(cat -- "$f")"
  fi

  VARS["$name"]="$v"
}

for var in "${PLACEHOLDERS[@]}"; do
  [[ -z "${VARS[$var]+x}" ]] && read_value_for "$var"
done

# --- vars temp file (base64) ---
VARS_FILE="$(mktemp)"
trap 'rm -f -- "$VARS_FILE"' EXIT

for k in "${!VARS[@]}"; do
  b64="$(printf '%s' "${VARS[$k]}" | base64 | tr -d '\n')"
  printf '%s\t%s\n' "$k" "$b64" >>"$VARS_FILE"
done

# --- Render template ---
RENDERED="$(
  VARS_FILE="$VARS_FILE" perl -0777 -pe '
    use strict; use warnings;
    use MIME::Base64 qw(decode_base64);

    my $file = $ENV{VARS_FILE};
    my %m;

    if (defined $file && $file ne "" && -f $file) {
      open my $fh, "<", $file or die "cannot read vars file: $!";
      while (my $line = <$fh>) {
        chomp $line;
        next if $line eq "";
        my ($k, $b64) = split(/\t/, $line, 2);
        next unless defined $k and defined $b64;
        $m{$k} = decode_base64($b64);
      }
      close $fh;
    }

    s/\{\{?([A-Za-z_][A-Za-z0-9_-]*)\}\}?/exists $m{$1} ? $m{$1} : $&/ge;
  ' <<<"$TEMPLATE"
)"

# --- Embed files (python3) ---
embed_files_py() {
  local max_bytes="$1"
  python3 <(cat <<'PY'
import os, re, sys

MAX = int(sys.argv[1])
text = sys.stdin.read()


# Tokenize while preserving whitespace
parts = re.findall(r"\s+|\S+", text)

trail_re = re.compile(r"^(.*?)([)\],.;:!?]+)$")
quoted_with_trail = re.compile(r"""^(['"])(.+?)\1([)\],.;:!?]+)?$""")

UNRES_HEAD = "/////UNRESOLVED FILE CONTENT " + ("\\" * 5)

def is_url(tok: str) -> bool:
  return re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*://", tok) is not None

def looks_like_path(tok: str) -> bool:
  if is_url(tok):
    return False
  if tok.startswith(("~/", "./", "../", "/")):
    return True
  return "/" in tok

def unresolved_block(tok: str, reason: str) -> str:
  return f"\n\n{UNRES_HEAD}\nPATH: {tok}\nREASON: {reason}\n\n"

def embed_for_token(tok: str):
  path = os.path.expanduser(tok)

  try:
    st = os.stat(path)
  except FileNotFoundError:
    return unresolved_block(tok, "not found")
  except PermissionError:
    return unresolved_block(tok, "permission denied")
  except OSError as e:
    return unresolved_block(tok, f"os error: {e}")

  if not os.path.isfile(path):
    return unresolved_block(tok, "not a file")
  if st.st_size > MAX:
    return unresolved_block(tok, f"too large ({st.st_size} bytes > {MAX})")

  # binary-ish check
  try:
    with open(path, "rb") as f:
      head = f.read(4096)
      if b"\x00" in head:
        return unresolved_block(tok, "binary content (NUL detected)")
  except Exception as e:
    return unresolved_block(tok, f"cannot read (binary check): {e}")

  try:
    with open(path, "r", encoding="utf-8", errors="replace") as f:
      content = f.read()
  except Exception as e:
    return unresolved_block(tok, f"cannot read as text: {e}")

  return f"\n\n[BEGIN FILE: {tok}]\n{content}\n[END FILE: {tok}]\n\n"

out = []

for p in parts:
  if p.isspace():
    out.append(p)
    continue

  raw = p
  ql = qr = ""
  trail_after_quote = ""

  mqt = quoted_with_trail.match(raw)
  if mqt:
    ql = mqt.group(1)
    tok0 = mqt.group(2)
    trail_after_quote = mqt.group(3) or ""
    qr = ql
  else:
    tok0 = raw

  # detach punctuation after token
  m = trail_re.match(tok0)
  if m:
    tok, trail = m.group(1), m.group(2)
  else:
    tok, trail = tok0, ""

  if looks_like_path(tok):
    out.append(ql + embed_for_token(tok) + trail + qr + trail_after_quote)
  else:
    out.append(raw)

sys.stdout.write("".join(out))
PY
) "$max_bytes"
}

FINAL="$RENDERED"
if [[ "$EMBED_FILES" == "on" ]]; then
  FINAL="$(printf "%s" "$FINAL" | embed_files_py "$MAX_BYTES")"
fi

# --- Clipboard (best-effort, never abort stdout) ---
try_clip() {
  local name="$1"; shift
  local tmp; tmp="$(mktemp)"
  if "$@" 2>"$tmp"; then
    rm -f "$tmp"
    return 0
  fi
  local err=""; err="$(cat "$tmp" 2>/dev/null || true)"
  rm -f "$tmp"
  echo "Avertissement: copie clipboard via $name a échoué: ${err:-unknown error}" >&2
  return 1
}

copy_to_clipboard_best_effort() {
  # Wayland only if actually on Wayland
  if command -v wl-copy >/dev/null 2>&1 && [[ -n "${WAYLAND_DISPLAY:-}" ]]; then
    if try_clip "wl-copy" bash -c 'cat | wl-copy' ; then return 0; fi
  fi
  # X11
  if [[ -n "${DISPLAY:-}" ]]; then
    if command -v xclip >/dev/null 2>&1; then
      if try_clip "xclip" bash -c 'cat | xclip -selection clipboard' ; then return 0; fi
    fi
    if command -v xsel >/dev/null 2>&1; then
      if try_clip "xsel" bash -c 'cat | xsel --clipboard --input' ; then return 0; fi
    fi
  fi
  # macOS
  if command -v pbcopy >/dev/null 2>&1; then
    if try_clip "pbcopy" bash -c 'cat | pbcopy' ; then return 0; fi
  fi
  return 1
}

if [[ "$DO_CLIPBOARD" == "yes" ]]; then
  if ! printf "%s" "$FINAL" | copy_to_clipboard_best_effort; then
    echo "Avertissement: aucun backend clipboard utilisable. (Sur X11: installe xclip ou xsel.)" >&2
  fi
fi

# --- Output ---
if [[ -n "$OUTPUT" ]]; then
  out_dir="$(dirname -- "$OUTPUT")"
  [[ -d "$out_dir" ]] || mkdir -p -- "$out_dir"
  printf "%s" "$FINAL" >"$OUTPUT"
fi

if [[ "$CLIPBOARD_ONLY" != "yes" ]]; then
  printf "%s" "$FINAL"
fi
