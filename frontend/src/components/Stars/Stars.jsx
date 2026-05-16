import './Stars.css';

export default function Stars({ rating, size = 'sm' }) {
         const n = Number(rating) || 0;
         const rounded = Math.round(n * 2) / 2;  // 0, 0.5, 1, 1.5, ..., 5
         const cells = [1, 2, 3, 4, 5].map(i => {
                 if (rounded >= i) return { key: i, ch: '★', cls: 'stars__glyph stars__glyph--full' };
                 if (rounded + 0.5 === i) return { key: i, ch: '½', cls: 'stars__glyph stars__glyph--half' };
                 return { key: i, ch: '☆', cls: 'stars__glyph stars__glyph--empty'};
                 });
         return (
             <span className={`stars stars--${size}`} aria-label={`${n.toFixed(1)} out of 5 stars`}>
                     {cells.map(c => <span key={c.key} className={c.cls}>{c.ch}</span>)}
             </span>
             );
}