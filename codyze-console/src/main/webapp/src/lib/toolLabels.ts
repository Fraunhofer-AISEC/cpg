/** A tool call of the agent as it is shown: the tool, what it was called on, and how. */
export interface ToolCallLabel {
  tool: string;
  /** The main argument, e.g. the name of the node or function the tool was called on */
  argument?: string;
  /** Further arguments that change what the tool does, e.g. `CDG backward` for a slice */
  detail?: string;
}

/**
 * The label of a tool call: its tool and its main argument. Node IDs are named by [nodeName],
 * which may not know them yet; until then, they are shortened.
 */
export function toolCallLabel(
  toolName: string,
  args: Record<string, unknown> | undefined,
  nodeName: (id: string) => string | undefined
): ToolCallLabel {
  const text = (key: string) => {
    const value = args?.[key];
    return typeof value === 'string' || typeof value === 'number' ? String(value) : undefined;
  };
  const node = (key: string) => {
    const id = text(key)?.toLowerCase();
    return id ? (nodeName(id) ?? `…${id.slice(-8)}`) : undefined;
  };

  switch (toolName) {
    case 'cpg_slice':
      return {
        tool: toolName,
        argument: node('id'),
        detail: [text('graph')?.toUpperCase() ?? 'PDG', text('direction')?.toLowerCase()]
          .filter(Boolean)
          .join(' ')
      };
    case 'cpg_list_call_arg_by_name_or_index': {
      const which = text('argumentName') ?? (text('index') ? `#${text('index')}` : undefined);
      return { tool: toolName, argument: node('nodeId'), detail: which };
    }
    case 'cpg_dataflow':
      return {
        tool: toolName,
        argument: text('from') && text('to') ? `${text('from')} → ${text('to')}` : undefined
      };
    case 'cpg_run_pass':
      return { tool: toolName, argument: text('passName')?.split('.').pop() };
    default:
      return { tool: toolName, argument: node('id') ?? node('nodeId') ?? text('name') };
  }
}

/** The node IDs among the arguments of a tool call, to name them in its label. */
export function toolNodeIds(args: Record<string, unknown> | undefined): string[] {
  return ['id', 'nodeId'].flatMap((key) => {
    const value = args?.[key];
    return typeof value === 'string' ? [value.toLowerCase()] : [];
  });
}
