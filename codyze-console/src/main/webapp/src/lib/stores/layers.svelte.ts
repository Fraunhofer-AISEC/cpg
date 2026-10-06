/** A layer of information that is drawn on top of the code and can be toggled. */
export type Layer = 'concepts' | 'external' | 'uncertain' | 'dataflow' | 'agent';

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
  },
  {
    id: 'dataflow',
    icon: '→',
    label: 'Dataflow',
    description:
      'The direct dataflows of the selection, as arcs next to the line numbers: arrows point to where the value goes',
    color: 'rgb(71, 85, 105)',
    activeClass: 'border-slate-300 bg-slate-100 text-slate-700'
  },
  {
    id: 'agent',
    icon: '●',
    label: 'Agent',
    description:
      'The evidence of the active question to the agent, as numbered markers of its steps in the gutter: solid if a tool returned the node, dashed if the answer only cites it',
    color: 'rgb(15, 23, 42)',
    activeClass: 'border-slate-400 bg-slate-100 text-slate-800'
  }
];

const storageKey = 'codyze-code-layers';

function load(): Record<Layer, boolean> {
  const visible: Record<Layer, boolean> = {
    concepts: true,
    external: true,
    uncertain: true,
    dataflow: true,
    agent: true
  };
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
