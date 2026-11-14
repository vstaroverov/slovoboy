/** Wins required for each new personal level: 1, then 2, then 3... */
export class Progression {
 constructor(data={}){this.victories=Number.isInteger(data.victories)?Math.max(0,Math.min(20000000,data.victories)):0;this.lastVictory=typeof data.lastVictory==='string'?data.lastVictory:'';this.pendingLevel=Number.isInteger(data.pendingLevel)?Math.max(0,data.pendingLevel):0}
 level(){let n=Math.floor((Math.sqrt(1+8*this.victories)-1)/2);while((n+1)*(n+2)/2<=this.victories)n++;while(n*(n+1)/2>this.victories)n--;return n}
 xpInLevel(){const n=this.level();return (this.victories-n*(n+1)/2)*100}
 xpNeeded(){return (this.level()+1)*100}
 reward(id){if(!id||id===this.lastVictory||this.victories>=20000000)return false;const old=this.level();this.victories++;this.lastVictory=id;if(this.level()>old)this.pendingLevel=this.level();return true}
}
