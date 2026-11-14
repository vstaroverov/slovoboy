# V-STAR Games · знак

Основа: дизайн-система V-STAR Group 2.3.0, product logo prompts, palette/tokens. Корпоративный логотип группы не изменяется. Самостоятельный знак игры: звезда с негативной V; плоская геометрия, два цвета, без градиентов и свечения.

- Лайм #D8FF18, тёмный сине-зелёный #061014, надпись #F6FBFF, подпись #9EB1B8.
- Типографика Manrope. Шрифт и лицензия OFL включены локально; внешних запросов к Google Fonts нет.
- v-star-games-icon.svg — настоящий вектор для иконки.
- v-star-games-mark.svg / .png — знак на прозрачном фоне.
- v-star-games-mono.svg — одна краска для светлой подложки.
- v-star-games-lockup.svg / .png — горизонтальная композиция с названием и подписью by V-STAR Group. SVG содержит контуры букв и не зависит от установленных шрифтов.
- PNG иконки 192/512 находятся в docs/ и web/dist/. В Android — VectorDrawable.

Геометрия задаётся tools/BrandAssets.java; генерация: javac -d build-brand tools/BrandAssets.java; java -cp build-brand BrandAssets . app/src/main/assets/fonts/Manrope-variable.ttf. UI использует те же координаты.
