/** A layer of information that is drawn on top of the code and can be toggled. */
export type Layer = 'concepts' | 'external' | 'uncertain';

export interface LayerInfo {
  id: Layer;
  icon: string;
  label: string;
  /** What the layer shows, e.g. as a tooltip */
  description: string;
  /** The color of the layer's marks, as a CSS color */
  color: string;
  /** The classes of the layer's toggle while it is shown */
  activeClass: string;
}

/** The layers in the order of their toggles. Each one has its own semantic color. */
export const layerInfos: LayerInfo[] = [
  {
    id: 'concepts',
    icon: '◆',
    label: 'Concepts',
    description: 'Concepts and operations, as icons in the gutter',
    color: 'rgb(147, 51, 234)',
    activeClass: 'border-purple-300 bg-purple-50 text-purple-700'
  },
  {
    id: 'external',
    icon: '↗',
    label: 'External',
    description: 'Calls to code that is not part of the analysis, underlined in orange',
    color: 'rgb(234, 88, 12)',
    activeClass: 'border-orange-300 bg-orange-50 text-orange-700'
  },
  {
    id: 'uncertain',
    icon: '⚠',
    label: 'Uncertain',
    description: 'Calls whose target the analysis could not determine, underlined in red',
    color: 'rgb(220, 38, 38)',
    activeClass: 'border-red-300 bg-red-50 text-red-700'
  }
];

const storageKey = 'codyze-code-layers';

function load(): Record<Layer, boolean> {
  const visible: Record<Layer, boolean> = { concepts: true, external: true, uncertain: true };
  try {
    const stored = JSON.parse(localStorage.getItem(storageKey) ?? '{}');
    for (const info of layerInfos) {
      if (typeof stored[info.id] === 'boolean') visible[info.id] = stored[info.id];
    }
  } catch {
    // Storage is not available, e.g. during SSR or in a private window
  }
  return visible;
}

/** Which layers are shown in the code viewers, remembered across sessions. */
class Layers {
  visible = $state(load());

  toggle(layer: Layer) {
    this.visible[layer] = !this.visible[layer];
    try {
      localStorage.setItem(storageKey, JSON.stringify(this.visible));
    } catch {
      // Not remembering the layers is fine
    }
  }
}

export const layers = new Layers();
