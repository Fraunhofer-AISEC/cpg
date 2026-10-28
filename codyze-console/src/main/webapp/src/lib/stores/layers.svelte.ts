/** A layer of information that is drawn on top of the code and can be toggled. */
export type Layer = 'concepts' | 'external' | 'findings' | 'dataflow' | 'agent';

export interface LayerInfo {
  id: Layer;
  icon: string;
  label: string;
  /** What the layer shows, e.g. as a tooltip */
  description: string;
  /** The class of the icon, in the color of the layer's marks in the code */
  iconClass: string;
  /**
   * What the analysis found in a file, counted in the summary above the code. The other layers
   * show what was asked for, e.g. the dataflows of the selection
   */
  category: boolean;
}

/** The layers in the order of their toggles: first the categories, then the others. */
export const layerInfos: LayerInfo[] = [
  {
    id: 'concepts',
    icon: '◆',
    label: 'Concepts',
    description: 'Concepts and operations, as icons in the gutter',
    iconClass: 'text-purple-600',
    category: true
  },
  {
    id: 'external',
    icon: '↗',
    label: 'External',
    description: 'Calls to code that is not part of the analysis, underlined in orange',
    iconClass: 'text-orange-600',
    category: true
  },
  {
    id: 'findings',
    icon: '▲',
    label: 'Findings',
    description: 'Places that violate a requirement of the project, as yellow lines',
    iconClass: 'text-amber-600',
    category: true
  },
  {
    id: 'dataflow',
    icon: '→',
    label: 'Dataflow',
    description:
      'The direct dataflows of the selection, as arcs next to the line numbers: arrows point to where the value goes',
    iconClass: 'text-slate-500',
    category: false
  },
  {
    id: 'agent',
    icon: '●',
    label: 'Agent',
    description:
      'The steps of the active question to the agent in the bar above the code; its evidence is shown from its answer',
    iconClass: 'text-slate-800',
    category: false
  }
];

const storageKey = 'codyze-code-layers';

function load(): Record<Layer, boolean> {
  const visible: Record<Layer, boolean> = {
    concepts: true,
    external: true,
    findings: true,
    dataflow: true,
    agent: true
  };
  try {
    const stored = JSON.parse(localStorage.getItem(storageKey) ?? '{}');
    for (const info of layerInfos) {
      // The agent layer has no toggle above the code, so hiding it only lasts until the page is loaded again
      if (info.id === 'agent') continue;
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
