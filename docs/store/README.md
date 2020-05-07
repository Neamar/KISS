# Usage

Regular `make` will just export the SVGs to PNGs.

# Compression

If you want crushed pngs with `pngcrush` you can use `make pngcrush`.

If you want tinyfied pngs with [tinypng](https://tinypng.com/) you can use `TINYPNG_API_KEY=YOURAPIKEY make tinypng`. Grab an API key for free at https://tinypng.com/.

## Performance

`pngcrush` compresses losslessly whereas tinypng "reduces" the quality of the PNG.


name | size | name | size | name | size
-----|------|------|------|------|-----
1.svg.png | 1.6M | 1.svg.crushed.png | 1.3M | 1.svg.tinypng.png | 632K
2.svg.png | 860K | 2.svg.crushed.png | 806K | 2.svg.tinypng.png | 286K
3.svg.png | 1.1M | 3.svg.crushed.png | 971K | 3.svg.tinypng.png | 341K
4.svg.png | 139K | 4.svg.crushed.png | 102K | 4.svg.tinypng.png |  48K
