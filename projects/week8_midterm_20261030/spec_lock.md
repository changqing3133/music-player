<!-- ppt-master-schema: spec-lock/v1 -->
# Execution Lock

## canvas
- viewBox: 0 0 1280 720
- format: PPT 16:9

## communication
- primary_language: zh-CN
- audience: 南京邮电大学《嵌入式手机终端开发技术》课程的任课老师与同班同学；老师按五个中期检查点判断进度，同学关注实现方式
- objective: 按五个中期检查点汇报中期进展，让老师确认项目已达到中期要求、功能有可演示的实物，并清楚剩余风险的负责人与闭环日期
- core_message: 必选 6/6、可选 1/1 已完成，代码已集成且构建可复现；真机回归是唯一未闭环项，11/07 前完成
- consumption_mode: balanced

## mode
- mode: instructional

## visual_style
- visual_style: blueprint

## colors
- background: #071A2B
- secondary_bg: #0C2740
- primary: #E6F7FF
- accent: #22D3EE
- secondary_accent: #FB7185
- body_text: #CBD5E1
- secondary_text: #7C93A9
- divider: #24455F

## typography
- font_family: Microsoft YaHei, Arial, sans-serif
- title_family: Microsoft YaHei, Arial, sans-serif
- body_family: Microsoft YaHei, Arial, sans-serif
- code_family: Consolas, monospace
- body: 24
- title: 48
- subtitle: 30
- display: 96
- annotation: 18
- code: 18
- footnote: 16

## icons
- library: tabler-outline
- stroke_width: 1.5
- inventory: tabler-outline/music, tabler-outline/device-mobile, tabler-outline/headphones, tabler-outline/player-play, tabler-outline/list-check, tabler-outline/circle-check, tabler-outline/stack-2, tabler-outline/users, tabler-outline/settings, tabler-outline/terminal, tabler-outline/git-branch, tabler-outline/brand-github, tabler-outline/package, tabler-outline/calendar, tabler-outline/alert-triangle, tabler-outline/wave-sine

## page_rhythm
- P01: anchor
- P02: breathing
- P03: breathing
- P04: dense
- P05: dense
- P06: dense
- P07: dense
- P08: dense
- P09: dense
- P10: anchor

## pptx_structure
- mode: flat

## forbidden
- `mask`, `<style>`, `class`, external CSS, `<foreignObject>`, `textPath`, `@font-face`, `<animate*>`, `<set>`, `<script>` / event attributes, `<iframe>`
- HTML named entities in text; write typography as raw Unicode and escape XML reserved characters
- 不使用照片或 AI 图片 (user)
- 全部元素保持可编辑，避免图片获取失败影响交付 (user)
