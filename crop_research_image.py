from pathlib import Path
from PIL import Image

src = Path(r"C:\Users\Admin\Desktop\lpsx\.codex_work\vsxue_2.png")
out = src.parent / "vsxue_crops"
out.mkdir(exist_ok=True)
im = Image.open(src)
step = 900
for i, y in enumerate(range(0, im.height, step), 1):
    im.crop((0, y, im.width, min(y + step, im.height))).save(out / f"crop_{i:02d}.png")
print(im.size, len(list(out.glob('*.png'))))
