# /// script
# requires-python = ">=3.10"
# dependencies = [
#   "zxing-cpp==3.1.1",                    # same decoder version as the app
#   "opencv-contrib-python-headless==4.14.*", # same OpenCV as the :wechatqr submodules
#   "pillow",
#   "segno",
#   "numpy",
# ]
# ///
"""Regenerates assets/scan_fixtures/*.png and verifies each one against the real decoders.

Run from the repo root:  mise exec uv@latest -- uv run app/src/androidTest/generate_scan_fixtures.py

small_blurred_qr.png is calibrated so that zxing-cpp alone fails but WeChatQRCode with the
bundled models succeeds; the script exits non-zero if no such setting is found.
"""
import io
import sys
from pathlib import Path

import cv2
import numpy as np
import segno
import zxingcpp
from PIL import Image, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "app/src/androidTest/assets/scan_fixtures"
MODELS = ROOT / "app/src/main/assets/wechat_qrcode"


def qr(text: str, scale: int = 10) -> Image.Image:
    buf = io.BytesIO()
    segno.make(text, error="m").save(buf, kind="png", scale=scale, border=4)
    return Image.open(io.BytesIO(buf.getvalue())).convert("L")


def barcode(text: str, fmt) -> Image.Image:
    img = zxingcpp.write_barcode(fmt, text, width=600, height=300)
    return ImageOps.expand(Image.fromarray(np.array(img)).convert("L"), border=40, fill=255)


def zxing_texts(img: Image.Image) -> list[str]:
    return [r.text for r in zxingcpp.read_barcodes(np.array(img), try_rotate=True, try_invert=True, try_downscale=True)]


wechat = cv2.wechat_qrcode_WeChatQRCode(
    *(str(MODELS / f) for f in ("detect.prototxt", "detect.caffemodel", "sr.prototxt", "sr.caffemodel"))
)


def wechat_texts(img: Image.Image) -> list[str]:
    return list(wechat.detectAndDecode(np.array(img))[0])


def blurred_qr(text: str) -> tuple[Image.Image, float, float]:
    """Smallest/blurriest-first search for a QR zxing-cpp can't read but WeChatQRCode can."""
    base = qr(text)
    for module_px in (3, 2.5, 2, 1.8, 1.6, 1.5):
        for blur in (0.6, 0.8, 1.0, 1.2, 1.4):
            side = round(base.width * module_px / 10)
            small = base.resize((side, side), Image.BILINEAR).filter(ImageFilter.GaussianBlur(blur))
            canvas = Image.new("L", (640, 480), 255)
            canvas.paste(small, (200, 150))
            if not zxing_texts(canvas) and text in wechat_texts(canvas):
                return canvas, module_px, blur
    sys.exit("no setting defeats zxing-cpp while WeChatQRCode still decodes")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    first, second = qr("first fixture"), qr("second fixture")
    two = Image.new("L", (first.width + second.width + 80, max(first.height, second.height)), 255)
    two.paste(first, (0, 0))
    two.paste(second, (first.width + 80, 0))
    blurred, module_px, blur = blurred_qr("https://example.com/blurred-fixture")

    fixtures = {
        "normal_qr.png": qr("https://example.com/"),
        "ean13.png": barcode("5901234123457", zxingcpp.BarcodeFormat.EAN13),
        "data_matrix.png": barcode("DataMatrix fixture", zxingcpp.BarcodeFormat.DataMatrix),
        "inverted_qr.png": ImageOps.invert(qr("inverted fixture")),
        "two_qr.png": two,
        "wechat_pay_qr.png": qr("wxp://f2f0eGmDYTyUj2nyz6vJMZeUOTW08oe39TEF"),
        "alipay_qr.png": qr("https://qr.alipay.com/fkx12345abcde"),
        "small_blurred_qr.png": blurred,
    }
    for name, img in fixtures.items():
        img.save(OUT / name)
        print(f"{name:22} zxing-cpp={zxing_texts(img)}")
    print(f"small_blurred_qr.png: module_px={module_px} blur={blur}, wechat={wechat_texts(blurred)}")


if __name__ == "__main__":
    main()
