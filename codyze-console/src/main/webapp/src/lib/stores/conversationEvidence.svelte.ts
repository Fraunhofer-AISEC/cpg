/* eslint-disable svelte/prefer-svelte-reactivity --
   The Maps and Sets here are temporary values of derived computations or replaced as a whole, they
   are never mutated after being published, so they do not need to be reactive. */
import {
  checkDiagnostics,
  extractCitations,
  extractNodeIds,
  resolveNodes
} from '$lib/agentEvidence';
import type { ChatMessage, EvidenceDiagnosticJSON, NodeJSON } from '$lib/types';

/** One message of the conversation, placed in the timeline. */
export interface TimelineEntry {
  message: ChatMessage;
  /** The index of the message in the conversation */
  index: number;
  /** The number of the step, for tool calls */
  step?: number;
}

/** A question with everything the agent did to answer it. */
export interface TimelineBlock {
  question: ChatMessage | null;
  entries: TimelineEntry[];
}

/** The nodes that one tool call (a step) returned. */
export interface StepEvidence {
  step: number;
  ids: string[];
}

/** The evidence of a block: what each step returned and what is only cited in the answer. */
export interface BlockEvidence {
  steps: StepEvidence[];
  /** The IDs that are only cited in the answer, not returned by a step */
  claimed: string[];
}

/** A place the answer of the active block cites, to follow the answer through the code. */
export interface ThreadStep {
  /** The nodes of the place: those cited one after the other on the same line */
  nodes: NodeJSON[];
  label: string;
  title: string;
}

/** What the evidence is derived from. It is read reactively, so it can be a getter of props. */
export interface ConversationSource {
  messages: ChatMessage[];
  streamingContent: string;
  isLoading: boolean;
  /** Whether the layer of the agent is shown in the code */
  agentVisible: boolean;
}

/** The number of diagnostics shown for a block before the rest is expanded. */
export const DIAGNOSTICS_PREVIEW = 3;

/**
 * The evidence trail of a conversation with the agent: the conversation as a timeline of questions
 * and tool calls (steps), the nodes the steps returned or the answers cite, and the places where
 * the analysis is uncertain that they rely on.
 *
 * It has to be created while a component is initialized, because it keeps its resolved nodes and
 * diagnostics up to date with effects.
 */
export class ConversationEvidence {
  private readonly source: () => ConversationSource;

  /** The nodes cited in the answers, resolved to their locations to label and open them */
  citedRefs = $state.raw<Map<string, NodeJSON>>(new Map());

  /** The nodes of the evidence, resolved to their locations. IDs that are no nodes are missing */
  evidenceRefs = $state.raw<Map<string, NodeJSON>>(new Map());

  /**
   * The places where the analysis is incomplete, uncertain or ends that the evidence of each block
   * relies on. The block the agent is still working on is checked once it is done.
   */
  blockDiagnostics = $state.raw<EvidenceDiagnosticJSON[][]>([]);

  /** The block the user chose to show in the code, or null for the latest one with evidence */
  chosenBlock = $state<number | null>(null);

  /** The blocks whose diagnostics are shown in full, otherwise only the first few are shown */
  diagnosticsExpanded = $state<Record<number, boolean>>({});

  /** The conversation as a timeline: one block per question, with the tool calls as steps */
  timeline = $derived.by(() => {
    const blocks: TimelineBlock[] = [];
    this.source().messages.forEach((message, index) => {
      if (message.role === 'user') {
        blocks.push({ question: message, entries: [] });
        return;
      }
      if (blocks.length === 0) blocks.push({ question: null, entries: [] });
      const block = blocks[blocks.length - 1];
      const step =
        message.contentType === 'tool-result'
          ? block.entries.filter((e) => e.step).length + 1
          : undefined;
      block.entries.push({ message, index, step });
    });
    return blocks;
  });

  /** The evidence of each block */
  blockEvidence: BlockEvidence[] = $derived(
    this.timeline.map((block) => {
      const steps = block.entries.flatMap((e) =>
        e.step ? [{ step: e.step, ids: extractNodeIds(e.message.toolResult?.content) }] : []
      );
      const returned = new Set(steps.flatMap((s) => s.ids));
      const cited = block.entries.flatMap((e) =>
        e.message.contentType === 'text' ? extractCitations(e.message.content) : []
      );
      return { steps, claimed: [...new Set(cited)].filter((id) => !returned.has(id)) };
    })
  );

  /** The block whose evidence is shown in the code: the chosen one, else the latest with evidence */
  activeBlock = $derived(
    this.chosenBlock !== null && this.chosenBlock < this.timeline.length
      ? this.chosenBlock
      : this.blockEvidence.findLastIndex(
          (e) => e.steps.some((s) => s.ids.length) || e.claimed.length
        )
  );

