#!/usr/bin/env python3
"""Render actual Java fixture metadata and separately generated visual samples as a standalone gallery."""
import base64
import hashlib
import html
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/character-v2'
samples = json.loads((OUT / 'samples.json').read_text())
manifest = {entry['id']: entry for entry in json.loads((OUT / 'image-manifest.json').read_text())}
report = (OUT / 'test-report.txt').read_text()
match = re.search(r'(\d+) tests successful', report)
passed = match.group(1) if match else '?'
CSS = '''
*{box-sizing:border-box}body{margin:0;background:#f5f4f1;color:#222327;font-family:-apple-system,BlinkMacSystemFont,'Apple SD Gothic Neo','Noto Sans KR',sans-serif}main{max-width:1160px;margin:auto;padding:38px 32px 24px}.eyebrow{font-size:11px;letter-spacing:2px;font-weight:800;color:#686570}.top{display:flex;justify-content:space-between;align-items:center}.status{font-size:12px;background:#e5f0e8;color:#285c3d;border:1px solid #d3e5d8;padding:8px 12px;border-radius:30px}h1{font-size:38px;letter-spacing:-1.8px;margin:16px 0 9px;font-weight:800}header p{color:#727279;font-size:14px;line-height:1.7;margin:0 0 23px}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.card{background:#fff;border:1px solid #e4e3df;border-radius:20px;overflow:hidden}.art{height:225px;position:relative;display:flex;align-items:center;justify-content:center}.art img{width:94%;height:94%;object-fit:contain}.number{position:absolute;top:15px;left:18px;font-size:11px;letter-spacing:1.2px;font-weight:700;color:#79737e}.genre{position:absolute;right:15px;top:13px;font-size:11px;background:#ffffffb0;padding:6px 10px;border-radius:15px;color:#5b5661}.content{padding:17px 21px 18px}h2{font-size:21px;letter-spacing:-.7px;margin:0 0 10px}.tags{display:flex;gap:6px;flex-wrap:wrap}.tag{font-size:11px;padding:5px 8px;border-radius:6px;background:#f3f2f5;color:#605c6c}.mood{margin-top:12px;color:#8a8791;font-size:11px}details{margin-top:10px;font-size:11px;color:#77727e}summary{cursor:pointer}pre{white-space:pre-wrap;font:11px/1.6 monospace;max-height:200px;overflow:auto;background:#f6f6f7;padding:12px;border-radius:9px}footer{margin-top:20px;font-size:11px;color:#88858d;line-height:1.8}.footline{display:flex;justify-content:space-between;gap:16px;border-top:1px solid #e2e0dc;padding-top:13px}@media(min-width:1450px){.grid{grid-template-columns:repeat(4,minmax(0,1fr))}.art{height:245px}.content{padding:17px 15px}h2{font-size:19px}}@media(max-width:620px){main{padding:25px 18px}.grid{grid-template-columns:1fr}h1{font-size:31px}.status{font-size:10px}.footline{display:block}}
'''
colors = ['#eee9f6','#f7f0dd','#e9edf4','#e8f2eb']
genres = {'ballad':'발라드','acoustic':'어쿠스틱','rock':'록','dance':'댄스'}
notes = ['새벽감성 · 조용히 음악에 빠진 순간','말랑말랑 · 포근한 리듬에 살랑살랑','플리과몰입 · 오늘만큼은 내가 헤드라이너','내적댄스 · 출근길도 나만의 페스티벌']
cards=[]
for i,sample in enumerate(samples):
    asset = manifest[sample['id']]
    if hashlib.sha256(sample['prompt'].encode()).hexdigest() != asset['promptSha256']:
        raise SystemExit('Preview images no longer match the exported prompts; regenerate visual samples first.')
    image = base64.b64encode((OUT / asset['file']).read_bytes()).decode()
    m=sample['metadata'];fixture=sample['fixture']
    tags=''.join('<span class="tag">'+html.escape(t)+'</span>' for t in m['tags'])
    cards.append('<article class="card"><div class="art" style="background:'+colors[i]+'"><span class="number">STAR / 0'+str(sample['id'])+'</span><span class="genre">'+genres[fixture['primaryGenre']]+'</span><img alt="'+html.escape(m['characterName'])+'" src="data:image/png;base64,'+image+'"></div><div class="content"><h2>'+html.escape(m['characterName'])+'</h2><div class="tags">'+tags+'</div><div class="mood">'+notes[i]+'</div><details><summary>생성 지시 보기</summary><pre>'+html.escape(sample['prompt'])+'</pre></details></div></article>')
