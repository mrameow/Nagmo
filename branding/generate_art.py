#!/usr/bin/env python3
"""Generates Nagmo's mascot ("Nagmo", a chibi sticky note) as SVG files and
Android vector drawables from a single shape definition.

Run from the repo root:  python3 branding/generate_art.py
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SVG_DIR = os.path.join(ROOT, "branding")
DRAWABLE_DIR = os.path.join(ROOT, "app", "src", "main", "res", "drawable")

INK = "#4A3222"
BODY = "#FFD95A"
BODY_SHADE = "#F5B921"
TAPE = "#8FD9C3"
BLUSH = "#FF8FA3"
MOUTH = "#B23A48"
TONGUE = "#FF8FA3"
WHITE = "#FFFFFF"
MEGAPHONE = "#FF6B6B"


def ellipse(cx, cy, rx, ry):
    return (f"M{cx - rx:g},{cy:g} a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0 "
            f"a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0 Z")


def circle(cx, cy, r):
    return ellipse(cx, cy, r, r)


def fill(d, color, alpha=1.0):
    return {"d": d, "fill": color, "alpha": alpha}


def stroke(d, color, width, alpha=1.0, fill_color=None):
    return {"d": d, "stroke": color, "width": width, "alpha": alpha, "fill": fill_color}


BODY_PATH = "M30,18 H90 Q100,18 100,28 V80 L82,98 H30 Q20,98 20,88 V28 Q20,18 30,18 Z"
FOLD_PATH = "M100,80 L88,80 Q82,80 82,86 L82,98 Z"


def base(arms):
    shapes = [fill(ellipse(60, 111, 32, 4), "#000000", 0.12)]
    # feet
    shapes += [stroke(ellipse(46, 103, 7, 4.5), INK, 2.5, fill_color=BODY_SHADE),
               stroke(ellipse(74, 103, 7, 4.5), INK, 2.5, fill_color=BODY_SHADE)]
    shapes += arms
    shapes += [stroke(BODY_PATH, INK, 3, fill_color=BODY),
               # soft highlight along the top
               fill("M30,23 H84 Q92,23 92,29 V31 H28 V29 Q28,23 30,23 Z", WHITE, 0.35),
               stroke(FOLD_PATH, INK, 3, fill_color=BODY_SHADE),
               # washi tape
               fill("M46,11 L76,13 L74,25 L44,23 Z", TAPE, 0.9)]
    return shapes


def open_eyes():
    return [fill(ellipse(44, 56, 7, 9), INK), fill(ellipse(76, 56, 7, 9), INK),
            fill(circle(46.5, 52, 3), WHITE), fill(circle(78.5, 52, 3), WHITE),
            fill(circle(42, 60, 1.5), WHITE), fill(circle(74, 60, 1.5), WHITE)]


def blush():
    return [fill(ellipse(33, 68, 6, 3.5), BLUSH, 0.75), fill(ellipse(87, 68, 6, 3.5), BLUSH, 0.75)]


def happy():
    arms = [stroke("M22,66 Q12,66 9,58", INK, 6), stroke("M22,66 Q12,66 9,58", BODY, 3),
            stroke("M98,66 Q108,66 111,58", INK, 6), stroke("M98,66 Q108,66 111,58", BODY, 3)]
    face = open_eyes() + blush() + [
        stroke("M52,66 Q60,77 68,66 Q60,69 52,66 Z", INK, 2, fill_color=MOUTH),
        fill(ellipse(60, 71.5, 3.5, 1.8), TONGUE)]
    return base(arms) + face


def nagging():
    arms = [stroke("M22,68 Q12,72 10,80", INK, 6), stroke("M22,68 Q12,72 10,80", BODY, 3),
            stroke("M98,64 Q104,58 101,50", INK, 6), stroke("M98,64 Q104,58 101,50", BODY, 3)]
    megaphone = [
        stroke("M95,44 L104,40 L104,54 L95,50 Z", INK, 2.5, fill_color=MEGAPHONE),
        stroke("M104,40 L116,30 L116,64 L104,54 Z", INK, 2.5, fill_color=MEGAPHONE),
        fill("M106,41 L113,35.5 L113,40 L106,44.5 Z", WHITE, 0.5),
    ]
    brows = [stroke("M36,42 L50,46", INK, 3), stroke("M84,42 L70,46", INK, 3)]
    face = open_eyes() + blush() + brows + [
        stroke(ellipse(60, 71, 6, 6.5), INK, 2, fill_color=MOUTH),
        fill(ellipse(60, 74.5, 3.5, 2), TONGUE)]
    lines = [stroke("M100,24 L106,17", INK, 2.5), stroke("M110,22 L113,13", INK, 2.5),
             stroke("M90,22 L92,14", INK, 2.5)]
    return base(arms) + face + megaphone + lines


def sleepy():
    arms = [stroke("M22,72 Q14,78 16,86", INK, 6), stroke("M22,72 Q14,78 16,86", BODY, 3),
            stroke("M98,72 Q106,78 104,86", INK, 6), stroke("M98,72 Q106,78 104,86", BODY, 3)]
    face = [stroke("M37,57 Q44,63 51,57", INK, 3), stroke("M69,57 Q76,63 83,57", INK, 3)] + blush() + [
        stroke(ellipse(60, 70, 3, 2.5), INK, 2, fill_color=MOUTH)]
    zz = [stroke("M92,6 H102 L92,16 H102", INK, 2.5), stroke("M106,20 H113 L106,27 H113", INK, 2.2)]
    return base(arms) + face + zz


def party():
    arms = [stroke("M22,60 Q12,50 13,38", INK, 6), stroke("M22,60 Q12,50 13,38", BODY, 3),
            stroke("M98,60 Q108,50 107,38", INK, 6), stroke("M98,60 Q108,50 107,38", BODY, 3)]
    face = [stroke("M37,59 L44,51 L51,59", INK, 3.2), stroke("M69,59 L76,51 L83,59", INK, 3.2)] + blush() + [
        stroke("M48,65 Q60,84 72,65 Q60,68 48,65 Z", INK, 2, fill_color=MOUTH),
        fill(ellipse(60, 75, 5, 2.6), TONGUE)]
    def star(cx, cy, r, color):
        p = []
        import math
        for i in range(10):
            rad = r if i % 2 == 0 else r * 0.45
            a = math.pi / 2 + i * math.pi / 5
            p.append((cx + rad * math.cos(a), cy - rad * math.sin(a)))
        d = "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in p) + " Z"
        return fill(d, color)
    sparkles = [star(10, 22, 7, "#FF8FA3"), star(110, 18, 6, TAPE), star(104, 92, 5, "#9EC5FF"),
                star(14, 92, 4.5, "#FFB85C")]
    return base(arms) + face + sparkles


MASCOTS = {"happy": happy, "nagging": nagging, "sleepy": sleepy, "party": party}


def svg(shapes, size=120, background=None):
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" width="{size * 4}" height="{size * 4}">']
    if background:
        out.append(background)
    for s in shapes:
        attrs = [f'd="{s["d"]}"']
        attrs.append(f'fill="{s["fill"]}"' if s.get("fill") else 'fill="none"')
        if "stroke" in s:
            attrs += [f'stroke="{s["stroke"]}"', f'stroke-width="{s["width"]}"',
                      'stroke-linecap="round"', 'stroke-linejoin="round"']
        if s["alpha"] != 1.0:
            attrs.append(f'opacity="{s["alpha"]}"')
        out.append("  <path " + " ".join(attrs) + "/>")
    out.append("</svg>")
    return "\n".join(out) + "\n"


def vector_paths(shapes, indent="    "):
    out = []
    for s in shapes:
        attrs = [f'android:pathData="{s["d"]}"']
        if s.get("fill"):
            attrs.append(f'android:fillColor="{s["fill"]}"')
            if s["alpha"] != 1.0:
                attrs.append(f'android:fillAlpha="{s["alpha"]}"')
        if "stroke" in s:
            attrs += [f'android:strokeColor="{s["stroke"]}"', f'android:strokeWidth="{s["width"]}"',
                      'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
            if s["alpha"] != 1.0:
                attrs.append(f'android:strokeAlpha="{s["alpha"]}"')
        out.append(f"{indent}<path\n{indent}    " + f"\n{indent}    ".join(attrs) + " />")
    return "\n".join(out)


def vector(shapes, dp=120, viewport=120, group=None):
    body = vector_paths(shapes, "        " if group else "    ")
    if group:
        body = f"    <group {group}>\n{body}\n    </group>"
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated by branding/generate_art.py. Do not edit by hand. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{dp}dp"\n    android:height="{dp}dp"\n'
            f'    android:viewportWidth="{viewport}"\n    android:viewportHeight="{viewport}">\n'
            f"{body}\n</vector>\n")


def write(path, text):
    with open(path, "w") as f:
        f.write(text)


def main():
    os.makedirs(DRAWABLE_DIR, exist_ok=True)
    for name, fn in MASCOTS.items():
        shapes = fn()
        write(os.path.join(SVG_DIR, f"mascot_{name}.svg"), svg(shapes))
        write(os.path.join(DRAWABLE_DIR, f"mascot_{name}.xml"), vector(shapes))

    # Adaptive launcher icon foreground: mascot scaled into the 66dp safe zone.
    scale = 0.6
    offset = (108 - 120 * scale) / 2
    group = (f'android:scaleX="{scale}" android:scaleY="{scale}" '
             f'android:translateX="{offset:g}" android:translateY="{offset:g}"')
    write(os.path.join(DRAWABLE_DIR, "ic_launcher_foreground.xml"),
          vector(happy(), dp=108, viewport=108, group=group))

    # Monochrome (themed icon) and small status icon: silhouette with eye cut-outs.
    silhouette = " ".join([BODY_PATH, ellipse(44, 56, 7, 9), ellipse(76, 56, 7, 9),
                           "M52,67 Q60,76 68,67 Q60,70 52,67 Z"])
    mono = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated by branding/generate_art.py. Do not edit by hand. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="{dp}dp"\n    android:height="{dp}dp"\n'
            '    android:viewportWidth="{vp}"\n    android:viewportHeight="{vp}"{tint}>\n'
            '    <group {group}>\n'
            '        <path\n'
            '            android:fillType="evenOdd"\n'
            '            android:fillColor="#FFFFFFFF"\n'
            f'            android:pathData="{silhouette}" />\n'
            '    </group>\n</vector>\n')
    write(os.path.join(DRAWABLE_DIR, "ic_launcher_monochrome.xml"),
          mono.format(dp=108, vp=108, tint="", group=group))
    small = 'android:scaleX="0.2" android:scaleY="0.2" android:translateX="0" android:translateY="0.4"'
    write(os.path.join(DRAWABLE_DIR, "ic_stat_nagmo.xml"),
          mono.format(dp=24, vp=24, tint='\n    android:tint="?android:attr/colorControlNormal"', group=small))

    # Full-colour logo for the README.
    bg = '<rect x="0" y="0" width="120" height="120" rx="26" fill="#FFF3C4"/>'
    inner = [dict(s, d=s["d"]) for s in happy()]
    write(os.path.join(SVG_DIR, "logo.svg"), svg(inner, background=bg))


if __name__ == "__main__":
    main()