  /**
   * The places the answer of the active block cites, in the order it cites them, to follow the
   * answer through the code one place after the other. Nodes only returned by the tool calls are no
   * place: they are named in the diagnostics under the answer.
   */
  threadSteps: ThreadStep[] = $derived.by(() => {
    if (!this.source().agentVisible) return [];
    return this.citedPlaces(this.activeBlock).map((nodes) => {
      // The most complete code of the place, e.g. the statement rather than a part of it
      const ref = nodes.reduce((a, b) =>
        (b.code || b.name).length > (a.code || a.name).length ? b : a
      );
      const code = ref.code || ref.name;
      return {
        nodes,
        label: code.length > 32 ? `${code.slice(0, 31)}…` : code,
        title: `${code} · ${ref.fileName ?? ''}:${ref.startLine}`
      };
    });
  });

  private readonly citedKey: string;

  private evidenceKey = $derived(
    [
      ...new Set(
        this.blockEvidence.flatMap((e) => [...e.steps.flatMap((s) => s.ids), ...e.claimed])
      )
    ]
      .sort()
      .join(',')
  );

  constructor(source: () => ConversationSource) {
    this.source = source;
    this.citedKey = $derived(
      [
        ...new Set([
          ...this.source().messages.flatMap((m) =>
            m.role === 'assistant' && m.contentType !== 'tool-result'
              ? extractCitations(m.content)
              : []
          ),
          ...extractCitations(this.source().streamingContent)
        ])
      ].join(',')
    );

    $effect(() => {
      const key = this.citedKey;
      if (!key) return;
      resolveNodes(key.split(',')).then((refs) => {
        if (key === this.citedKey) this.citedRefs = refs;
      });
    });

    $effect(() => {
      const key = this.evidenceKey;
      if (!key) return;
      resolveNodes(key.split(',')).then((refs) => {
        if (key === this.evidenceKey) this.evidenceRefs = refs;
      });
    });

    $effect(() => {
      const evidence = this.blockEvidence;
      const pending = this.source().isLoading ? evidence.length - 1 : -1;
      Promise.all(
        evidence.map((e, b) =>
          b === pending ? [] : checkDiagnostics([...e.steps.flatMap((s) => s.ids), ...e.claimed])
        )
      ).then((diagnostics) => {
        if (evidence === this.blockEvidence) this.blockDiagnostics = diagnostics;
      });
    });
  }

  /** The resolved nodes of the given IDs, without those that are no nodes. */
  stepNodes(ids: string[]): NodeJSON[] {
    return ids.flatMap((id) => this.evidenceRefs.get(id) ?? []);
  }

  /**
   * The steps the answer of a block relies on. A node the answer cites counts for the most specific
   * step that returned it, the one with the fewest nodes: an overview that lists all functions
   * returns most cited nodes too, but the answer relies on the step that looked at them.
   */
  usedSteps(block: number): Set<number> {
    const steps = this.blockEvidence[block]?.steps ?? [];
    const used = new Set<number>();
    const cited = (this.timeline[block]?.entries ?? []).flatMap((e) =>
      e.message.contentType === 'text' ? extractCitations(e.message.content) : []
    );
    for (const id of new Set(cited)) {
      const step = steps
        .filter((s) => s.ids.includes(id))
        .reduce<StepEvidence | null>((a, s) => (!a || s.ids.length < a.ids.length ? s : a), null);
      if (step) used.add(step.step);
    }
    return used;
  }

  /** The places in the code the answer of a block cites, in the order it cites them. */
  citedPlaces(block: number): NodeJSON[][] {
    const ids = (this.timeline[block]?.entries ?? []).flatMap((e) =>
      e.message.contentType === 'text' ? extractCitations(e.message.content) : []
    );
    return placesOf(
      [...new Set(ids)].flatMap((id) => {
        const ref = this.citedRefs.get(id) ?? this.evidenceRefs.get(id);
        return ref && ref.startLine >= 1 ? [ref] : [];
      })
    );
  }

  /** Who relies on a diagnostic of a block, e.g. "Step 1", "Steps 1, 3" or "The answer". */
  diagnosticSubject(
    diagnostic: EvidenceDiagnosticJSON,
    block: number
  ): { text: string; plural: boolean } {
    const steps = (this.blockEvidence[block]?.steps ?? [])
      .filter((s) => s.ids.some((id) => diagnostic.evidence.includes(id)))
      .map((s) => s.step);
    if (steps.length === 0) return { text: 'The answer', plural: false };
    return {
      text: `${steps.length === 1 ? 'Step' : 'Steps'} ${steps.join(', ')}`,
      plural: steps.length > 1
    };
  }
}

/**
 * The places of cited nodes, in the order of their citations: nodes cited one after the other on
 * the same line, e.g. a call and the variable it is assigned to, are one place.
 */
function placesOf(refs: NodeJSON[]): NodeJSON[][] {
  const places: NodeJSON[][] = [];
  for (const ref of refs) {
    const last = places.at(-1)?.[0];
    if (
      last &&
      last.translationUnitId === ref.translationUnitId &&
      last.startLine === ref.startLine
    ) {
      places.at(-1)!.push(ref);
    } else {
      places.push([ref]);
    }
  }
  return places;
}
