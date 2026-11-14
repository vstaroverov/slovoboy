import {Game} from '../dist/engine.js';import fs from 'node:fs';import crypto from 'node:crypto';import assert from 'node:assert/strict';
const {words,sets}=JSON.parse(fs.readFileSync(new URL('../dist/words.json',import.meta.url)));
assert.equal(words.length,360);assert.equal(new Set(words).size,360);
const array=a=>'['+a.join(', ')+']';
const frame=g=>[g.human,g.bot].map(p=>p.hp+array(p.hand)+(p.offer??'null')+p.replace+p.cards.map(c=>array(c.parts)+array(c.filled)).join('')).join('');
const refs=fs.readFileSync(new URL('frames.txt',import.meta.url),'utf8').trim().split(/\r?\n/);
for(const line of refs){const [level,seed,expected]=line.split(' ');let g=new Game(Number(seed),Number(level),words,sets),hash=crypto.createHash('sha256');for(let t=0;t<450;t++){hash.update(frame(g));let placed=false;for(let h=0;h<g.human.hand.length&&!placed;h++)for(let c=0;c<6&&!placed;c++)for(let s=0;s<g.human.cards[c].parts.length&&!placed;s++)if(!g.human.cards[c].filled[s]&&g.human.cards[c].parts[s]===g.human.hand[h])placed=g.place(h,c,s);if(t%7===0&&g.human.hand.length)g.offer(g.human.hand.length-1);if(t%17===0)g.requestCard(5);g.tick();g.continueRound();if(t===37)g=Game.restore(JSON.parse(g.serialize()),words,sets)}assert.equal(hash.digest('hex'),expected,`Android/web mismatch level ${level} seed ${seed}`)}
console.log('Android/web exact parity: 60 seeded duels × 450 frames, including save/restore, exchange, replacement and continuation.');
for(const level of [1,2]){let g=new Game(123,level,words,sets);g.human.hp=1;g.bot.hp=1;for(let t=0;t<1000&&!['win','lose'].includes(g.result);t++){g.tick();g.continueRound()}assert.ok(['win','lose'].includes(g.result));assert.equal(g.human.hp===0||g.bot.hp===0,true)}
console.log('Terminal results and zero-HP conditions passed.');
