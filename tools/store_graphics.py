"""Draws store/icon-512.png and store/feature-graphic.png. Run from the repository root: python3 tools/store_graphics.py"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math, sys
FONTS='mobile_app/NabiUrRahmahApp/app/src/main/res/font/'
logo=Image.open('store/emblem-640.png').convert('RGBA')

icon=Image.new('RGBA',(640,640),(255,255,255,255)); icon.alpha_composite(logo)
icon.convert('RGB').resize((512,512),Image.LANCZOS).save('store/icon-512.png', optimize=True)

W,H=1024,500; S=2
img=Image.new('RGB',(W*S,H*S)); d=ImageDraw.Draw(img)
top=(0x5A,0x08,0x0E); bottom=(0x2A,0x03,0x06)
for y in range(H*S):
    t=y/(H*S); d.line([(0,y),(W*S,y)], fill=tuple(int(a+(b-a)*t) for a,b in zip(top,bottom)))
pat=Image.new('RGBA',img.size,(0,0,0,0)); pd=ImageDraw.Draw(pat)
step=120*S
def star(cx,cy,r):
    pts=[(cx+(r if i%2==0 else r*0.62)*math.cos(math.pi/8*i), cy+(r if i%2==0 else r*0.62)*math.sin(math.pi/8*i)) for i in range(16)]
    pd.polygon(pts, outline=(245,179,22,40), width=2*S)
for gx in range(-1, W*S//step+2):
    for gy in range(-1, H*S//step+2):
        star(gx*step+(step//2 if gy%2 else 0), gy*step, 44*S)
img=Image.alpha_composite(img.convert('RGBA'), pat)
glow=Image.new('RGBA',img.size,(0,0,0,0)); gd=ImageDraw.Draw(glow)
cx,cy,r=250*S,250*S,185*S
gd.ellipse([cx-r-30*S,cy-r-30*S,cx+r+30*S,cy+r+30*S], fill=(245,179,22,80))
img=Image.alpha_composite(img,glow.filter(ImageFilter.GaussianBlur(36*S)))
em=logo.resize((2*r,2*r),Image.LANCZOS)
mask=Image.new('L',em.size,0); ImageDraw.Draw(mask).ellipse([2,2,2*r-3,2*r-3],fill=255)
img.paste(em,(cx-r,cy-r),mask)
d=ImageDraw.Draw(img)
size=76
while True:
    title=ImageFont.truetype(FONTS+'inter_bold.ttf',size*S)
    saw=ImageFont.truetype(FONTS+'noto_naskh_arabic_bold.ttf',int(size*0.85)*S)
    width=d.textlength("Nabi ur Rahmah",font=title)+16*S+d.textlength("\uFDFA",font=saw)
    if width <= (W-490-44)*S: break
    size-=2
print('title size',size)
sub=ImageFont.truetype(FONTS+'inter_medium.ttf',30*S)
x=490*S
d.text((x,140*S),"Nabi ur Rahmah",font=title,fill=(255,255,255))
tw=d.textlength("Nabi ur Rahmah",font=title)
tb=d.textbbox((x,140*S),"Nabi ur Rahmah",font=title); sb=d.textbbox((0,0),"\uFDFA",font=saw)
d.text((x+tw+14*S,(tb[1]+tb[3])//2-(sb[1]+sb[3])//2),"\uFDFA",font=saw,fill=(255,217,138))
d.text((x,250*S),"The Prophet of Mercy",font=sub,fill=(255,217,138))
d.line([(x,306*S),(x+90*S,306*S)],fill=(245,179,22),width=4*S)
d.text((x,330*S),"Flyers · Videos · Books",font=sub,fill=(255,255,255))
d.text((x,374*S),"in many languages",font=sub,fill=(230,205,205))
img.convert('RGB').resize((W,H),Image.LANCZOS).save('store/feature-graphic.png', optimize=True)