page='<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Plitter · 캐릭터 V2 샘플</title><style>'+CSS+'</style></head><body><main><header><div class="top"><span class="eyebrow">PLITTER / CHARACTER LAB</span><span class="status">● 로컬 테스트 '+passed+'개 통과</span></div><h1>플리마다, 다른 나.</h1><p>같은 스타, 다른 텐션. 음악 취향에 맞춰 표정·포즈·소품과 이름·태그를 함께 골랐어요.</p></header><section class="grid">'+''.join(cards)+'</section><footer><div class="footline"><span>이름·태그·편집 지시: 구현한 Java V2 생성 코드의 고정 테스트 입력 결과</span><span>규칙 v2 / 후보 사전 v2</span></div><div>이미지: 해당 편집 지시와 기본 캐릭터를 사용한 이미지 생성 도구의 시각 샘플 · 실제 운영 OpenAI API·S3 연동 결과와는 다릅니다.</div></footer></main></body></html>'
(OUT / 'gallery.html').write_text(page)
print('Rendered standalone gallery:', OUT / 'gallery.html')

# A document-rendered contact sheet is also provided; it does not require browser file access.
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import HexColor
from reportlab.lib.utils import ImageReader
import subprocess

font_file=Path('/Library/Fonts/Arial Unicode.ttf')
if font_file.exists():
    pdfmetrics.registerFont(TTFont('GalleryKorean',str(font_file)))
    directory=ROOT/'tmp/pdfs';directory.mkdir(parents=True,exist_ok=True)
    pdf_path=directory/'character-v2-preview.pdf'
    c=canvas.Canvas(str(pdf_path),pagesize=(1100,1040))
    c.setTitle('Plitter Character V2 preview')
    c.setFillColor(HexColor('#f5f4f1'));c.rect(0,0,1100,1040,fill=1,stroke=0)
    def text(x,y,value,size=12,color='#222327',font='GalleryKorean'):
        c.setFont(font,size);c.setFillColor(HexColor(color));c.drawString(x,y,value)
    text(42,995,'PLITTER / CHARACTER LAB',12,'#6d6876','Helvetica-Bold')
    c.setFillColor(HexColor('#e1eee5'));c.roundRect(864,978,194,31,15,fill=1,stroke=0)
    text(882,989,'로컬 테스트 '+passed+'개 통과',12,'#285c3d')
    text(42,941,'플리마다, 다른 나.',37)
    text(43,910,'같은 스타, 다른 텐션. 음악 취향에 맞춰 이름과 표정, 포즈, 소품을 함께 골랐어요.',13,'#747079')
    for i,sample in enumerate(samples):
        x=42+(i%2)*518;y=510-(i//2)*393;w=498;h=373
        c.setFillColor(HexColor('#ffffff'));c.setStrokeColor(HexColor('#e3e0df'))
        c.roundRect(x,y,w,h,18,fill=1,stroke=1)
        c.saveState();path=c.beginPath();path.roundRect(x,y,w,h,18);c.clipPath(path,stroke=0,fill=0)
        c.setFillColor(HexColor(colors[i]));c.rect(x,y+129,w,244,fill=1,stroke=0)
        c.restoreState()
        asset=OUT/manifest[sample['id']]['file']
        c.drawImage(ImageReader(str(asset)),x+40,y+135,width=w-80,height=231,preserveAspectRatio=True,anchor='c',mask='auto')
        text(x+18,y+h-25,'STAR / 0'+str(sample['id']),10,'#80778a','Helvetica-Bold')
        text(x+w-69,y+h-25,genres[sample['fixture']['primaryGenre']],11,'#756a80')
        text(x+21,y+94,sample['metadata']['characterName'],23)
        tx=x+21
        for tag in sample['metadata']['tags']:
            tw=pdfmetrics.stringWidth(tag,'GalleryKorean',11)+16
            c.setFillColor(HexColor('#f1eff5'));c.roundRect(tx,y+50,tw,25,6,fill=1,stroke=0)
            text(tx+8,y+58,tag,11,'#625b73');tx+=tw+7
        text(x+21,y+23,notes[i],11,'#918897')
    c.setStrokeColor(HexColor('#dedbd7'));c.line(42,87,1058,87)
    text(42,65,'이름·태그·편집 지시: 구현한 Java V2 코드의 고정 테스트 입력 결과',11,'#77717d')
    text(42,44,'이미지: 해당 지시와 기본 캐릭터를 사용한 시각 샘플 · 운영 OpenAI API·S3 연동 결과는 별도 검증이 필요합니다.',10,'#8a838e')
    c.showPage();c.save()
    subprocess.run(['pdftoppm','-singlefile','-scale-to','1600','-png',str(pdf_path),str(OUT/'preview')],check=True)
    print('Rendered contact sheet:',OUT/'preview.png')
