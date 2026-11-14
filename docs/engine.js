// Offline rules shared with Android: RC3 level 1, RC5 level 2.
export class Random {
 constructor(seed=Date.now()){this.seed=(BigInt(seed)^0x5deece66dn)&((1n<<48n)-1n)}
 bits(n){this.seed=(this.seed*0x5deece66dn+11n)&((1n<<48n)-1n);return Number(this.seed>>BigInt(48-n))}
 int(n){if((n&-n)===n)return Math.floor(n*this.bits(31)/2147483648);let b,v;do{b=this.bits(31);v=b%n}while(b-v+n-1>=2147483648);return v}
 double(){return (this.bits(26)*134217728+this.bits(27))/9007199254740992}
 shuffle(a){for(let i=a.length;i>1;i--){const j=this.int(i);[a[i-1],a[j]]=[a[j],a[i-1]]}}
}
const card=split=>({parts:split.split('-'),filled:split.split('-').map(()=>false)});
const word=c=>c.parts.join(''),freq=(a,s)=>a.filter(x=>x===s).length;
const player=()=>({cards:[],hand:[],offer:null,lastReceived:null,receivedAt:-100,replace:-1,hp:30,wordsCompleted:0,damageDealt:0,roundWords:0,roundDamage:0});
export class Game {
 constructor(seed,level,catalog,sets,history={human:{seen:{},last:{},draws:0},bot:{seen:{},last:{},draws:0}}){
  Object.assign(this,{matchId:globalThis.crypto?.randomUUID?.()??`duel-${Date.now()}-${Math.random()}`,victoryRewarded:false,level,catalog,sets,history,random:new Random(seed),human:player(),bot:player(),bag:[],elapsed:0,totalElapsed:0,round:1,result:null,attackId:0,eventId:0,exchangeCount:0,usefulExchangeHuman:0});
  if(level===1){let first=this.random.int(sets.length);this.human.cards=sets[first].map(card);this.bot.cards=sets[(first+1)%sets.length].map(card)}else for(const p of [this.human,this.bot]){for(const n of [4,3,3,2,2,2])p.cards.push(this.select(p,n));p.cards=[...p.cards.slice(3),p.cards[1],p.cards[2],p.cards[0]]}
  for(const p of [this.human,this.bot]){p.hand.push(...p.cards[this.random.int(3)].parts);this.draw(p,6-p.hand.length)}
 }
 static restore(data,catalog,sets){let g=Object.assign(Object.create(Game.prototype),data);g.matchId??=globalThis.crypto?.randomUUID?.()??`legacy-${Date.now()}-${Math.random()}`;g.catalog=catalog;g.sets=sets;g.random=Object.assign(Object.create(Random.prototype),{seed:BigInt(data.random.seed)});return g}
 serialize(){return JSON.stringify(this,(k,v)=>typeof v==='bigint'?v.toString():k==='catalog'||k==='sets'?undefined:v)}
 select(p,n){
  let all=this.level===1?this.sets.flat():this.catalog;
  const choices=all.filter(s=>s.split('-').length===n&&!p.cards.some(c=>word(c)===s.replaceAll('-','')));
  if(this.level===1)return card(choices[this.random.int(choices.length)]);
  let history=p===this.human?this.history.human:this.history.bot;
  const weights=choices.map(s=>{let w=s.replaceAll('-',''),count=history.seen[w]||0,novel=count===0?12:Math.max(1,12/(1+count))*((history.last[w]!=null&&history.draws-history.last[w]<12)?.25:1);let shared=new Set(s.split('-').filter(t=>p.cards.some(c=>c.parts.length!==n&&c.parts.includes(t))));return novel*(1+Math.min(3,shared.size)*2)});
  let pick=this.random.double()*weights.reduce((a,b)=>a+b,0),i=choices.length-1;for(let j=0;j<choices.length;j++){pick-=weights[j];if(pick<0){i=j;break}}
  let c=card(choices[i]),w=word(c);history.seen[w]=Math.min(1000000,(history.seen[w]||0)+1);history.last[w]=++history.draws;return c;
 }
 token(){
  if(!this.bag.length){for(const p of [this.human,this.bot]){let owned={};for(const t of [...p.hand,...(p.offer?[p.offer]:[])])owned[t]=(owned[t]||0)+1;for(const c of p.cards)for(let s=0;s<c.parts.length;s++)if(!c.filled[s]){let t=c.parts[s];if(owned[t]>0)owned[t]--;else this.bag.push(t)}}if(!this.bag.length)for(const p of [this.human,this.bot])for(const c of p.cards)this.bag.push(...c.parts);this.random.shuffle(this.bag)}return this.bag.pop();
 }
 draw(p,n){while(n-->0&&p.hand.length+(p.offer?1:0)<12)p.hand.push(this.token())}
 complete(p,other,c,human){let old=p.cards[c];if(!old.filled.every(Boolean))return;this.lastAttackDamage=Math.min(other.hp,[0,0,1,3,5][old.parts.length]);other.hp-=this.lastAttackDamage;p.wordsCompleted++;p.damageDealt+=this.lastAttackDamage;p.roundWords++;p.roundDamage+=this.lastAttackDamage;this.lastAttackWord=word(old);this.lastAttackHuman=human;this.attackId++;p.cards[c]=this.select(p,old.parts.length);if(p.replace===c)p.replace=-1;this.bag=[];this.resolve()}
 place(h,c,s){let p=this.human;if(this.result||!p.hand[h]||p.cards[c].filled[s]||p.cards[c].parts[s]!==p.hand[h])return false;p.cards[c].filled[s]=true;p.hand.splice(h,1);this.complete(p,this.bot,c,true);return true}
 take(c,s){let p=this.human;if(this.result||p.hand.length+(p.offer?1:0)>=12||!p.cards[c].filled[s])return false;p.cards[c].filled[s]=false;p.hand.push(p.cards[c].parts[s]);return true}
 needs(p,t){return p.cards.reduce((a,c)=>a+c.parts.filter((s,i)=>!c.filled[i]&&s===t).length,0)>freq(p.hand,t)}
 exchange(){let h=this.human,b=this.bot;if(!h.offer||!b.offer)return;this.exchangeCount++;if(this.needs(h,b.offer))this.usefulExchangeHuman++;h.lastReceived=b.offer;b.lastReceived=h.offer;h.receivedAt=b.receivedAt=this.totalElapsed;h.hand.push(b.offer);b.hand.push(h.offer);h.offer=b.offer=null;this.eventId++}
 offer(i){if(this.result)return;let p=this.human;if(p.offer){if(p.hand.length<12){p.hand.push(p.offer);p.offer=null}return}if(i>=0&&i<p.hand.length)p.offer=p.hand.splice(i,1)[0];this.exchange()}
 discard(i){if(!this.result&&i>=0)this.human.hand.splice(i,1)}
 requestCard(c){if(this.result)return;this.human.replace=this.human.replace===c?-1:c;this.replaceCards()}
 replaceCards(){if(this.human.replace<0||this.bot.replace<0)return;for(const p of [this.human,this.bot]){let old=p.cards[p.replace];for(let s=0;s<old.parts.length;s++)if(old.filled[s]&&p.hand.length+(p.offer?1:0)<12)p.hand.push(old.parts[s]);p.cards[p.replace]=this.select(p,old.parts.length);p.replace=-1}this.bag=[];this.eventId++}
 botAction(){
  const p=this.bot;let best=null,value=-Infinity;
  for(let h=0;h<p.hand.length;h++)for(let c=0;c<p.cards.length;c++){let card=p.cards[c],filled=0,missing=0,found=0,available={};for(const t of p.hand)available[t]=(available[t]||0)+1;for(let s=0;s<card.parts.length;s++)if(card.filled[s])filled++;else{missing++;if(available[card.parts[s]]>0){found++;available[card.parts[s]]--}}for(let s=0;s<card.parts.length;s++)if(!card.filled[s]&&card.parts[s]===p.hand[h]){let v=(found===missing?100:0)+filled*8+found*5-missing*3;if(v>value){value=v;best={h,c,s}}}}
  if(best){p.cards[best.c].filled[best.s]=true;p.hand.splice(best.h,1);this.complete(p,this.human,best.c,false);if(this.result)return}
  if(!p.offer&&p.hand.length){let h=p.hand.findIndex(t=>!(t===p.lastReceived&&this.totalElapsed-p.receivedAt<6)&&freq(p.hand,t)>p.cards.reduce((sum,c)=>sum+c.parts.filter((s,i)=>!c.filled[i]&&s===t).length,0));if(h>=0)p.offer=p.hand.splice(h,1)[0]}
  this.exchange();if(this.elapsed%6===0&&p.replace<0)for(let c=p.cards.length-1;c>=0;c--)if(p.cards[c].filled.every(b=>!b)){p.replace=c;break}this.replaceCards();
 }
 interval(){return this.level===1?10:Math.max(1,11-this.round)}
 resolve(){if(this.human.hp===0||this.bot.hp===0)this.result=this.bot.hp===0?'win':'lose';else if(this.elapsed>=90)this.result='round'}
 tick(){if(this.result)return;this.elapsed++;this.totalElapsed++;if(this.elapsed%2===0)this.botAction();if(this.result)return;if(this.elapsed%this.interval()===0){this.draw(this.human,1);this.draw(this.bot,1)}this.resolve()}
 continueRound(){if(this.result!=='round')return false;this.result=null;this.round++;this.elapsed=0;for(const p of [this.human,this.bot])p.roundWords=p.roundDamage=0;return true}
}
