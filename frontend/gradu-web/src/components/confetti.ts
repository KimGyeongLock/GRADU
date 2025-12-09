type ConfettiFn = (opts?: import("canvas-confetti").Options) => void;

let _confetti: ConfettiFn | null = null;

async function getConfetti(): Promise<ConfettiFn> {
  if (_confetti) return _confetti;
  const mod = await import("canvas-confetti");
  _confetti = mod.default;
  return _confetti!;
}

export async function fireConfetti(duration = 1800) {
  const confetti = await getConfetti();
  const end = Date.now() + duration;
  (function frame() {
    confetti({ particleCount: 5, angle: 60, spread: 65, origin: { x: 0 } });
    confetti({ particleCount: 5, angle: 120, spread: 65, origin: { x: 1 } });
    if (Date.now() < end) requestAnimationFrame(frame);
  })();
}
