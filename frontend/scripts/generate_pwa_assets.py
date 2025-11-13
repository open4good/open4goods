"""Utility script to regenerate Nudger PWA icons and screenshots."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

BASE_DIR = Path(__file__).resolve().parents[1] / 'app' / 'public' / 'pwa-assets'
ICONS_DIR = BASE_DIR / 'icons'
MASKABLE_DIR = BASE_DIR / 'icons-maskable'
SCREENS_DIR = BASE_DIR / 'screenshots'
SIZES = [72, 96, 128, 144, 152, 180, 192, 256, 384, 512, 1024]
BRAND_COLORS = ['#00DE9F', '#00D1CE', '#0088D6']
TEXT = 'N'
FONT_PATH = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'


def ensure_directories() -> None:
  for directory in (ICONS_DIR, MASKABLE_DIR, SCREENS_DIR):
    directory.mkdir(parents=True, exist_ok=True)


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
  try:
    return ImageFont.truetype(FONT_PATH, size)
  except OSError:
    return ImageFont.load_default()


def render_icon(size: int, maskable: bool) -> Image.Image:
  image = Image.new('RGBA', (size, size), BRAND_COLORS[0])
  draw = ImageDraw.Draw(image)
  stripe_height = max(1, size // len(BRAND_COLORS))
  for idx, color in enumerate(BRAND_COLORS):
    draw.rectangle([(0, idx * stripe_height), (size, min(size, (idx + 1) * stripe_height))], fill=color)

  if not maskable:
    radius = max(8, size // 5)
    mask = Image.new('L', (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([(0, 0), (size, size)], radius=radius, fill=255)
    image.putalpha(mask)

  font = load_font(max(16, int(size * 0.55)))
  bbox = draw.textbbox((0, 0), TEXT, font=font)
  text_w = bbox[2] - bbox[0]
  text_h = bbox[3] - bbox[1]
  draw.text(((size - text_w) / 2, (size - text_h) / 2), TEXT, font=font, fill='white')

  return image


def generate_icons() -> None:
  for size in SIZES:
    render_icon(size, maskable=False).save(ICONS_DIR / f'nudger-standard-{size}x{size}.png')
    render_icon(size, maskable=True).save(MASKABLE_DIR / f'nudger-maskable-{size}x{size}.png')


def generate_screenshots() -> None:
  scenes = [
    (1280, 720, 'desktop-dashboard', 'Nudger dashboard', 'Impact scores, prix et tendances'),
    (720, 1280, 'mobile-search', 'Nudger mobile', 'Comparateur responsable sur mobile'),
  ]
  for width, height, slug, title, subtitle in scenes:
    image = Image.new('RGB', (width, height), '#F3FFFB')
    draw = ImageDraw.Draw(image)
    draw.rectangle([(0, 0), (width, height // 2)], fill=BRAND_COLORS[0])
    draw.rectangle([(0, height // 2), (width, height)], fill=BRAND_COLORS[-1])
    title_font = load_font(int(height * 0.08))
    subtitle_font = load_font(int(height * 0.045))
    title_bbox = draw.textbbox((0, 0), title, font=title_font)
    subtitle_bbox = draw.textbbox((0, 0), subtitle, font=subtitle_font)
    draw.text(((width - (title_bbox[2] - title_bbox[0])) / 2, height * 0.25), title, font=title_font, fill='white')
    draw.text(((width - (subtitle_bbox[2] - subtitle_bbox[0])) / 2, height * 0.65), subtitle, font=subtitle_font, fill='white')
    image.save(SCREENS_DIR / f'{slug}.png')


def main() -> None:
  ensure_directories()
  generate_icons()
  generate_screenshots()
  print(f'Regenerated icons in {ICONS_DIR}')
  print(f'Regenerated maskable icons in {MASKABLE_DIR}')
  print(f'Regenerated screenshots in {SCREENS_DIR}')


if __name__ == '__main__':
  main()
